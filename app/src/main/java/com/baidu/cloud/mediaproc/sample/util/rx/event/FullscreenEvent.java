package com.baidu.cloud.mediaproc.sample.util.rx.event;

/**
 * Created by wenyiming on 24/10/2017.
 */

public class FullscreenEvent {

    public boolean isFullscreen() {
        return isFullscreen;
    }

    private final boolean isFullscreen;

    public FullscreenEvent(boolean fullscreen) {
        isFullscreen = fullscreen;
    }
}
