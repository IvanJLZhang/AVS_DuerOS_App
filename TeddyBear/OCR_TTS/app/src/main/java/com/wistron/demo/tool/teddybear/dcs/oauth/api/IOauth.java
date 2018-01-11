package com.wistron.demo.tool.teddybear.dcs.oauth.api;

/**
 * 百度认证接口
 * Created by ivanjlzhang on 17-9-23.
 */

public interface IOauth {
    /**
     * 获取accessToken
     *
     * @return String
     */
    String getAccessToken();

    /**
     * 用户认证
     */
    void authorize();

    /**
     * 判断当前的token信息是否有效
     *
     * @return true/false
     */
    boolean isSessionValid();
}
