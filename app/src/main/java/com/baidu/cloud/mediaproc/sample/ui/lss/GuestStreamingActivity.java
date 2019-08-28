package com.baidu.cloud.mediaproc.sample.ui.lss;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.cloud.bdrtmpsession.BDRtmpSessionBasic;
import com.baidu.cloud.bdrtmpsession.OnSessionEventListener;
import com.baidu.cloud.media.player.IMediaPlayer;
import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityGuestStreamingBinding;
import com.baidu.cloud.mediaproc.sample.ui.lss.viewmodel.GuestStreamViewModel;
import com.baidu.cloud.mediaproc.sample.util.PixelUtil;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;
import com.baidu.cloud.mediastream.config.LiveConfig;
import com.baidu.cloud.mediastream.session.LiveStreamSession;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 双向 RTMP 连麦直播的用户（小主播）端，可以观看大主播直播并且发起和大主播的连麦
 */
public class GuestStreamingActivity extends AppCompatActivity implements OnSessionEventListener {
    private static final String TAG = "GuestStreamingActivity";

    private LiveStreamSession mSession;
    private String pushUrlBase = "";
    private String pullUrlBase = "";
    private String pullUrlMergeBase = "";

    private BDRtmpSessionBasic.UserRole mRole = BDRtmpSessionBasic.UserRole.Host;

