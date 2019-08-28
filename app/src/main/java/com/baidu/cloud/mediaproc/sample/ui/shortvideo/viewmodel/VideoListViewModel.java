package com.baidu.cloud.mediaproc.sample.ui.shortvideo.viewmodel;

import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.view.View;

import com.baidu.cloud.mediaproc.sample.ui.base.BaseModel;
import com.baidu.cloud.mediaproc.sample.util.model.VideoInfo;


/**
 * VideoAdapter 中的视图模型类，负责逻辑和数据的控制
 * Created by wenyiming on 02/05/2017.
 */

public class VideoListViewModel extends BaseModel {
    private static final String TAG = "VideoListViewModel";

    public ObservableField<String> description = new ObservableField<>();

    public ObservableBoolean dirty = new ObservableBoolean(false);
    public ObservableBoolean like = new ObservableBoolean(false);

    private VideoInfo videoInfo;

    public void setVideoInfo(VideoInfo info) {
        description.set(info.description);
        like.set(info.like);
        videoInfo = info;
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

    public void onClickStar(View view) {
        videoInfo.like = !videoInfo.like;
        like.set(videoInfo.like);
    }
}
