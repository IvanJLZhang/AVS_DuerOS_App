package com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * setVolume指令对应的Payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class SetVolumePayload extends Payload {
    private long volume;

    public SetVolumePayload() {
    }

    public SetVolumePayload(long volume) {
        this.volume = volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public long getVolume() {
        return volume;
    }
}
