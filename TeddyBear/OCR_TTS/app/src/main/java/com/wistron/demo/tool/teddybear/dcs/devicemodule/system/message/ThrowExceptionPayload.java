package com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class ThrowExceptionPayload extends Payload {
    private String code;
    private String description;

    public ThrowExceptionPayload() {
    }

    public ThrowExceptionPayload(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "code=" + code + "  description=" + description;
    }
}
