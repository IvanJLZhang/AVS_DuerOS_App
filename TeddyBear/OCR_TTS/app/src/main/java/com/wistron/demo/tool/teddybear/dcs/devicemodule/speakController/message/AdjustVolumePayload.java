package com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * adjustVolume指令对应的Payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class AdjustVolumePayload extends Payload {
    private long volume;

    public AdjustVolumePayload() {
    }

    public AdjustVolumePayload(long volume) {
        this.volume = volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getVolume() {
        return volume;
    }
}
