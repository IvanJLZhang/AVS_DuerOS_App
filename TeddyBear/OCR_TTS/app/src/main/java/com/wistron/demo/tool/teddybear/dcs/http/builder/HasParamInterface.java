package com.wistron.demo.tool.teddybear.dcs.http.builder;

import java.util.Map;

/**
 * 请求是否包含参数
 * Created by ivanjlzhang on 17-9-22.
 */

public interface HasParamInterface {
    /**
     * 构建请求参数
     *
     * @param params 请求参数
     * @return OkHttpRequestBuilder
     */
    OkHttpRequestBuilder params(Map<String, String> params);

    /**
     * @param key 请求参数的key
     * @param val 请求参数的值
     * @return OkHttpRequestBuilder
     */
    OkHttpRequestBuilder addParams(String key, String val);

}
