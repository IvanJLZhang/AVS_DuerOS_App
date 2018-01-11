package com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * LinkClicked事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class LinkClickedPayload extends Payload {
    private String url;

    public LinkClickedPayload(String url) {
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
