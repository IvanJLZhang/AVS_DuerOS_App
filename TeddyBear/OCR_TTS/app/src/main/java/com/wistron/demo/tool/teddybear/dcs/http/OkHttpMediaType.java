package com.wistron.demo.tool.teddybear.dcs.http;

import okhttp3.MediaType;

import static com.wistron.demo.tool.teddybear.dcs.http.HttpConfig.ContentTypes.APPLICATION_AUDIO;
import static com.wistron.demo.tool.teddybear.dcs.http.HttpConfig.ContentTypes.APPLICATION_JSON;

/**
 * okhttp media_type
 * Created by ivanjlzhang on 17-9-21.
 */

public class OkHttpMediaType {
    // json类型
    public static final MediaType MEDIA_JSON_TYPE = MediaType.parse(APPLICATION_JSON);
    // 数据流类型
    public static final MediaType MEDIA_STREAM_TYPE = MediaType.parse(APPLICATION_AUDIO);
}
