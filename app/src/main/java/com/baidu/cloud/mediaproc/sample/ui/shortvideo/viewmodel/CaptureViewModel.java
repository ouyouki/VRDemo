package com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableFloat;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.baidu.cloud.gpuimage.ColorAdjustFilter;
import com.baidu.cloud.gpuimage.GPUImageNaturalBeautyFilter;
import com.baidu.cloud.gpuimage.basefilters.GPUImageFilter;
import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityCaptureBinding;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.ConfigProcessActivity;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.FiltersAdapter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnFilterChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnMusicChoseListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnSkinBeautyListener;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener.OnTuneListener;
import com.baidu.cloud.mediaproc.sample.util.model.Music;
import com.baidu.cloud.mediaproc.sample.widget.CaptureProgressView;
import com.baidu.cloud.mediaprocess.AuthManager;
import com.baidu.cloud.mediastream.config.LiveConfig;
import com.baidu.cloud.mediastream.session.LiveCaptureSession;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


public class CaptureViewModel extends BaseModel implements OnTuneListener, OnFilterChoseListener,
        OnSkinBeautyListener, OnMusicChoseListener {
    private static final String TAG = "CaptureViewModel";
    private final CaptureProgressView progressView;
    public LiveCaptureSession mLiveCaptureSession;

    private String mp4SavedPath;

    public ObservableFloat capturedTime = new ObservableFloat();

    // 是否处于录制步骤中，录制暂停也算处于录制步骤中
    public ObservableBoolean isCapturing = new ObservableBoolean(false);
    public ObservableBoolean isPausing = new ObservableBoolean(false);
    // 是否处于录制完等待用户决定删除还是进行合成的步骤
    public ObservableBoolean isWaitingNextStep = new ObservableBoolean(false);
    public ObservableBoolean isFlashOn = new ObservableBoolean(false);
    public ObservableBoolean isFragmentAdd = new ObservableBoolean(false);
    public ObservableField<String> filter = new ObservableField<>("None");

    public int videoHeight = 1280;
    public int videoWidth = 720;
    public int captureTimeInMs = 15 * 1000;
    public int mCurrentCamera;
    private Context context;

    // 如果录制的分辨率小于 720p 建议使用 GPUImageSoftenBeautyFilter
    // public GPUImageSoftenBeautyFilter beautyFilter;
    private GPUImageNaturalBeautyFilter beautyFilter;
    private ColorAdjustFilter colorAdjustFilter;
    private GPUImageFilter customFilter;
    // 滤镜的顺序不能错，否则可能达不到需要的效果
    private List<GPUImageFilter> filterList = new ArrayList<>();

    public CaptureViewModel(final Context context, ActivityCaptureBinding binding) {
        this.context = context;
        this.progressView = binding.btnCapture;
        progressView.setMax(captureTimeInMs / 1000);

        RxView.touches(binding.surfaceView)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<MotionEvent>() {
                    @Override
                    public void accept(@NonNull MotionEvent motionEvent) throws Exception {
                        if (motionEvent.getAction() == MotionEvent.ACTION_UP && mLiveCaptureSession != null) {
                            mLiveCaptureSession.focusToPoint((int) motionEvent.getX(), (int) motionEvent.getY());
                        }
                    }
                });

        mCurrentCamera = LiveConfig.CAMERA_FACING_BACK;
        AuthManager.setAK("your-ak-set-here!");

        LiveConfig.Builder builder = new LiveConfig.Builder();
        builder.setCameraOrientation(90)
                .setOutputOrientation(0)
                .setCameraId(LiveConfig.CAMERA_FACING_BACK)
                .setVideoWidth(videoWidth)
                .setVideoHeight(videoHeight);
        mLiveCaptureSession = new LiveCaptureSession(context, builder.build());
        mLiveCaptureSession.setSurfaceHolder(binding.surfaceView.getHolder());
        mLiveCaptureSession.setCaptureErrorListener(new LiveCaptureSession.CaptureErrorListener() {
            @Override
            public void onError(int error, String desc) {
                Log.e(TAG, "onError: id=" + error + ";info=" + desc);
            }
        });

        beautyFilter = new GPUImageNaturalBeautyFilter();
        colorAdjustFilter = new ColorAdjustFilter(context);
        customFilter = new GPUImageFilter();

        filterList.add(beautyFilter);

        // FIXME:  face sticker is not ready
        // stickerFilter = new GPUImageFaceStickerFilter(context);
        // filterList.add(stickerFilter);
        // mFaceDetector = new FaceDetector(context, stickerFilter);
        // mLiveCaptureSession.setFaceDetector(mFaceDetector);

        filterList.add(colorAdjustFilter);
        filterList.add(customFilter);
        mLiveCaptureSession.setGPUImageFilters(filterList);
        mLiveCaptureSession.setupDevice();
    }

    @Override
    public void onResume() {
        mLiveCaptureSession.resume();
    }

    @Override
    public void onPause() {
        if (tryStopCapture()) {
            File file = new File(mp4SavedPath);
            if (file.exists()) {
                file.delete();
            }
        }
        mLiveCaptureSession.pause();
    }

    @Override
    public void onDestroy() {
        mLiveCaptureSession.release();
        mLiveCaptureSession = null;
    }

    private AtomicLong tick = new AtomicLong(0);
    private Disposable captureTask;

    public void onClickCapture(final View view) {
        if (isWaitingNextStep.get()) {
            onClickFinish(view);
            return;
        }
        if (!tryPauseCapture()) {
            // 如果还没开始录制
            if (tick.get() == 0) {
                progressView.post(new Runnable() {
                    @Override
                    public void run() {
                        isCapturing.set(true);
                    }
                });
                mp4SavedPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                        .getPath() + "/sdk-capture-" + System.currentTimeMillis() + ".mp4";
                mLiveCaptureSession.configMp4Muxer(mp4SavedPath);
                progressView.setShowInnerBackground(false);
                mLiveCaptureSession.start();
            } else {
                mLiveCaptureSession.resume();
                isPausing.set(false);
            }
            progressView.setAttributeResourceId(R.drawable.ic_pause_white_24dp);
            // FIXME:  face sticker is not ready
//            final List<String> stickerNames = getFaceStickerNameList();
//            if (stickerNames != null && stickerNames.size() > 0) {
//                final int count = stickerNames.size();
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        for (int i = 0; i < count; i++) {
//                            setFaceStickerByName(stickerNames.get(i));
//                            try {
//                                Thread.sleep(5000);
//                            } catch (Exception e) {
//                                // Nothing here.
//                            }
//                        }
//                    }
//                }).start();
//            }
            captureTask = Flowable.interval(10, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map(new Function<Long, Long>() {
                        @Override
                        public Long apply(@NonNull Long aLong) throws Exception {
                            return tick.getAndIncrement();
                        }
                    })
                    .takeWhile(new Predicate<Long>() {
                        @Override
                        public boolean test(@NonNull Long aLong) throws Exception {
                            if (aLong > captureTimeInMs / 10) {
                                return false;
                            }
                            return true;
                        }
                    })
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            tryPauseCapture();
                            isWaitingNextStep.set(true);
                        }
                    })
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            progressView.setProgress(aLong / 100f);
                            capturedTime.set(aLong / 100f);
                        }
                    });
        }
    }

    public void onClickCancel(View view) {
        tryStopCapture();
        Log.d(TAG, "onClickCancel: " + new File(mp4SavedPath).delete());
    }

    public void onClickFinish(View view) {
        tryStopCapture();
        Toast.makeText(view.getContext(), "Video has saved to: " + mp4SavedPath, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, ConfigProcessActivity.class);
        intent.putExtra(ConfigProcessActivity.EXTRA_VIDEO_PATH, mp4SavedPath);
        context.startActivity(intent);
    }

    /**
     * @return 是否在调用该方法时已经开始拍摄
     */
    private synchronized boolean tryStopCapture() {
        // 如果没有开始拍摄直接返回
        if (tick.get() == 0) {
            return false;
        }
        if (captureTask != null && !captureTask.isDisposed()) {
            captureTask.dispose();
            captureTask = null;
        }
        mLiveCaptureSession.stop();
        mLiveCaptureSession.destroyMp4Muxer();
        tick.set(0);
        isCapturing.set(false);
        isPausing.set(false);
        isWaitingNextStep.set(false);
        progressView.setShowInnerBackground(true);
        progressView.setAttributeResourceId(0);
        progressView.setProgress(0);
        return true;
    }

    /**
     * @return 如果由录制状态进入暂停状态则返回 true
     */
    private synchronized boolean tryPauseCapture() {
        if (tick.get() > 0 && captureTask != null && !captureTask.isDisposed()) {
            mLiveCaptureSession.pause();
            isPausing.set(true);
            if (progressView.getProgress() == captureTimeInMs / 10) {
                progressView.setAttributeResourceId(R.mipmap.ic_continue);
            } else {
                progressView.setAttributeResourceId(R.drawable.ic_play_arrow_white_24dp);
            }
            captureTask.dispose();
            captureTask = null;
            return true;
        }
        return false;
    }

    public void onClickFlash(View view) {
        if (mCurrentCamera == LiveConfig.CAMERA_FACING_FRONT) {
            Toast.makeText(view.getContext(), "使用前置摄像头时不能开启闪光灯", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFlashOn.get()) {
            mLiveCaptureSession.toggleFlash(false);
            isFlashOn.set(false);
        } else {
            mLiveCaptureSession.toggleFlash(true);
            isFlashOn.set(true);
        }
    }

    public void onClickSwitch(View view) {
        Log.d(TAG, "onClickSwitch: ");
        if (mLiveCaptureSession.canSwitchCamera()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (mCurrentCamera == LiveConfig.CAMERA_FACING_BACK) {
                        Log.d("CaptureViewModel", "onClickSwitch: ");
                        mCurrentCamera = LiveConfig.CAMERA_FACING_FRONT;
                        mLiveCaptureSession.switchCamera(mCurrentCamera);
                        if (isFlashOn.get()) {
                            mLiveCaptureSession.toggleFlash(false);
                            isFlashOn.set(false);
                        }
                    } else {
                        mCurrentCamera = LiveConfig.CAMERA_FACING_BACK;
                        mLiveCaptureSession.switchCamera(mCurrentCamera);
                    }
                }
            }).start();
        } else {
            Toast.makeText(view.getContext(), "抱歉！该分辨率下不支持切换摄像头！", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "onClickSwitch: ");
    }

    @Override
    public void onResolutionChose(int width, int height) {

    }

    @Override
    public void onTimeChose(int seconds) {
        captureTimeInMs = seconds * 1000;
        progressView.setMax(seconds);
    }

    @Override
    public void onVolumeChange(float volume) {
        mLiveCaptureSession.setRecordTrackGain(volume);
    }

    @Override
    public void onBrightnessChange(float brightness) {
        colorAdjustFilter.setBrightness(brightness);
    }

    @Override
    public void onContrastChange(float contrast) {
        colorAdjustFilter.setContrast(contrast);
    }

    @Override
    public void onSaturationChange(float saturation) {
        colorAdjustFilter.setSaturation(saturation);
    }

    @Override
    public void onHueChange(float hue) {
        colorAdjustFilter.setHue(hue);
    }

    @Override
    public void onSharpnessChange(float sharpness) {
        colorAdjustFilter.setSharpness(sharpness);
    }

    @Override
    public void onFilterChose(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final GPUImageFilter filter = FiltersAdapter.getFilterByName(context, name);
                filterList.remove(customFilter);
                customFilter = filter;
                filterList.add(customFilter);
                mLiveCaptureSession.setGPUImageFilters(filterList);
                CaptureViewModel.this.filter.set(name);
            }
        }).start();
    }

    @Override
    public void onSmoothChange(float smooth) {
        beautyFilter.setSmoothLevel(smooth);
    }

    @Override
    public void onBrightChange(float bright) {
        beautyFilter.setBrightLevel(bright);
    }

    @Override
    public void onPinkChange(float pink) {
        beautyFilter.setPinkLevel(pink);
    }

    @Override
    public void onMusicChose(Music music) {
        if (music.id == -1) {
            mLiveCaptureSession.configBackgroundMusic(false, null, false);
        } else {
            mLiveCaptureSession.configBackgroundMusic(true, music.uri, true);
        }
    }

    @Override
    public void onMusicVolumeChange(float volume) {
        mLiveCaptureSession.setBGMTrackGain(volume);
    }

    @Override
    public void onIntervalChose(int start) {
        mLiveCaptureSession.configBackgroundMusicClip(start * 1000, captureTimeInMs * 1000);
    }

    @Override
    public void onMusicSetDone() {
        // do nothing
    }

    // FIXME:  face sticker is not ready
    // face sticker related
