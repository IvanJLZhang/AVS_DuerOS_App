package com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api;

import android.content.Context;

import com.wistron.demo.tool.teddybear.parent_side.dcs.util.PreferenceUtil;
/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class OauthPreferenceUtil extends PreferenceUtil {
    public static final String BAIDU_OAUTH_CONFIG = "baidu_oauth_config";

    /**
     * 保存数据的方法，拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     * @param context
     * @param key
     * @param value
     */
    public static void put(Context context, String key, Object value){
        put(context, BAIDU_OAUTH_CONFIG, key, value);
    }
    /**
     * 得到保存数据的方法，
     * 根据默认值得到保存的数据的具体类型，
     * 然后调用相对于的方法获取值
     * @param context
     * @param key
     * @param defaultObject
     * @return
     */
    public static Object get(Context context, String key, Object defaultObject){
        return get(context, BAIDU_OAUTH_CONFIG, key, defaultObject);
    }

    public static void clear(Context context) {
        clear(context, BAIDU_OAUTH_CONFIG);
    }

    public static void setAccessToken(Context context, String value) {
        put(context, OauthConfig.PrefenenceKey.SP_ACCESS_TOKEN, value);
    }

    public static String getAccessToken(Context context) {
        return (String) get(context, OauthConfig.PrefenenceKey.SP_ACCESS_TOKEN, "");
    }

    public static void setExpires(Context context, long value) {
        put(context, OauthConfig.PrefenenceKey.SP_EXPIRE_SECONDS, value);
    }

    public static long getExpires(Context context) {
        return (long) get(context, OauthConfig.PrefenenceKey.SP_EXPIRE_SECONDS, 0L);
    }

    public static void setCreateTime(Context context, long value) {
        put(context, OauthConfig.PrefenenceKey.SP_CREATE_TIME, value);
    }

    public static long getCreateTime(Context context) {
        return (long) get(context, OauthConfig.PrefenenceKey.SP_CREATE_TIME, 0L);
    }

    public static void clearAllOauth(Context context) {
        clear(context);
    }
}

