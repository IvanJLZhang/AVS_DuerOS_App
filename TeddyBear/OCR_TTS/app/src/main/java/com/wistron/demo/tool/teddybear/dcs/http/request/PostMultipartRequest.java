package com.wistron.demo.tool.teddybear.dcs.http.request;

import com.wistron.demo.tool.teddybear.dcs.http.builder.PostMultipartBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 用于post multipart请求
 * Created by ivanjlzhang on 17-9-22.
 */

public class PostMultipartRequest extends OkHttpRequest {
    private List<PostMultipartBuilder.Multipart> multiParts;

    public PostMultipartRequest(String url,
                                Object tag,
                                Map<String, String> params,
                                Map<String, String> headers,
                                LinkedList<PostMultipartBuilder.Multipart> multiParts,
                                int id) {
        super(url, tag, params, headers, id);
        this.multiParts = multiParts;
    }

    @Override
    protected RequestBody buildRequestBody() {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        addParams(builder);
        return builder.build();
    }

    @Override
    protected Request buildRequest(RequestBody requestBody) {
        return builder.post(requestBody).build();
    }

    private void addParams(MultipartBody.Builder builder) {
        if (multiParts != null && !multiParts.isEmpty()) {
            for (int i = 0; i < multiParts.size(); i++) {
                PostMultipartBuilder.Multipart part = multiParts.get(i);
                builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + part.key + "\""),
                        part.requestBody);
            }
        }
    }
}
