package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * Alerts模块上报各种事件的payload结构
 * Created by ivanjlzhang on 17-9-21.
 */

public class AlertPayload extends Payload {
    // 本闹钟的唯一token
    public String token;

    public AlertPayload(String token) {
        this.token = token;
    }
}
