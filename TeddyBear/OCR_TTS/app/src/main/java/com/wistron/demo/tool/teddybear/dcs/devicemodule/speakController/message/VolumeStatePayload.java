package com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * 本模块端状态对应的Payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class VolumeStatePayload extends Payload {
    private long volume;
    private boolean muted;

    public VolumeStatePayload() {
    }

    public VolumeStatePayload(long volume, boolean muted) {
        this.volume = volume;
        this.muted = muted;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getVolume() {
        return this.volume;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean getMuted() {
        return muted;
    }
}
