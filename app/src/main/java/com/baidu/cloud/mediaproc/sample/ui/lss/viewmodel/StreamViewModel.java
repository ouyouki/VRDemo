package com.baidu.cloud.mediaproc.sample.ui.lss.viewmodel;

import java.util.concurrent.TimeUnit;

import com.baidu.cloud.bdrtmpsession.BDRtmpSessionBasic;
import com.baidu.cloud.bdrtmpsession.OnSessionEventListener;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityStreamingBinding;
import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.ui.lss.adapter.CallerAdapter;
import com.baidu.cloud.mediastream.config.LiveConfig;
import com.baidu.cloud.mediastream.session.LiveStreamSession;

import android.app.Activity;
import android.content.Intent;
import android.databinding.ObservableArrayMap;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by wenyiming on 02/05/2017.
 */

public class StreamViewModel extends BaseModel implements CallerAdapter.OnCallerListener,
        OnSessionEventListener {

    private static final String TAG = "StreamViewModel";
    private final Activity activity;
    private final ActivityStreamingBinding binding;

    public ObservableField<String> currentChosenCaller = new ObservableField<>();
    public ObservableField<String> currentChosenCallerID = new ObservableField<>();
    public ObservableField<String> mConversationTime = new ObservableField<>();

    public ObservableBoolean requesting = new ObservableBoolean(false);
    public ObservableBoolean timeShowing = new ObservableBoolean(false);
    public ObservableArrayMap<String, String> requestMap = new ObservableArrayMap<>();
    public CallerAdapter callerAdapter;

    public ObservableBoolean preview1Playing = new ObservableBoolean(false);
    public ObservableBoolean preview2Playing = new ObservableBoolean(false);

    private LiveStreamSession mSession;
    private String pushUrlBase = "";
    private String pullUrlBase = "";
    private BDRtmpSessionBasic.UserRole mRole = BDRtmpSessionBasic.UserRole.Host;
    private String mRoomName = "";

    public StreamViewModel(Activity activity, Intent intent, ActivityStreamingBinding binding) {
        currentChosenCaller.set("A");
        callerAdapter = new CallerAdapter(requestMap, this);
        this.activity = activity;
        this.binding = binding;

        int role = intent.getIntExtra("role", 0);
        // 1-->主播  2-->高级观众 3-->普通观众
        if (role != 1) {
            throw new RuntimeException("Should not reach here");
        }
        mRole = getUserRoleByType(role);
        mRoomName = intent.getStringExtra("room");
        pullUrlBase = intent.getStringExtra("url_play");
        pushUrlBase = intent.getStringExtra("url_push");

        LiveConfig.Builder builder = new LiveConfig.Builder();
        builder.setVideoWidth(720)
                .setVideoHeight(1280)
                .setVideoFPS(15)
                .setInitVideoBitrate(400000)
                .setMinVideoBitrate(100000)
                .setMaxVideoBitrate(800000)
                .setVideoEnabled(true)
                .setAudioSampleRate(44100)
                .setAudioBitrate(64000)
                .setCameraOrientation(0)
                .setAudioEnabled(true)
                .setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK);
        mSession = new LiveStreamSession(activity, builder.build());
        mSession.setRtmpEventListener(this);

        mSession.setupDevice();
        mSession.setSurfaceHolder(binding.localPreview.getHolder());
        mSession.configRtmpSession(pushUrlBase + mRoomName + mRole.toString(), mRole);
    }

    public void setMuteAudio(boolean isMute) {
        mSession.setMuteAudio(isMute);
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
    protected void onResume() {

    }

    @Override
    protected void onPause() {

    }

    @Override
    public void onDestroy() {
        if (binding.remotePreview1.isPlaying()) {
            binding.remotePreview1.stopPlayback();
        }
        if (binding.remotePreview2.isPlaying()) {
            binding.remotePreview2.stopPlayback();
        }
        mSession.stopStreaming();
        mSession.destroyRtmpSession();
        mSession.releaseDevice();
    }

    @Override
    public void onSessionConnected() {
        Log.d(TAG, "onSessionConnected: ");
        mSession.startStreaming();
    }

    @Override
    public void onError(int errorCode) {
        Log.d(TAG, "onError: " + errorCode);
    }

    @Override
    public void onConversationRequest(final String callerUrl, final String userId) {
        if (requestMap.size() == 2 && !requestMap.containsKey(userId)) {
            mSession.stopCallWith(callerUrl, userId);
        }
        requestMap.put(userId, callerUrl);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requesting.set(true);
                callerAdapter.notifyDataSetChanged();
            }
        });
    }

    private Disposable timeTask;

    @Override
    public void onConversationStarted(String userId) {
        Log.d(TAG, "onConversationStarted: " + userId);
        playUrl(pullUrlBase + userId);
        if (timeTask == null || timeTask.isDisposed()) {
            timeTask = Flowable.interval(1, TimeUnit.SECONDS)
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(@NonNull Long aLong) throws Exception {
                            mConversationTime.set(valueToString(aLong));
                        }
                    });
        }
    }

    @Override
    public void onConversationFailed(final String userId, final FailureReason failReasonCode) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (requestMap.containsKey(userId)) {
                    requestMap.remove(userId);
                    if (requestMap.size() == 0) {
                        requesting.set(false);
                    }
                    callerAdapter.notifyDataSetChanged();
                }
                Toast.makeText(activity, "Start conversation failed with reason " + failReasonCode
                        .toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConversationEnded(final String userId) {
        stopPlayUrl(pullUrlBase + userId);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 如果在用户主动取消的时候主播还没有做出应答，则会调用该代码
                if (requestMap.containsKey(userId)) {
                    requestMap.remove(userId);
                    if (requestMap.size() == 0) {
                        requesting.set(false);
                    }
                    callerAdapter.notifyDataSetChanged();
                }
                if (!preview1Playing.get() && !preview2Playing.get()) {
                    if (timeTask != null && !timeTask.isDisposed()) {
                        timeTask.dispose();
                    }
                    timeShowing.set(false);
                }
                Toast.makeText(activity, "Conversation with user " + userId + " was ended.", Toast
                        .LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onSelectCaller(String userId, String character) {
        currentChosenCaller.set(character);
        currentChosenCallerID.set(userId);
    }

    public void onClickAccept(View view) {
        mSession.startCallWith(requestMap.get(currentChosenCallerID.get()), currentChosenCallerID.get());
        requestMap.remove(currentChosenCallerID.get());
        if (requestMap.size() == 0) {
            requesting.set(false);
        }
        callerAdapter.notifyDataSetChanged();
    }

    public void onClickRefuse(View view) {
        mSession.stopCallWith(requestMap.get(currentChosenCallerID.get()), currentChosenCallerID.get());
        requestMap.remove(currentChosenCallerID.get());
        if (requestMap.size() == 0) {
            requesting.set(false);
        }
        callerAdapter.notifyDataSetChanged();
    }

    /**
     * 根据播放地址选择播放器播放
     */
    private void playUrl(final String pullUrl) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timeShowing.set(true);
                if (!binding.remotePreview1.isPlaying()) {
                    preview1Playing.set(true);
                    binding.remotePreview1.setVideoPath(pullUrl);
                    binding.remotePreview1.start();
                } else if (!binding.remotePreview2.isPlaying()) {
                    preview2Playing.set(true);
                    binding.remotePreview2.setVideoPath(pullUrl);
                    binding.remotePreview2.start();
                } else {
                    Toast.makeText(activity, "播放器数量不够了", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    /**
     * 根据播放地址找到对应的播放器并停止播放
     */
    private void stopPlayUrl(final String pullUrl) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding.remotePreview1.isPlaying()
                        && binding.remotePreview1.getCurrentPlayingUrl().equals(pullUrl)) {
                    binding.remotePreview1.stopPlayback();
                    preview1Playing.set(false);
                } else if (binding.remotePreview2.isPlaying()
                        && binding.remotePreview2.getCurrentPlayingUrl().equals(pullUrl)) {
                    binding.remotePreview2.stopPlayback();
                    preview2Playing.set(false);
                } else {
                    Log.d(TAG, "run: 该地址 " + pullUrl + "并未开始播放");
                }
            }
        });
    }

    public void onClickClosePreview1(View view) {
        if (binding.remotePreview1.isPlaying()) {
            String remoteUrl = binding.remotePreview1.getCurrentPlayingUrl();
            String userId = remoteUrl.substring(remoteUrl.lastIndexOf('/') + 1);
            mSession.stopCallWith(pushUrlBase + userId, userId);
        }
    }

    public void onClickClosePreview2(View view) {
        if (binding.remotePreview2.isPlaying()) {
            String remoteUrl = binding.remotePreview2.getCurrentPlayingUrl();
            String userId = remoteUrl.substring(remoteUrl.lastIndexOf('/') + 1);
            mSession.stopCallWith(pushUrlBase + userId, userId);
        }
    }

    public void onClickEndCall(View view) {
        onClickClosePreview1(view);
        onClickClosePreview2(view);
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

}
