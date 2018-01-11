package com.wistron.demo.tool.teddybear.dcs.http.builder;

import com.wistron.demo.tool.teddybear.dcs.http.request.PostStringRequest;
import com.wistron.demo.tool.teddybear.dcs.http.request.RequestCall;

import okhttp3.MediaType;

/**
 *
 * Created by ivanjlzhang on 17-9-23.
 */

public class PostStringBuilder extends OkHttpRequestBuilder {
    // 内容
    private String content;
    // 类型
    private MediaType mediaType;

    public PostStringBuilder content(String content) {
        this.content = content;
        return this;
    }

    public PostStringBuilder mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public RequestCall build() {
        return new PostStringRequest(url, tag, params, headers, content, mediaType, id).build();
    }
}
