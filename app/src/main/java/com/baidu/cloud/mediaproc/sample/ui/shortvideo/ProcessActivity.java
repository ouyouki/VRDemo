package com.baidu.cloud.mediaproc.sample.ui.shortvideo;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityProcessBinding;
import com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel.ProcessViewModel;
import com.baidu.cloud.mediaproc.sample.util.model.ProcessParam;
import com.bumptech.glide.Glide;

public class ProcessActivity extends AppCompatActivity {

    private ActivityProcessBinding binding;
    private ProcessViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_process);

        ProcessParam param = getIntent().getParcelableExtra("param");
        viewModel = new ProcessViewModel(this, param, binding.btnCapture);
        binding.setModel(viewModel);

        Glide.with(this)
                .load(param.mediaFilePath)
                .centerCrop()
                .into(binding.imageView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.onDestroy();
    }

    public void onClickClose(View view) {
        startActivity(new Intent(this, ShortVideoActivity.class));
    }
}
