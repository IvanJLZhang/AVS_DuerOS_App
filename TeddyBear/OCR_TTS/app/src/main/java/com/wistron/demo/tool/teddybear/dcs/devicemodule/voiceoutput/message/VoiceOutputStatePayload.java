package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * 本模块端状态对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class VoiceOutputStatePayload extends Payload {
    public String token;
    public long offsetInMilliseconds;
    public String playerActivity;

    public VoiceOutputStatePayload(String token, long offsetInMilliseconds, String playerActivity) {
        this.token = token;
        this.offsetInMilliseconds = offsetInMilliseconds;
        this.playerActivity = playerActivity;
    }
}
