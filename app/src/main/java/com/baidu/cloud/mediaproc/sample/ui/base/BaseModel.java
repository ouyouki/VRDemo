package com.baidu.cloud.mediaproc.sample.ui.base;

/**
 * 视图模型的基类
 * Created by wenyiming on 19/04/2017.
 */

public abstract class BaseModel {

    protected abstract void onResume();

    protected abstract void onPause();

    protected abstract void onDestroy();
}