//    private OnFaceStickerPlayEventListener mStickerPlayEventListener = new OnFaceStickerPlayEventListener() {
//
//        @Override
//        public void onSoundPlayStarted(String filePath) { // it's the time to play sound
//            if (!TextUtils.isEmpty(filePath)) {
//                // TODO::use audio-filter to play sound
//            }
//        }
//
//        @Override
//        public void onStickerPlayCompleted() {
//            // TODO:: we may play it again from start
//        }
//
//    };
//
//    private FaceSticker mFaceSticker = null;
//    private FaceDetector mFaceDetector = null;
//    private GPUImageFaceStickerFilter stickerFilter = null;
//
//    /**
//     * 获取当前已经所支持的道具名称列表
//     *
//     * @return
//     */
//    public List<String> getFaceStickerNameList() {
//        return FaceStickerReader.getInstance(context).getDownloadedFaceStickerNameList();
//    }
//
//    /**
//     * 设置道具接口
//     *
//     * @param name 道具名称，注意该名称必须由getFaceStickerNameList接口获取，设置为空串将取消所有道具效果
//     */
//    public void setFaceStickerByName(final String name) {
//        if (TextUtils.isEmpty(name)) {
//            setupFaceSticker(null);
//        } else {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    mFaceSticker = FaceStickerReader.getInstance(context).parseFaceStickerByFileName(name);
//                    mFaceSticker.setStickerPlayEventListener(mStickerPlayEventListener);
//                    // TODO:: notify app about face-sticker-loaded event
//                    setupFaceSticker(mFaceSticker);
//                }
//            }).start();
//        }
//    }
//
//    private void setupFaceSticker(FaceSticker sticker) {
//        if (stickerFilter != null) {
//            stickerFilter.setFaceSticker(sticker);
//        }
//        // TODO:: stop sound playback of last sticker if any
//    }
}
