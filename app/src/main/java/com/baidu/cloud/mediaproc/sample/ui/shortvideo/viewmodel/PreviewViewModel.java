package com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel;

import android.content.Context;
import android.content.Intent;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.databinding.ActivityPreviewBinding;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.ConfigProcessActivity;
import com.baidu.cloud.mediaproc.sample.util.FileUtils;
import com.baidu.cloud.mediastream.config.ProcessConfig;
import com.baidu.cloud.mediastream.listener.PreviewStateListener;
import com.baidu.cloud.mediastream.listener.ProcessStateListener;
import com.baidu.cloud.mediastream.session.MediaPreviewSession;
import com.baidu.cloud.mediastream.session.MediaProcessSession;

import java.io.File;

import static com.baidu.cloud.mediaproc.sample.util.MusicTool.stringForTime;

/**
 * Created by wenyiming on 17/04/2017.
 */

public class PreviewViewModel extends BaseModel {
    private static final String TAG = "PreviewViewModel";
    private MediaProcessSession mMediaProcessSession;
    private MediaPreviewSession mMediaPreviewSession;

    private String mp4SavedPath;

    public ObservableBoolean isPreviewStarted = new ObservableBoolean(false);
    private boolean isProcessStarted = false;
    public ObservableInt processProgress = new ObservableInt(0);
    public ObservableInt previewProgress = new ObservableInt(0);
    public ObservableInt previewProgressMax = new ObservableInt(0);
    private int startTime = 0;
    public ObservableInt intervalTime = new ObservableInt(15);
    public ObservableField<String> startTimeString = new ObservableField<>("00:00");

    public PreviewViewModel(final Context context, final ActivityPreviewBinding binding, String videoPath) {
        final int time = (int) (FileUtils.getDurationOfVideoInUs(videoPath) / 1000);
        final LinearLayoutManager layoutManager = (LinearLayoutManager) binding.recyclerView
                .getLayoutManager();
        previewProgressMax.set((int) (FileUtils.getDurationOfVideoInUs(videoPath) / 1000));
        binding.recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_SETTLING) {
                    tryStopPreview();
                } else {
                    tryStartPreview();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                startTime = Math.min(layoutManager.findFirstVisibleItemPosition() * 2500, time - intervalTime.get() * 1000);
                startTimeString.set(stringForTime(startTime));
                if (mMediaPreviewSession != null) {
                    mMediaPreviewSession.configMediaFileClip(startTime * 1000, intervalTime.get() * 1000000);
                }
                if (mMediaProcessSession != null) {
                    mMediaProcessSession.configMediaFileClip(startTime * 1000, intervalTime.get() * 1000000);
                }
            }
        });
        mp4SavedPath = videoPath;

        ProcessConfig.Builder builder = new ProcessConfig.Builder();
        FileUtils.configProcessConfig(videoPath, builder);
        mMediaProcessSession = new MediaProcessSession(context, builder.build());
        mMediaProcessSession.setProcessStateListener(new ProcessStateListener() {
            @Override
            public void onProgress(int progress) {
                isProcessStarted = true;
                processProgress.set(100 - progress);
            }

            @Override
            public void onFinish(boolean isSuccess, int what) {
                isProcessStarted = false;
                if (isSuccess) {
                    Toast.makeText(context, "Video has been transfer and save to: "
                            + mp4SavedPath, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(context, ConfigProcessActivity.class);
                    intent.putExtra(ConfigProcessActivity.EXTRA_VIDEO_PATH, mp4SavedPath);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "转码失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mMediaPreviewSession = new MediaPreviewSession(context);
        mMediaPreviewSession.setVideoAudioEnabled(true, true);
        mMediaPreviewSession.setLooping(false);
        mMediaPreviewSession.setSurfaceHolder(binding.surfaceView.getHolder());
        mMediaPreviewSession.setPreviewStateListener(new PreviewStateListener() {
            @Override
            public void onProgress(int progress, long currentPTSInUs) {
                previewProgress.set((int) (currentPTSInUs / 1000));
            }

            @Override
            public void onSizeChanged(int videoWidth, int videoHeight, int videoOrientation) {
                final ConstraintLayout.LayoutParams params =
                        (ConstraintLayout.LayoutParams) binding.frameLayout.getLayoutParams();
                String ratio;
                if (videoOrientation == 90 || videoOrientation == 270) {
                    ratio = videoHeight + ":" + videoWidth;
                } else {
                    ratio = videoWidth + ":" + videoHeight;
                }
                params.dimensionRatio = ratio;
                binding.activityConfigProcess.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.frameLayout.setLayoutParams(params);
                    }
                });
            }

            @Override
            public void onFinish(boolean isSuccess, int what) {
                Log.d(TAG, "onFinish: " + isSuccess + " " + what);
            }

            @Override
            public void onDuration(int durationInMilliSec) {
                Log.d(TAG, "onDuration: " + durationInMilliSec);
                binding.recyclerView.setEnabled(true);
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

        mMediaPreviewSession.setMediaFilePath(videoPath);
        mMediaProcessSession.setMediaFilePath(videoPath);
//        viewModel.mMediaPreviewSession.start();
    }

    public void onClickTopRight(final View view) {
        if (isProcessStarted) {
            return;
        }
        if (isPreviewStarted.get()) {
            // 如果已经开始预览说明选择了区间，需要裁剪视频
            mp4SavedPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    .getPath() + "/sdk-cut-" + System.currentTimeMillis() + ".mp4";
            mMediaProcessSession.configMp4Saver(true, mp4SavedPath);
            mMediaProcessSession.start();
        } else {
            Intent intent = new Intent(view.getContext(), ConfigProcessActivity.class);
            intent.putExtra(ConfigProcessActivity.EXTRA_VIDEO_PATH, mp4SavedPath);
            view.getContext().startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        mMediaPreviewSession.resume();
    }

    @Override
    public void onPause() {
        mMediaPreviewSession.pause();
    }

    @Override
    public void onDestroy() {
        tryStopPreview();
        if (isProcessStarted) {
            mMediaProcessSession.stop();
            new File(mp4SavedPath).delete();
        }
        mMediaPreviewSession.release();
    }

    public void onIntervalChose(View view) {
        tryStopPreview();
        if (view instanceof RadioButton) {
            int time = Integer.parseInt(((RadioButton) view).getText().toString().split("s")[0]);
            intervalTime.set(time);
            mMediaPreviewSession.configMediaFileClip(startTime * 1000, time * 1000000);
            mMediaProcessSession.configMediaFileClip(startTime * 1000, time * 1000000);
        }
        tryStartPreview();
    }

    private void tryStartPreview() {
        if (!isPreviewStarted.get()) {
            mMediaPreviewSession.start();
            isPreviewStarted.set(true);
        }
    }

    private void tryStopPreview() {
        if (isPreviewStarted.get()) {
            isPreviewStarted.set(false);
            mMediaPreviewSession.stop();
        }
    }

}
