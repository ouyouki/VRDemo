package com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.baidu.cloud.gpuimage.ColorAdjustFilter;
import com.baidu.cloud.gpuimage.GPUImageSoftenBeautyFilter;
import com.baidu.cloud.gpuimage.basefilters.GPUImageFilter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.util.FileUtils;
import com.baidu.cloud.mediaproc.sample.util.ResourceUtil;
import com.baidu.cloud.mediaproc.sample.util.model.ProcessParam;
import com.baidu.cloud.mediaproc.sample.widget.CaptureProgressView;
import com.baidu.cloud.mediastream.config.ProcessConfig;
import com.baidu.cloud.mediastream.listener.ProcessStateListener;
import com.baidu.cloud.mediastream.session.MediaProcessSession;
import com.baidubce.services.vod.model.ProcessMediaResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;


public class ProcessViewModel extends BaseModel {
    private static final String TAG = "ProcessViewModel";
    private MediaProcessSession mMediaProcessSession;
    private SharedPreferences sharedPreferences;

    private String mp4SavedPath;

    public ObservableBoolean isProcessing = new ObservableBoolean(false);
    public ObservableBoolean isMediaProcessSuccess = new ObservableBoolean(false);
    public ObservableField<String> processTint = new ObservableField<>("合成中，请稍候…");
    public ObservableField<String> processProgressTint = new ObservableField<>("0%");
    public ObservableInt processProgress = new ObservableInt(0);

    private boolean saveToDirectory = false;

    private CaptureProgressView progressView;

    public ProcessViewModel(final Context context, final ProcessParam param, CaptureProgressView btnCapture) {
        Log.d(TAG, "ProcessViewModel: " + param);
        progressView = btnCapture;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        ProcessConfig.Builder builder = new ProcessConfig.Builder();
        FileUtils.configProcessConfig(param.mediaFilePath, builder);
        builder.setPlaybackRate(param.playbackRate);
        mMediaProcessSession = new MediaProcessSession(context, builder.build());
//        builder.setInitVideoBitrate(2048000);
        mMediaProcessSession.setProcessStateListener(new ProcessStateListener() {
            @Override
            public void onProgress(final int progress) {
                processProgressTint.set(progress + "%");
                processProgress.set(100 - progress);
                progressView.setProgress(progress);
            }

            @Override
            public void onFinish(boolean isSuccess, int what) {
                processProgressTint.set("");
                isProcessing.set(false);
                if (isSuccess) {
                    isMediaProcessSuccess.set(true);
                    processTint.set("合成成功");
                } else {
                    processTint.set("合成失败，错误码：" + what);
                    new File(mp4SavedPath).delete();
                }
            }
        });
        mMediaProcessSession.setMediaFilePath(param.mediaFilePath);

        if (!TextUtils.isEmpty(param.bgmUri)) {
            mMediaProcessSession.configBackgroundMusic(true, param.bgmUri, true);
            mMediaProcessSession.setBGMTrackGain(param.bgmTrackGain);
            mMediaProcessSession.configBackgroundMusicClip(param.bgmStart, param.bgmInterval);
        }

        GPUImageSoftenBeautyFilter beautyFilter = new GPUImageSoftenBeautyFilter();
        beautyFilter.setBrightLevel(param.brightLevel);
        beautyFilter.setPinkLevel(param.pinkLevel);
        beautyFilter.setSmoothLevel(param.smoothLevel);
        ColorAdjustFilter colorAdjustFilter = new ColorAdjustFilter(context);
        colorAdjustFilter.setBrightness(param.brightness);
        colorAdjustFilter.setContrast(param.contrast);
        colorAdjustFilter.setHue(param.hue);
        colorAdjustFilter.setSaturation(param.saturation);
        colorAdjustFilter.setSharpness(param.sharpness);

        // 滤镜的顺序不能错，否则可能达不到需要的效果
        final List<GPUImageFilter> filterList = new ArrayList<>();
        filterList.add(beautyFilter);
        filterList.add(colorAdjustFilter);

        new Thread(new Runnable() {
            @Override
            public void run() {
                GPUImageFilter filter = FiltersAdapter.getFilterByName(context, param.customFilter);
                filterList.add(filter);
                mMediaProcessSession.setGPUImageFilters(filterList);
                mp4SavedPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getPath()
                        + "/sdk-process-" + System.currentTimeMillis() + ".mp4";
                mMediaProcessSession.configMp4Saver(true, mp4SavedPath);
                isProcessing.set(true);
                mMediaProcessSession.start();
            }
        }).start();
    }

    public void upload(final View view) {
        if (isProcessing.get() || processTint.get().equals("上传成功")) {
            return;
        }
        isProcessing.set(true);
        processTint.set("上传中…");
        final ObjectAnimator animator = ObjectAnimator.ofInt(processProgress, "", 99, 0);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(10000);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                processProgressTint.set(100 - processProgress.get() + "%");
                progressView.setProgress(100 - processProgress.get());
            }
        });
        animator.start();
        // TODO: 25/04/2017 vod upload need progress callback
        ResourceUtil.INSTANCE
                .applyUploadAndProcess(new File(mp4SavedPath))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ProcessMediaResponse>() {
                    @Override
                    public void accept(@NonNull ProcessMediaResponse processMediaResponse) throws Exception {
                        Set<String> set = sharedPreferences.getStringSet("media", new HashSet<String>());
                        Set<String> strings = new HashSet<>();
                        strings.addAll(set);
                        strings.add(processMediaResponse.getMediaId());
                        sharedPreferences.edit().putStringSet("media", strings).commit();
                        processTint.set("上传成功");
                        if (animator.isRunning()) {
                            animator.cancel();
                            processProgress.set(0);
                        }
                        isProcessing.set(false);
                        processProgressTint.set("100%");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        processTint.set(throwable.getMessage());
                    }
                });
    }

    @Override
    public void onResume() {
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onDestroy() {
        if (isProcessing.get() && !isMediaProcessSuccess.get()) {
            mMediaProcessSession.stop();
            new File(mp4SavedPath).delete();
        }
        if (!saveToDirectory) {
            new File(mp4SavedPath).delete();
        }
    }

    public void cancel(View view) {
        if (isProcessing.get() && !isMediaProcessSuccess.get()) {
            isProcessing.set(false);
            processTint.set("合成被取消");
            mMediaProcessSession.stop();
            new File(mp4SavedPath).delete();
        } else {
            Toast.makeText(view.getContext(), "上传不能取消", Toast.LENGTH_SHORT).show();
        }
    }

    public void saveToDirectory(View view) {
        saveToDirectory = true;
        processTint.set("视频文件保存在" + mp4SavedPath);
    }

}
