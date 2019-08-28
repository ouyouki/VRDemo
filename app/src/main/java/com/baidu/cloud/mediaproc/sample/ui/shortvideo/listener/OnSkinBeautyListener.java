package com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener;

public interface OnSkinBeautyListener {

    /**
     * 磨皮程度
     *
     * @param smooth 范围从 0.0 到 1.0，默认为 0.0
     */
    void onSmoothChange(float smooth);

    /**
     * 美白程度
     *
     * @param bright 范围从 0.0 到 1.0，默认为 0.0
     */
    void onBrightChange(float bright);

    /**
     * 粉嫩程度
     *
     * @param pink 范围从 0.0 到 1.0，默认为 0.0
     */
    void onPinkChange(float pink);

}