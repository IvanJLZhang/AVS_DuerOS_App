package com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import java.io.Serializable;

/**
 * HtmlView指令对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class HtmlPayload extends Payload implements Serializable {
    private String url;
    private String token;

    public HtmlPayload() {
    }

    public HtmlPayload(String url, String token) {
        this.url = url;
        this.token = token;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
