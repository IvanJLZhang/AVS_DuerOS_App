package com.wistron.demo.tool.teddybear.dcs.systemimpl.player;

import android.content.Context;

import com.wistron.demo.tool.teddybear.dcs.util.PreferenceUtil;

/**
 * Media Player 保存音量和静音的状态
 * Created by ivanjlzhang on 17-9-23.
 */

public class MediaPlayerPreferenceUtil extends PreferenceUtil {
    // 保存到的文件名字
    private static final String BAIDU_MEDIA_CONFIG = "baidu_media_config";

    /**
     * 保存音量或者静音的数据状态
     *
     * @param context 上下文
     * @param key     键
     * @param object  值
     */
    public static void put(Context context, String key, Object object) {
        put(context, BAIDU_MEDIA_CONFIG, key, object);
    }

    /**
     * 读取音量或者静音的数据状态
     *
     * @param context       上下文
     * @param key           键
     * @param defaultObject 默认值
     */
    public static Object get(Context context, String key, Object defaultObject) {
        return get(context, BAIDU_MEDIA_CONFIG, key, defaultObject);
    }
}
