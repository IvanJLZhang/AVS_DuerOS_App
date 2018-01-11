package com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * setEndPoint指令对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class SetEndPointPayload extends Payload {
    private String endpoint;

    public SetEndPointPayload() {

    }

    public SetEndPointPayload(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }
}