    private ActivityGuestStreamingBinding binding;
    private GuestStreamViewModel viewModel;
    private String uuid = UUID.randomUUID().toString();
    private boolean sessionInited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.requestFeature(Window.FEATURE_NO_TITLE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_guest_streaming);
        binding.remotePreview.setVideoScalingMode(BDCloudVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        binding.remotePreview1.setVideoScalingMode(BDCloudVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        binding.remotePreview.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Toast.makeText(GuestStreamingActivity.this, "发生错误，请检查网络和主播推流是否成功", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        binding.remotePreview1.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Toast.makeText(GuestStreamingActivity.this, "发生错误，请检查网络和主播推流是否成功", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        binding.remotePreview.setMaxProbeTime(50);
        binding.remotePreview.setMaxCacheSizeInBytes(60 * 1024);
        binding.remotePreview.setBufferTimeInMs(200);
        binding.remotePreview.setMaxProbeSize(16 * 2048);
        binding.remotePreview.toggleFrameChasing(true);
        binding.remotePreview1.setMaxProbeTime(50);
        binding.remotePreview1.setMaxCacheSizeInBytes(60 * 1024);
        binding.remotePreview1.setBufferTimeInMs(200);
        binding.remotePreview1.setMaxProbeSize(16 * 2048);
        binding.remotePreview1.toggleFrameChasing(true);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        am.setMode(AudioManager.MODE_IN_CALL);

        Intent i = getIntent();
        viewModel = new GuestStreamViewModel(i);
        binding.setModel(viewModel);
        int role = i.getIntExtra("role", 0);
        // 1-->主播  2-->高级观众 3-->普通观众
        if (role == 0 || role == 1) {
            throw new RuntimeException("Should not reach here");
        }
        mRole = getUserRoleByType(role);
        pullUrlBase = i.getStringExtra("url_play");
        pullUrlMergeBase = i.getStringExtra("url_play_merge");
        pushUrlBase = i.getStringExtra("url_push");

        LiveConfig.Builder builder = new LiveConfig.Builder();
        builder.setVideoWidth(360)
                .setVideoHeight(640)
                .setCameraOrientation(90)
                .setVideoFPS(20)
                .setInitVideoBitrate(400000)
                .setMinVideoBitrate(100000)
                .setMaxVideoBitrate(800000)
                .setVideoEnabled(true)
                .setAudioSampleRate(44100)
                .setAudioBitrate(64000)
                .setAudioEnabled(true)
                .setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT);
        mSession = new LiveStreamSession(this, builder.build());
        mSession.setRtmpEventListener(this);

        mSession.setupDevice();
        binding.remotePreview.setVideoPath(pullUrlMergeBase + viewModel.mRoomName.get() + "Host");
        binding.remotePreview.start();
        mSession.setSurfaceHolder(binding.localPreview.getHolder());
        mSession.configRtmpSession(pushUrlBase + viewModel.mRoomName.get() + mRole.toString() + uuid, mRole);
    }

    private BDRtmpSessionBasic.UserRole getUserRoleByType(int type) {
        switch (type) {
            case 1:
                return BDRtmpSessionBasic.UserRole.Host;
            case 2:
                return BDRtmpSessionBasic.UserRole.Guest;
            default:
                return BDRtmpSessionBasic.UserRole.Audience;
        }
    }

    @Override
    protected void onDestroy() {
        if (binding.remotePreview.isPlaying()) {
            binding.remotePreview.stopPlayback();
        }
        if (binding.remotePreview1.isPlaying()) {
            binding.remotePreview1.stopPlayback();
        }
        mSession.destroyRtmpSession();
        mSession.releaseDevice();
        super.onDestroy();
    }

    @Override
    public void onSessionConnected() {
        Log.d(TAG, "onSessionConnected: ");
        sessionInited = true;
    }

    @Override
    public void onError(int errorCode) {
        Log.d(TAG, "onError: " + errorCode);
    }

    /**
     * 因为小主播不会在demo场景中被呼叫所以直接返回 false
     *
     * @param callerUrl 小主播的推流地址
     * @param userId 小主播的UserId
     * @return
     */
    @Override
    public void onConversationRequest(final String callerUrl, final String userId) {
    }

    private Disposable timeTask;

    @Override
    public void onConversationStarted(String userId) {
        Log.d(TAG, "onConversationStarted: ");
        timeTask = Flowable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(@NonNull Long aLong) throws Exception {
                        viewModel.mConversationTime.set(valueToString(aLong));
                    }
                });
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewModel.timeShowing.set(true);
                viewModel.waiting.set(false);
                switchHostPreview();
            }
        });
    }

    @Override
    public void onConversationFailed(String userId, final FailureReason failReasonCode) {
        mSession.stopStreaming();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GuestStreamingActivity.this, "Start conversation failed with reason " + failReasonCode
                        .toString(), Toast.LENGTH_SHORT).show();
                viewModel.waiting.set(false);
                viewModel.calling.set(false);
                // 如果是呼叫主播时主播拒绝也会调用这块代码，而此时依旧是全屏播放，因此不需要切换播放组件
                if (binding.remotePreview1.isPlaying()) {
                    switchHostPreview();
                }
            }
        });
    }

    @Override
    public void onConversationEnded(final String userId) {
        if (timeTask != null && !timeTask.isDisposed()) {
            timeTask.dispose();
        }
        mSession.stopStreaming();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GuestStreamingActivity.this, "Conversation with user " + userId + " was ended.", Toast
                        .LENGTH_SHORT).show();
                viewModel.calling.set(false);
                viewModel.waiting.set(false);
                viewModel.timeShowing.set(false);
                // 如果是呼叫主播时小主播主动取消呼叫会调用这块代码，而此时依旧是全屏播放，因此不需要切换播放组件
                if (binding.remotePreview1.isPlaying()) {
                    switchHostPreview();
                }
            }
        });
    }

    public void onClickClose(View view) {
        finish();
    }

    public void onClickCall(View view) {
        if (!sessionInited) {
            return;
        }
        if (!viewModel.calling.get()) {
            viewModel.calling.set(true);
            viewModel.waiting.set(true);
            mSession.startStreaming();
            mSession.startCallWith(pushUrlBase + viewModel.mRoomName.get() + "Host",
                    viewModel.mRoomName.get() + "Host");
            Toast.makeText(this, "Ringing!!", Toast.LENGTH_LONG).show();
        } else {
            mSession.stopCallWith(pushUrlBase + viewModel.mRoomName.get() + "Host",
                    viewModel.mRoomName.get() + "Host");
        }
    }

    private String valueToString(long second) {
        long hh = second / 3600;
        long mm = second % 3600 / 60;
        long ss = second % 60;
        String strTemp = null;
        if (0 != hh) {
            strTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            strTemp = String.format("%02d:%02d", mm, ss);
        }
        return strTemp;
    }

    private void switchHostPreview() {
        if (binding.remotePreview.isPlaying() && !binding.remotePreview1.isPlaying()) {
            binding.remotePreview.stopPlayback();
            binding.remotePreview1.setVideoPath(pullUrlBase + viewModel.mRoomName.get() + "Host");
            binding.remotePreview1.start();
        } else if (!binding.remotePreview.isPlaying() && binding.remotePreview1.isPlaying()) {
            binding.remotePreview1.stopPlayback();
            binding.remotePreview.setVideoPath(pullUrlMergeBase + viewModel.mRoomName.get() + "Host");
            binding.remotePreview.start();
        }
    }
}
