package com.baidu.cloud.mediaproc.sample.ui.lss;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.widget.CaptureProgressView;
import com.baidu.cloud.mediastream.session.LiveScreenStreamSession;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
final class ScreenCaptureController extends FrameLayout implements View.OnClickListener {

    private static final String TAG = "ScreenCaptureController";
    private WindowManager mWindowManager;
    private CaptureProgressView progressView;
    private ViewDismissListener mViewDismissListener;
    private LiveScreenStreamSession mSession;

    private WindowManager.LayoutParams layoutParams;
    private boolean isDragging;
    private boolean isStart;
    private boolean isPaused;

    public ScreenCaptureController(Context context, LiveScreenStreamSession session) {
        super(context);
        init(context);
        mSession = session;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private void init(final Context context) {
        View mWholeView = View.inflate(context, R.layout.floating_screen_streaming, null);

        progressView = (CaptureProgressView) mWholeView.findViewById(R.id.btn_capture);
        progressView.setOnClickListener(this);
        progressView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                isDragging = true;
                return true;
            }
        });

        addView(mWholeView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    public void setViewDismissHandler(ViewDismissListener viewDismissListener) {
        mViewDismissListener = viewDismissListener;
    }

    public void show() {
        int w = WindowManager.LayoutParams.WRAP_CONTENT;
        int h = WindowManager.LayoutParams.WRAP_CONTENT;

        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 解决Android 7.1.1起不能再用Toast的问题（先解决crash）
            if (Build.VERSION.SDK_INT > 24) {
                type = WindowManager.LayoutParams.TYPE_PHONE;
            } else {
                type = WindowManager.LayoutParams.TYPE_TOAST;
            }
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams = new WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.CENTER;
        mWindowManager.addView(this, layoutParams);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getRawX();
                lastY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                // 相对于屏幕左上角的位置
                if (isDragging) {
                    layoutParams.x += (int) ((event.getRawX() - lastX));
                    layoutParams.y += (int) ((event.getRawY() - lastY));
                    mWindowManager.updateViewLayout(this, layoutParams);
                }
                lastX = event.getRawX();
                lastY = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                isDragging = false;
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    float lastX = 0;
    float lastY = 0;

    @Override
    public void onClick(View v) {
        if (!isDragging) {
            if (!isStart) {
                // 还没开始推流
                isStart = true;
                mSession.startStreaming();
                progressView.setShowInnerBackground(false);
                progressView.setAttributeResourceId(R.drawable.ic_pause_white_24dp);
            } else if (!isPaused) {
                // 推流中
                mSession.pauseStreaming();
                isPaused = true;
                progressView.setAttributeResourceId(R.drawable.ic_play_arrow_white_24dp);
            } else {
                // 推流暂停中
                mSession.resumeStreaming();
                isPaused = false;
                progressView.setAttributeResourceId(R.drawable.ic_pause_white_24dp);
            }
        }
    }

    void postOnSessionConnected() {
        post(new Runnable() {
            @Override
            public void run() {
                progressView.setEnabled(true);
            }
        });
    }

    public void removePoppedViewAndClear() {
        // remove view
        if (mWindowManager != null) {
            mWindowManager.removeView(this);
        }
        if (mViewDismissListener != null) {
            mViewDismissListener.onViewDismiss();
        }
    }

    interface ViewDismissListener {
        void onViewDismiss();
    }
}
