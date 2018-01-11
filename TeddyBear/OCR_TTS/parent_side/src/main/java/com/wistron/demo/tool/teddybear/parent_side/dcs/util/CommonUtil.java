package com.wistron.demo.tool.teddybear.parent_side.dcs.util;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class CommonUtil {
    /**
     * 将key1=value1&key2=value2格式的query转换成key-value形式的参数串
     * @param query key1=value1&key2=value2格式的query
     * @return key-value形式的bundle
     */
    public static Bundle decodeUrl(String query){
        Bundle ret = new Bundle();
        if(query != null){
            String[] pairs = query.split("&");
            for (String pair: pairs) {
                String[] keyAndValues = pair.split("=");
                if(keyAndValues != null && keyAndValues.length == 2){
                    String key = keyAndValues[0];
                    String value = keyAndValues[1];
                    if(!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)){
                        ret.putString(URLDecoder.decode(key), URLDecoder.decode(value));
                    }
                }
            }
        }
        return ret;
    }

    public static String encodeUrl(Bundle params){
        if(params == null || params.isEmpty())
            return null;
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (String key:
             params.keySet()) {
            String paramValue = params.getString(key);
            if(paramValue == null)
                continue;
            if(first){
                first = false;
            }else
                sb.append("&");
            sb.append(URLEncoder.encode(key)).append("=").append(URLEncoder.encode(paramValue));

        }
        return sb.toString();
    }

    /**
     * 展示一个通用的弹出框UI
     *
     * @param context 展示弹出框的上下文环境
     * @param title   警告的title信息
     * @param text    警告信息
     */
    public static void showAlert(Context context, String title, String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }
}
