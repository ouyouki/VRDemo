package com.baidu.cloud.mediaproc.sample.widget.video;

import android.content.Context;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.SeekBar;

import com.baidu.cloud.media.player.IMediaPlayer;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;
import com.baidu.cloud.mediaproc.sample.util.rx.RxBusHelper;
import com.baidu.cloud.mediaproc.sample.util.rx.event.FullscreenEvent;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;

import io.reactivex.disposables.CompositeDisposable;

import static com.baidu.cloud.mediaproc.sample.util.MusicTool.stringForTime;


/**
 * CustomMediaController 中的视图模型类，负责逻辑和数据的控制
 * Created by wenyiming on 02/05/2017.
 */

public class VideoViewModel extends BaseModel implements Handler.Callback,
        RxBusHelper.OnEventListener<FullscreenEvent> {
    private static final String TAG = "VideoViewModel";

    public ObservableField<String> description = new ObservableField<>();
    public ObservableField<String> cacheInfo = new ObservableField<>("加载中");
    public ObservableField<String> currentTime = new ObservableField<>();
    public ObservableField<String> endTime = new ObservableField<>();
    public ObservableField<String> previewImage = new ObservableField<>();

    public ObservableInt previewVisibility = new ObservableInt(View.VISIBLE);
    public ObservableInt progress = new ObservableInt(0);
    public ObservableInt secondaryProgress = new ObservableInt(0);

    public ObservableBoolean hasError = new ObservableBoolean(false);
    public ObservableBoolean showPanel = new ObservableBoolean(false);
    public ObservableBoolean isBuffering = new ObservableBoolean(false);
    public ObservableBoolean isPlaying = new ObservableBoolean(false);
    public ObservableBoolean isFullscreen = new ObservableBoolean(false);

    public VideoInfo videoInfo;

    private boolean mDragging;
    private BDCloudVideoView mPlayer;
    private static final int sDefaultTimeout = 3000;

    private Handler mHandler;
    private final AccessibilityManager mAccessibilityManager;
    private final CompositeDisposable compositeDisposable;

    public VideoViewModel(Context context) {
        mHandler = new Handler(this);
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        compositeDisposable = new CompositeDisposable();
        RxBusHelper.doOnMainThread(FullscreenEvent.class, compositeDisposable, this);
    }

    public void setMediaPlayer(BDCloudVideoView videoView) {
        if (mPlayer != null) {
            mPlayer.setOnPlayerStateListener(null);
            mPlayer.setOnBufferingUpdateListener(null);
            mPlayer.setOnSeekCompleteListener(null);
            mPlayer.setOnInfoListener(null);
        }
        videoView.setOnPlayerStateListener(new BDCloudVideoView.OnPlayerStateListener() {
            @Override
            public void onPlayerStateChanged(BDCloudVideoView.PlayerState nowState) {
                switch (nowState) {
                    case STATE_PLAYING:
                        mHandler.post(mShowProgress);
                        isPlaying.set(true);
                        previewVisibility.set(View.INVISIBLE);
                        break;
                    case STATE_PREPARING:
                        isBuffering.set(true);
                        break;
                    case STATE_PREPARED:
                        isBuffering.set(false);
                        break;
                    case STATE_PAUSED:
                        isPlaying.set(false);
                        break;
                    case STATE_IDLE:
                    case STATE_PLAYBACK_COMPLETED:
                        mHandler.removeCallbacks(mShowProgress);
                        isPlaying.set(false);
                        previewVisibility.set(View.VISIBLE);
                        break;
                    case STATE_ERROR:
                        hasError.set(true);
                        isBuffering.set(false);
                        cacheInfo.set("加载失败，刷新重试");
                    default:
                        break;
                }
            }
        });
        videoView.setOnBufferingUpdateListener(new IMediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
                cacheInfo.set(String.format("加载中，%d%%", i));
            }
        });
        videoView.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(IMediaPlayer iMediaPlayer) {
                isBuffering.set(false);
            }
        });
        videoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                switch (i) {
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        isBuffering.set(true);
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        isBuffering.set(false);
                        cacheInfo.set("加载中");
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        mPlayer = videoView;
    }

    public void setVideoInfo(VideoInfo info) {
        description.set(info.description);
        currentTime.set(stringForTime(0));
        endTime.set(stringForTime(info.duration));
        previewImage.set(info.imageUrl);
        videoInfo = info;
        mPlayer.stopPlayback();
        mPlayer.reSetRender();
        mPlayer.setVideoPath(videoInfo.url);
        mPlayer.start();
    }

    @Override
    protected void onResume() {
        if (mPlayer != null && !mPlayer.isPlaying()) {
            mPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
    }

    private final Runnable mFadeOut = new Runnable() {
        @Override
        public void run() {
            showPanel.set(false);
        }
    };

    private final Runnable mShowProgress = new Runnable() {
        @Override
        public void run() {
            int pos = setProgress();
            if (!mDragging && mPlayer.isPlaying()) {
                mHandler.postDelayed(mShowProgress, 1000 - (pos % 1000));
            }
        }
    };

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (duration > 0) {
            // use long to avoid overflow
            long pos = 1000L * position / duration;
            progress.set((int) pos);
        }
        int percent = mPlayer.getBufferPercentage();
        secondaryProgress.set(percent * 10);

        endTime.set(stringForTime(duration));
        currentTime.set(stringForTime(position));

        return position;
    }

    public void show() {
        show(sDefaultTimeout);
    }

    public void show(int timeout) {
        showPanel.set(true);
        if (timeout != 0 && !mAccessibilityManager.isTouchExplorationEnabled()) {
            mHandler.removeCallbacks(mFadeOut);
            mHandler.postDelayed(mFadeOut, timeout);
        }
    }

    public final SeekBarBindingAdapter.OnStopTrackingTouch onStopTrackingTouch =
            new SeekBarBindingAdapter.OnStopTrackingTouch() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mDragging = false;
                    mHandler.post(mShowProgress);

                    long duration = mPlayer.getDuration();
                    long newPosition = (duration * seekBar.getProgress()) / 1000L;
                    mPlayer.seekTo((int) newPosition);
                    currentTime.set(stringForTime((int) newPosition));
                }
            };

    public final SeekBarBindingAdapter.OnStartTrackingTouch onStartTrackingTouch =
            new SeekBarBindingAdapter.OnStartTrackingTouch() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mDragging = true;
                    mHandler.removeCallbacks(mShowProgress);
                }
            };

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    public void onPauseResume(View view) {
        hasError.set(false);
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
    }

    public void onClickFullscreen(View v) {
        RxBusHelper.post(new FullscreenEvent(!isFullscreen.get()));

    }

    @Override
    public void onEvent(final FullscreenEvent fullscreenEvent) {
        // 防止全屏状态改变后，出现的组件间距变化导致的抖动
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                isFullscreen.set(fullscreenEvent.isFullscreen());
            }
        }, 200);
    }

    @Override
    public void onError(Throwable throwable) {

    }
}
