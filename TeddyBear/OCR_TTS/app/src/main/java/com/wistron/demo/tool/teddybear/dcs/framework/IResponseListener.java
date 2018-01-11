package com.wistron.demo.tool.teddybear.dcs.framework;

/**
 * 网络请求回调
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IResponseListener {
    /**
     * 成功回调
     *
     * @param statusCode http返回statusCode
     */
    void onSucceed(int statusCode);

    /**
     * 失败回调
     *
     * @param errorMessage 出错的异常信息
     */
    void onFailed(String errorMessage);

}
