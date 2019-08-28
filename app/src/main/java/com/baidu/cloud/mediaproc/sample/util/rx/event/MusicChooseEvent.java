package com.baidu.cloud.mediaproc.sample.util.rx.event;

import com.baidu.cloud.mediaproc.sample.util.model.Music;

/**
 * Created by wenyiming on 24/10/2017.
 */

public class MusicChooseEvent {


    private final Music music;

    public MusicChooseEvent(Music music) {
        this.music = music;
    }

    public Music getMusic() {
        return music;
    }
}
