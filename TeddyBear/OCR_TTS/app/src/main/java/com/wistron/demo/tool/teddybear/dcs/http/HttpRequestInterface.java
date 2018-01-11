package com.wistron.demo.tool.teddybear.dcs.http;

import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.http.callback.DcsCallback;

/**
 * 网络请求工具接口
 * Created by ivanjlzhang on 17-9-22.
 */

public interface HttpRequestInterface {
    /**
     * 异步Post EventString请求
     *
     * @param requestBody 请求信息体
     * @param dcsCallback 结果回调接口
     */
    void doPostEventStringAsync(DcsRequestBody requestBody, DcsCallback dcsCallback);

    /**
     * 异步PostMultipart请求
     *
     * @param requestBody       请求信息体
     * @param streamRequestBody 请求信息体stream
     * @param dcsCallback       结果回调接口
     */
    void doPostEventMultipartAsync(DcsRequestBody requestBody,
                                   DcsStreamRequestBody streamRequestBody,
                                   DcsCallback dcsCallback);

    /**
     * 异步Get Directives请求
     *
     * @param requestBody 请求信息体
     * @param dcsCallback 结果回调接口
     */
    void doGetDirectivesAsync(DcsRequestBody requestBody, DcsCallback dcsCallback);

    /**
     * 异步Get Ping请求
     *
     * @param requestBody 请求信息体
     * @param dcsCallback 结果回调接口
     */
    void doGetPingAsync(DcsRequestBody requestBody, DcsCallback dcsCallback);

    /**
     * 取消请求
     *
     * @param requestTag 请求标识
     */
    void cancelRequest(Object requestTag);
}
