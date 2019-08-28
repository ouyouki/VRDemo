package com.baidu.cloud.mediaproc.sample.ui.lss;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.baidu.cloud.bdrtmpsession.BDRtmpSessionBasic;
import com.baidu.cloud.bdrtmpsession.OnSessionEventListener;
import com.baidu.cloud.mediastream.config.LiveConfig;
import com.baidu.cloud.mediastream.session.LiveScreenStreamSession;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public final class ScreenStreamingService extends Service
        implements ScreenCaptureController.ViewDismissListener, OnSessionEventListener {

    private static final String TAG = "ScreenStreamingService";
    private String pushUrlBase = "";

    private ScreenCaptureController mTipViewController;
    private LiveScreenStreamSession mSession;
    private BDRtmpSessionBasic.UserRole mRole = BDRtmpSessionBasic.UserRole.Host;
    private String mRoomName = "";

    private int mResultCode;
    private Intent mResultData;
    private boolean sessionConnected;

    public static void start(Context context, @NonNull Intent i) {
        Intent serviceIntent = new Intent(context, ScreenStreamingService.class);
        serviceIntent.putExtras(i.getExtras());
        context.startService(serviceIntent);
    }

    private static BDRtmpSessionBasic.UserRole getUserRoleByType(int type) {
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
    public void onCreate() {
        LiveConfig.Builder builder = new LiveConfig.Builder();
        builder.setVideoWidth(1280)
                .setVideoHeight(720)
                .setVideoFPS(30)
                .setCameraOrientation(0)
                .setVideoEnabled(true)
                .setAudioEnabled(true);
        mSession = new LiveScreenStreamSession(this, builder.build());
        mSession.setRtmpEventListener(this);
        mSession.setupDevice();
        mSession.setCaptureErrorListener(new LiveScreenStreamSession.CaptureErrorListener() {
            @Override
            public void onError(int error, String desc) {
                Toast.makeText(ScreenStreamingService.this, desc, Toast.LENGTH_SHORT).show();
            }
        });
        mTipViewController = new ScreenCaptureController(getApplication(), mSession);
        mTipViewController.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.stopStreaming();
        mSession.destroyRtmpSession();
        mSession.releaseDevice();
        sessionConnected = false;
        if (mTipViewController != null) {
            mTipViewController.setViewDismissHandler(null);
            mTipViewController.removePoppedViewAndClear();
            mTipViewController = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent.toString());
        int role = intent.getIntExtra("role", 0);
        // 1-->主播  2-->高级观众 3-->普通观众
        if (role != 1) {
            throw new RuntimeException("Should not reach here");
        }
        mRole = getUserRoleByType(role);
        mRoomName = intent.getStringExtra("room");
        mResultCode = intent.getIntExtra("result_code", 0);
        mResultData = intent.getParcelableExtra("result_data");
        pushUrlBase = intent.getStringExtra("url_push");
        mSession.startMediaProjection(mResultCode, mResultData);
        if (!sessionConnected) {
            mSession.configRtmpSession(pushUrlBase + mRoomName + mRole.toString(), mRole);
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onViewDismiss() {
        stopSelf();
    }

    @Override
    public void onSessionConnected() {
        sessionConnected = true;
        mTipViewController.postOnSessionConnected();
    }

    @Override
    public void onError(int errorCode) {
        Log.d(TAG, "onError: " + errorCode);
    }

    @Override
    public void onConversationRequest(String url, String userId) {
    }

    @Override
    public void onConversationStarted(String userId) {

    }

    @Override
    public void onConversationFailed(String userId, OnSessionEventListener.FailureReason failReasonCode) {

    }

    @Override
    public void onConversationEnded(String userId) {

    }
}