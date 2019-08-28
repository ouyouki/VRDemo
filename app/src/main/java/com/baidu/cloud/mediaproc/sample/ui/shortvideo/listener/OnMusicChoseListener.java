package com.baidu.cloud.mediaproc.sample.ui.shortvideo.listener;

import com.baidu.cloud.mediaproc.sample.util.model.Music;

public interface OnMusicChoseListener {

    /**
     * @param music 选择的音乐，音乐 id 为 -1 即没有选择音乐
     */
    void onMusicChose(Music music);

    /**
     * @param volume 背景音音量，从 0.0 到 1.0，默认 1.0
     */
    void onMusicVolumeChange(float volume);

    /**
     * @param start 背景音开始时间，毫秒
     */
    void onIntervalChose(int start);

    void onMusicSetDone();
}