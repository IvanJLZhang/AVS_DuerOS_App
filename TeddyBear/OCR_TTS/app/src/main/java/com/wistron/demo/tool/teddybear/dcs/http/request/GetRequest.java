package com.wistron.demo.tool.teddybear.dcs.http.request;

import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * 普通GetRequest
 * Created by ivanjlzhang on 17-9-22.
 */

public class GetRequest extends OkHttpRequest {
    public GetRequest(String url,
                      Object tag,
                      Map<String, String> params,
                      Map<String, String> headers,
                      int id) {
        super(url, tag, params, headers, id);
    }

    @Override
    protected RequestBody buildRequestBody() {
        return null;
    }

    @Override
    protected Request buildRequest(RequestBody requestBody) {
        return builder.get().build();
    }
}
