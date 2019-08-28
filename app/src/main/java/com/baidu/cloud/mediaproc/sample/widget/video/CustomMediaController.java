/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.cloud.mediaproc.sample.widget.video;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.MediaController;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.LayoutVideoPanelBinding;
import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;


public class CustomMediaController {

    private static final String TAG = "CustomMediaController";
    private MediaController.MediaPlayerControl mPlayer;
    private final Context mContext;
    private ViewGroup mAnchor;
    private ViewGroup mRoot;
    private FrameLayout.LayoutParams mDecorLayoutParams;
    public VideoViewModel viewModel;

    public CustomMediaController(Context context) {
        mContext = context;
        mDecorLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        mRoot = makeControllerView();
        mRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        viewModel.show(0); // show until hide is called
                        break;
                    case MotionEvent.ACTION_UP:
                        viewModel.show(); // start timeout
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        viewModel.showPanel.set(false);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        mRoot.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                final boolean uniqueDown = event.getRepeatCount() == 0
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        || keyCode == KeyEvent.KEYCODE_SPACE) {
                    if (uniqueDown) {
                        viewModel.onPauseResume(null);
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    if (uniqueDown && !mPlayer.isPlaying()) {
                        mPlayer.start();
                        viewModel.show();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    if (uniqueDown && mPlayer.isPlaying()) {
                        mPlayer.pause();
                        viewModel.show();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                        || keyCode == KeyEvent.KEYCODE_CAMERA) {
                    // don't show the controls for volume adjustment
                    return false;
                } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                    if (uniqueDown && viewModel.showPanel.get()) {
                        viewModel.showPanel.set(false);
                        return true;
                    }
                    return false;
                }

                viewModel.show();
                return false;
            }
        });
        mRoot.setFocusable(true);
        mRoot.setFocusableInTouchMode(true);
        mRoot.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        mRoot.requestFocus();
    }

    private void updateFloatingWindowLayout() {
        int[] anchorPos = new int[2];
        mAnchor.getLocationOnScreen(anchorPos);

        // we need to know the size of the controller so we can properly currentTime it
        // within its space
        mRoot.measure(View.MeasureSpec.makeMeasureSpec(mAnchor.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(mAnchor.getHeight(), View.MeasureSpec.AT_MOST));

        FrameLayout.LayoutParams p = mDecorLayoutParams;
        p.width = mAnchor.getWidth();
    }

    private final View.OnLayoutChangeListener mLayoutChangeListener =
            new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight,
                                           int oldBottom) {
                    updateFloatingWindowLayout();
                    if (mRoot.getParent() == mAnchor) {
                        mRoot.removeCallbacks(updateView);
                        mRoot.post(updateView);
                    }
                }
            };

    public void setMediaPlayer(BDCloudVideoView player) {
        mPlayer = player;
        viewModel.setMediaPlayer(player);
    }

    private Runnable updateView = new Runnable() {
        @Override
        public void run() {
            mAnchor.updateViewLayout(mRoot, mDecorLayoutParams);
        }
    };

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * When VideoView calls this method, it will use the VideoView's parent
     * as the anchor.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(ViewGroup view) {
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }

        updateFloatingWindowLayout();
        mAnchor.addView(mRoot, mDecorLayoutParams);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected ViewGroup makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = (ViewGroup) inflate.inflate(R.layout.layout_video_panel, null);

        LayoutVideoPanelBinding binding = DataBindingUtil.bind(mRoot);
        viewModel = new VideoViewModel(mContext);
        binding.setModel(viewModel);

        return mRoot;
    }

    public void setVideoInfo(VideoInfo videoInfo) {
        viewModel.setVideoInfo(videoInfo);
    }
}
