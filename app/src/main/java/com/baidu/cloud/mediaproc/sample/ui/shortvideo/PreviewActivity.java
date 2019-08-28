package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityPreviewBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.adapter.VideoCutAdapter;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel.PreviewViewModel;
import com.bumptech.glide.Glide;

import static com.baidu.cloud.mediaproc.sample.ui.shortvideo.ConfigProcessActivity.EXTRA_VIDEO_PATH;

public class PreviewActivity extends AppCompatActivity {

    private ActivityPreviewBinding binding;
    private PreviewViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        win.requestFeature(Window.FEATURE_NO_TITLE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_preview);

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        Log.d("PreviewViewModel", "onCreate: " + videoPath);

        viewModel = new PreviewViewModel(this, binding, videoPath);
        binding.setModel(viewModel);
        binding.recyclerView.setAdapter(new VideoCutAdapter(this, videoPath));
        Glide.with(this)
                .load(videoPath)
                .fitCenter()
                .into(binding.imageView);
    }

    public void onClickTopLeft(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.onResume();
    }
}
