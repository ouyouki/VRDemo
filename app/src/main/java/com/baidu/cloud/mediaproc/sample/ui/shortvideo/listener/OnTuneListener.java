package com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener;

public interface OnTuneListener {

    void onResolutionChose(int width, int height);

    void onTimeChose(int seconds);

    void onVolumeChange(float volume);

    /**
     * 亮度
     *
     * @param brightness 范围从 -1.0 到 1.0，默认为 0
     */
    void onBrightnessChange(float brightness);

    /**
     * 对比度
     *
     * @param contrast 范围从 0.0 到 4.0，默认为 1.0
     */
    void onContrastChange(float contrast);

    /**
     * 饱和度
     *
     * @param saturation 范围从 0.0 到 2.0，默认为 1.0
     */
    void onSaturationChange(float saturation);

    /**
     * 色温
     *
     * @param hue 范围从 -180.0 到 180.0，默认为 0.0
     */
    void onHueChange(float hue);

    /**
     * 锐度
     *
     * @param sharpness 范围从 -4.0 到 4.0，默认 0.0
     */
    void onSharpnessChange(float sharpness);
}