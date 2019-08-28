package com.baidu.cloud.mediaproc.sample.ui.lss;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityStreamingBinding;
import com.baidu.cloud.mediaproc.sample.ui.lss.viewmodel.StreamViewModel;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StreamingActivity extends AppCompatActivity {
    private static final String TAG = "StreamingActivity";

    private ActivityStreamingBinding binding;
    private StreamViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.requestFeature(Window.FEATURE_NO_TITLE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_streaming);
        binding.remotePreview1.setVideoScalingMode(BDCloudVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        binding.remotePreview2.setVideoScalingMode(BDCloudVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
        binding.remotePreview1.setMaxProbeTime(50);
        binding.remotePreview1.setMaxCacheSizeInBytes(60 * 1024);
        binding.remotePreview1.setBufferTimeInMs(200);
        binding.remotePreview1.setMaxProbeSize(16 * 2048);
        binding.remotePreview1.toggleFrameChasing(true);
        binding.remotePreview2.setMaxProbeTime(50);
        binding.remotePreview2.setMaxCacheSizeInBytes(60 * 1024);
        binding.remotePreview2.setBufferTimeInMs(200);
        binding.remotePreview2.setMaxProbeSize(16 * 2048);
        binding.remotePreview2.toggleFrameChasing(true);

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        am.setMode(AudioManager.MODE_IN_CALL);

        Intent i = getIntent();
        viewModel = new StreamViewModel(this, i, binding);
        binding.setModel(viewModel);
        binding.callPanel.callerList.setAdapter(viewModel.callerAdapter);

    }

    @Override
    protected void onDestroy() {
        viewModel.onDestroy();
        super.onDestroy();
    }

    public void onClickClose(View view) {
        finish();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 4:
                // 推流结束
                Toast.makeText(this, "推流结束", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

}