package com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * setMuted指令对应的Payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class SetMutePayload extends Payload {
    private boolean mute;

    public SetMutePayload() {
    }

    public SetMutePayload(boolean mute) {
        this.mute = mute;
    }

    public void setMute(boolean mute) {
        this.mute = mute;
    }

    public boolean getMute() {
        return this.mute;
    }
}
