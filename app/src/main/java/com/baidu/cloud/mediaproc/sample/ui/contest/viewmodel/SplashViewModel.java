package com.baidu.cloud.mediaproc.sample.ui.contest.viewmodel;

import android.content.Intent;
import android.view.View;

import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.ui.contest.ContestActivity;


public class SplashViewModel extends BaseModel {

    private String playUrl;
    private String hostUrl;

    public SplashViewModel(final String playUrl, String hostUrl) {
        this.playUrl = playUrl;
        this.hostUrl = hostUrl;
    }

    public void onClickStart(View view) {
        Intent intent = new Intent(view.getContext(), ContestActivity.class);
        intent.putExtra("url_play", playUrl);
        intent.putExtra("url_host", hostUrl);
        view.getContext().startActivity(intent);
    }

    @Override
    protected void onResume() {

    }

    @Override
    protected void onPause() {

    }

    @Override
    protected void onDestroy() {

    }
}
