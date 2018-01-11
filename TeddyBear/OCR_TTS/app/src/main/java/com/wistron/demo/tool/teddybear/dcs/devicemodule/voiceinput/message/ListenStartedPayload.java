package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * ListenStarted事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class ListenStartedPayload extends Payload {
    public static final String FORMAT = "AUDIO_L16_RATE_16000_CHANNELS_1";
    private String format;

    public ListenStartedPayload(String format) {
        this.format = format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }
}
