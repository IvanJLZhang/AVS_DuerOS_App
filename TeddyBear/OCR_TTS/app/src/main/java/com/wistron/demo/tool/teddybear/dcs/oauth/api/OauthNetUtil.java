package com.wistron.demo.tool.teddybear.dcs.oauth.api;

import android.os.Bundle;

import com.wistron.demo.tool.teddybear.dcs.util.CommonUtil;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class OauthNetUtil {
    /**
     * 提取回调url中的token信息，用于User-Agent Flow中的授权操作
     * @param url 回调的url，包括token信息
     * @return 返回bundle类型的token信息
     */
    public static Bundle parseUrl(String url){
        Bundle ret;
        url = url.replace("bdconnect", "http");
        try {
            URL urlParams = new URL(url);
            ret = CommonUtil.decodeUrl(urlParams.getQuery());
            ret.putAll(CommonUtil.decodeUrl(urlParams.getRef()));
            return ret;
        }catch (MalformedURLException e){
            return new Bundle();
        }
    }
}
