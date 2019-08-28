package com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.baidu.cloud.gpuimage.ColorAdjustFilter;
import com.baidu.cloud.gpuimage.GPUImageSoftenBeautyFilter;
import com.baidu.cloud.gpuimage.basefilters.GPUImageFilter;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.ProcessActivity;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnProcessTuneListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnSkinBeautyListener;
import com.baidu.cloud.mediaproc.sample.util.FileUtils;
import com.baidu.cloud.mediaproc.sample.util.MusicTool;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.util.model.ProcessParam;
import com.baidu.cloud.mediastream.listener.PreviewStateListener;
import com.baidu.cloud.mediastream.session.MediaPreviewSession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by wenyiming on 17/04/2017.
 */

public class ConfigProcessViewModel extends BaseModel implements OnProcessTuneListener,
        OnFilterChoseListener, OnSkinBeautyListener, OnMusicChoseListener {
    private static final String TAG = "ConfigProcessViewModel";
    private MediaPreviewSession mMediaPreviewSession;

    public ProcessParam param = new ProcessParam();

    public ObservableBoolean isFragmentAdd = new ObservableBoolean(false);

    public ObservableField<String> endTime = new ObservableField<>("00:00");
    public ObservableField<String> currentTime = new ObservableField<>("00:00");
    public ObservableInt previewProgress = new ObservableInt(0);
    public ObservableInt previewProgressMax = new ObservableInt(0);
    public int durationInMilliSec;

    // 是否处于预览的步骤中，包括播放状态和预览暂停状态
    public ObservableBoolean isInPreviewStep = new ObservableBoolean(false);
    public ObservableBoolean isPreviewing = new ObservableBoolean(false);

    private boolean isPausedManually = false;

    private Context context;

    private GPUImageSoftenBeautyFilter beautyFilter;
    private ColorAdjustFilter colorAdjustFilter;
    private GPUImageFilter customFilter;
    // 滤镜的顺序不能错，否则可能达不到需要的效果
    private List<GPUImageFilter> filterList = new ArrayList<>();
    private long previewClipStartTimeUs = 0;

    public ConfigProcessViewModel(final Context context, SurfaceView surfaceView,
                                  final FrameLayout surfaceFrame, String videoPath) {
        this.context = context;
        param.mediaFilePath = videoPath;

        mMediaPreviewSession = new MediaPreviewSession(context);
        mMediaPreviewSession.setVideoAudioEnabled(true, true);
        mMediaPreviewSession.setSurfaceHolder(surfaceView.getHolder());
        long videoDurationInUs = FileUtils.getDurationOfVideoInUs(videoPath);
        durationInMilliSec = (int) (videoDurationInUs / 1000);
        previewProgressMax.set(durationInMilliSec);
        endTime.set(MusicTool.stringForTime(durationInMilliSec));
        mMediaPreviewSession.setMediaFilePath(videoPath);

        mMediaPreviewSession.setPreviewStateListener(new PreviewStateListener() {
            @Override
            public void onProgress(int progress, long currentPTSInUs) {
                previewProgress.set((int) (currentPTSInUs / 1000));
                currentTime.set(MusicTool.stringForTime((int) (currentPTSInUs / 1000)));
            }

            @Override
            public void onSizeChanged(int videoWidth, int videoHeight, int videoOrientation) {
                final ConstraintLayout.LayoutParams params =
                        (ConstraintLayout.LayoutParams) surfaceFrame.getLayoutParams();
                String ratio;
                if (videoOrientation == 90 || videoOrientation == 270) {
                    ratio = videoHeight + ":" + videoWidth;
                } else {
                    ratio = videoWidth + ":" + videoHeight;
                }
                params.dimensionRatio = ratio;
                surfaceFrame.post(new Runnable() {
                    @Override
                    public void run() {
                        surfaceFrame.setLayoutParams(params);
                    }
                });
            }

            @Override
            public void onFinish(boolean isSuccess, int what) {
                tryStopPreview();
            }

            @Override
            public void onDuration(int durationInMilliSec) {

            }

            @Override
            public void onStarted() {

            }

            @Override
            public void onStopped() {

            }

            @Override
            public void onPaused() {

            }

            @Override
            public void onResumed() {

            }

            @Override
            public void onReleased() {

            }

        });

        beautyFilter = new GPUImageSoftenBeautyFilter();
        colorAdjustFilter = new ColorAdjustFilter(context);
        customFilter = new GPUImageFilter();
        filterList.add(beautyFilter);
        filterList.add(colorAdjustFilter);
        filterList.add(customFilter);
        mMediaPreviewSession.setGPUImageFilters(filterList);
    }

    public void onClickTopRight(final View view) {
        Intent intent = new Intent(context, ProcessActivity.class);
        intent.putExtra("param", param);
        context.startActivity(intent);
    }

    public void onClickPreview(View view) {
        if (isPreviewing.get()) {
            mMediaPreviewSession.pause();
            isPausedManually = true;
            isPreviewing.set(false);
        } else {
            isPausedManually = false;
            if (isInPreviewStep.get()) {
                mMediaPreviewSession.resume();
            } else {
                mMediaPreviewSession.start();
                isInPreviewStep.set(true);
            }
            isPreviewing.set(true);
        }
    }

    @Override
    public void onBrightnessChange(float brightness) {
        colorAdjustFilter.setBrightness(brightness);
        param.brightness = brightness;
    }

    @Override
    public void onContrastChange(float contrast) {
        colorAdjustFilter.setContrast(contrast);
        param.contrast = contrast;
    }

    @Override
    public void onSaturationChange(float saturation) {
        colorAdjustFilter.setSaturation(saturation);
        param.saturation = saturation;
    }

    @Override
    public void onHueChange(float hue) {
        colorAdjustFilter.setHue(hue);
        param.hue = hue;
    }

    @Override
    public void onSharpnessChange(float sharpness) {
        colorAdjustFilter.setSharpness(sharpness);
        param.sharpness = sharpness;
    }

    @Override
    public void onFilterChose(final String name) {
        tryStopPreview();
        Flowable.fromCallable(new Callable<GPUImageFilter>() {
            @Override
            public GPUImageFilter call() throws Exception {
                return FiltersAdapter.getFilterByName(context, name);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<GPUImageFilter>() {
                    @Override
                    public void accept(@NonNull GPUImageFilter gpuImageFilter) throws Exception {
                        param.customFilter = name;
                        filterList.remove(customFilter);
                        customFilter = gpuImageFilter;
                        filterList.add(customFilter);
                        mMediaPreviewSession.setGPUImageFilters(filterList);
                        mMediaPreviewSession.start();
                        isInPreviewStep.set(true);
                        isPreviewing.set(true);
                        isPausedManually = false;
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(@NonNull Throwable throwable) throws Exception {
                        Toast.makeText(context, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        if (isInPreviewStep.get() && !isPausedManually) {
            isPreviewing.set(true);
            mMediaPreviewSession.resume();
        }
    }

    @Override
    public void onPause() {
        if (isInPreviewStep.get()) {
            isPreviewing.set(false);
            mMediaPreviewSession.pause();
        }
    }

    @Override
    public void onDestroy() {
        tryStopPreview();
        mMediaPreviewSession.release();
    }

    @Override
    public void onSmoothChange(float smooth) {
        beautyFilter.setSmoothLevel(smooth);
        param.smoothLevel = smooth;
    }

    @Override
    public void onBrightChange(float bright) {
        beautyFilter.setBrightLevel(bright);
        param.brightLevel = bright;
    }

    @Override
    public void onPinkChange(float pink) {
        beautyFilter.setPinkLevel(pink);
        param.pinkLevel = pink;
    }

    @Override
    public void onMusicChose(Music music) {
        tryStopPreview();
        if (music.id == -1) {
            mMediaPreviewSession.configBackgroundMusic(false, null, false);
            param.bgmUri = null;
        } else {
            mMediaPreviewSession.configBackgroundMusic(true, music.uri, true);
            param.bgmUri = music.uri;
        }
    }

    @Override
    public void onMusicVolumeChange(float volume) {
        mMediaPreviewSession.setBGMTrackGain(volume);
        param.bgmTrackGain = volume;
    }

    @Override
    public void onIntervalChose(int start) {
        tryStopPreview();
        mMediaPreviewSession.configBackgroundMusicClip(start * 1000, durationInMilliSec * 1000);
        param.bgmStart = start * 1000;
        param.bgmInterval = durationInMilliSec * 1000;
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        tryStopPreview();
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        previewClipStartTimeUs = 1000 * seekBar.getProgress();
        mMediaPreviewSession.configMediaFileClip(previewClipStartTimeUs,
                1000L * durationInMilliSec - previewClipStartTimeUs);
        onClickPreview(seekBar);
    }

    @Override
    public void onMusicSetDone() {
        // select over, start now

    }

    private void tryStopPreview() {
        if (isInPreviewStep.get()) {
            Log.d(TAG, "tryStopPreview: ");
            isInPreviewStep.set(false);
            isPreviewing.set(false);
            isPausedManually = true;
            mMediaPreviewSession.stop();
        }
    }
}
