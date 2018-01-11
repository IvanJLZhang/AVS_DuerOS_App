package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * Audio Player模块上报各种事件的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class AudioPlayerPayload extends Payload {
    public String token;
    public long offsetInMilliseconds;

    public AudioPlayerPayload(String token, long offsetInMilliseconds) {
        this.token = token;
        this.offsetInMilliseconds = offsetInMilliseconds;
    }
}
