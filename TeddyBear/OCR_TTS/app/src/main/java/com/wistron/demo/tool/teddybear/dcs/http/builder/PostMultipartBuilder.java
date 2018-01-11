package com.wistron.demo.tool.teddybear.dcs.http.builder;

import com.wistron.demo.tool.teddybear.dcs.http.request.PostMultipartRequest;
import com.wistron.demo.tool.teddybear.dcs.http.request.RequestCall;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import okhttp3.RequestBody;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class PostMultipartBuilder extends OkHttpRequestBuilder<PostMultipartBuilder>
implements HasParamInterface{
    private LinkedList<Multipart> multiParts = new LinkedList<>();

    @Override
    public RequestCall build() {
        return new PostMultipartRequest(url, tag, params, headers, multiParts, id).build();
    }

    @Override
    public PostMultipartBuilder params(Map<String, String> params) {
        this.params = params;
        return this;
    }

    @Override
    public PostMultipartBuilder addParams(String key, String val) {
        if (this.params == null) {
            params = new LinkedHashMap<>();
        }
        params.put(key, val);
        return this;
    }

    /**
     * 添加多个Multipart
     *
     * @param map map集合
     * @return 当前对象
     */
    public PostMultipartBuilder multiParts(Map<String, RequestBody> map) {
        if (map != null) {
            multiParts = new LinkedList<>();
        }
        for (String k : map.keySet()) {
            this.multiParts.add(new Multipart(k, map.get(k)));
        }
        return this;
    }

    /**
     * 添加一个Multipart
     *
     * @param name name
     * @param body body
     * @return 当前对象
     */
    public PostMultipartBuilder addMultiPart(String name, RequestBody body) {
        multiParts.add(new Multipart(name, body));
        return this;
    }

    /**
     * 请求体body-Multipart
     */
    public static final class Multipart implements Serializable {
        // body的key
        public String key;
        // body的内容
        public RequestBody requestBody;

        public Multipart(String name, RequestBody requestBody) {
            this.key = name;
            this.requestBody = requestBody;
        }
    }
}
