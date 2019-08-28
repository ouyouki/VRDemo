package com.baidu.cloud.mediaproc.sample.ui.contest;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityContestBinding;
import com.baidu.cloud.mediaproc.sample.ui.base.RxAppCompatActivity;
import com.baidu.cloud.mediaproc.sample.ui.contest.viewmodel.ContestViewModel;
import com.baidu.cloud.mediaproc.sample.widget.video.BDCloudVideoView;

public class ContestActivity extends RxAppCompatActivity {

    private ActivityContestBinding binding;

    private ContestViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contest);
        String playUrl = getIntent().getStringExtra("url_play");
        String hostUrl = getIntent().getStringExtra("url_host");
        if (TextUtils.isEmpty(playUrl) || !(playUrl.startsWith("rtmp://") || playUrl.startsWith("http://"))) {
            Toast.makeText(this, "未传入播放地址或播放地址不正确", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (TextUtils.isEmpty(hostUrl) || !(hostUrl.startsWith("http://") || hostUrl.startsWith("https://"))) {
            Toast.makeText(this, "未传入服务器或服务器地址不正确", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        viewModel = new ContestViewModel(lifecycleSubject, binding, playUrl, hostUrl);
        binding.setModel(viewModel);

        // 播放器参数优化
        binding.videoView.setVideoScalingMode(BDCloudVideoView.VIDEO_SCALING_MODE_SCALE_TO_FIT);
        binding.videoView.setMaxProbeTime(50);
        binding.videoView.setMaxCacheSizeInBytes(1024 * 1024);
        binding.videoView.setBufferTimeInMs(200);
        binding.videoView.setMaxProbeSize(16 * 2048);
        binding.videoView.toggleFrameChasing(true);
        binding.videoView.setOnMetadataListener(viewModel);
        binding.videoView.setVideoPath(playUrl);
        binding.videoView.start();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewModel != null) {
            viewModel.onDestroy();
        }
        binding.videoView.stopPlayback();
    }
}
