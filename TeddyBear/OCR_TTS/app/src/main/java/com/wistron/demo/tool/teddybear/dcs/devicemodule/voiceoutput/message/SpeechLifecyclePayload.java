package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * SpeechStarted和SpeechFinished事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class SpeechLifecyclePayload extends Payload {
    private String token;

    public SpeechLifecyclePayload(String token) {
        this.token = token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
