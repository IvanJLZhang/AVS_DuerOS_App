package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * DeleteAlert指令对应的payload结构
 * Created by ivanjlzhang on 17-9-21.
 */

public class DeleteAlertPayload extends Payload {
    public String token;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
