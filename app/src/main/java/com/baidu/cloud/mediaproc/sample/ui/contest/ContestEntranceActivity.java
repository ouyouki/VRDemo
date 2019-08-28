package com.baidu.cloud.mediaproc.sample.ui.contest;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.baidu.cloud.mediaproc.sample.R;
import com.baidu.cloud.mediaproc.sample.databinding.ActivityContestEntranceBinding;
import com.jakewharton.rxbinding2.widget.RxTextView;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class ContestEntranceActivity extends AppCompatActivity {
    private ActivityContestEntranceBinding binding;

    private String playUrl;
    private String hostUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_contest_entrance);
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
//            actionBar.setHomeButtonEnabled(true);
        }
        initUIandEvent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initUIandEvent() {
        RxTextView.textChanges(binding.etUrlPlay)
                .map(new Function<CharSequence, Boolean>() {
                    @Override
                    public Boolean apply(CharSequence url) throws Exception {
                        if (TextUtils.isEmpty(url)) {
                            return false;
                        }
                        playUrl = url.toString();
                        return true;
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        binding.btnPlay.setEnabled(aBoolean);
                    }
                });
        RxTextView.textChanges(binding.etUrlHost)
                .map(new Function<CharSequence, Boolean>() {
                    @Override
                    public Boolean apply(CharSequence url) throws Exception {
                        if (TextUtils.isEmpty(url)) {
                            return false;
                        }
                        hostUrl = url.toString();
                        return true;
                    }
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        binding.btnPlay.setEnabled(aBoolean);
                    }
                });
        binding.etUrlPlay.setText("rtmp://zhongceplay.kaywang.cn/baiduyun/zhongce");
        // FIXME: 08/02/2018 您必须在此替换您自己的答题服务器地址
        binding.etUrlHost.setText("http://180.76.115.172:8080/");
    }

    public void onClickPlay(View view) {
        Intent i = new Intent(ContestEntranceActivity.this, ContestSplashActivity.class);
        i.putExtra("url_play", playUrl);
        i.putExtra("url_host", hostUrl);
        startActivity(i);
    }
}
