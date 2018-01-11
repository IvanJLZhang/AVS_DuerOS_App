package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * Listen指令对应的payload结构
 * Created by ivanjlzhang on 17-9-23.
 */

public class ListenPayload extends Payload {
    private long timeoutInMilliseconds;

    public void setTimeoutInMilliseconds(long timeoutInMilliseconds) {
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }

    public long getTimeoutInMilliseconds() {
        return timeoutInMilliseconds;
    }
}
