package com.wistron.demo.tool.teddybear.dcs.http;

import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.http.callback.DcsCallback;
import com.wistron.demo.tool.teddybear.dcs.util.ObjectMapperUtil;

import org.litepal.util.LogUtil;

import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.RequestBody;

/**
 * 请求实现
 * Created by ivanjlzhang on 17-9-22.
 */

public class OkHttpRequestImpl implements HttpRequestInterface {
    private static final String TAG = "OkHttpRequestImpl";
    private final DcsHttpManager dcsHttpManager;

    public OkHttpRequestImpl() {
        dcsHttpManager = DcsHttpManager.getInstance();
    }

    @Override
    public void doPostEventStringAsync(DcsRequestBody requestBody, DcsCallback dcsCallback) {
        String bodyJson = ObjectMapperUtil.instance().objectToJson(requestBody);
        LogUtil.d(TAG, "doPostEventStringAsync-bodyJson:" + bodyJson);
        Map<String, RequestBody> multiParts = new LinkedHashMap<>();
        multiParts.put(HttpConfig.Parameters.DATA_METADATA,
                RequestBody.create(OkHttpMediaType.MEDIA_JSON_TYPE, bodyJson));
        DcsHttpManager.post()
                .url(HttpConfig.getEventsUrl())
                .headers(HttpConfig.getDCSHeaders())
                .multiParts(multiParts)
                .tag(HttpConfig.HTTP_EVENT_TAG)
                .build()
                .execute(dcsCallback);
    }

    @Override
    public void doPostEventMultipartAsync(DcsRequestBody requestBody,
                                          DcsStreamRequestBody streamRequestBody,
                                          DcsCallback dcsCallback) {
        String bodyJson = ObjectMapperUtil.instance().objectToJson(requestBody);
        LogUtil.d(TAG, "doPostEventMultipartAsync-bodyJson:" + bodyJson);
        Map<String, RequestBody> multiParts = new LinkedHashMap<>();
        multiParts.put(HttpConfig.Parameters.DATA_METADATA,
                RequestBody.create(OkHttpMediaType.MEDIA_JSON_TYPE, bodyJson));
        multiParts.put(HttpConfig.Parameters.DATA_AUDIO, streamRequestBody);
        DcsHttpManager.post()
                .url(HttpConfig.getEventsUrl())
                .headers(HttpConfig.getDCSHeaders())
                .multiParts(multiParts)
                .tag(HttpConfig.HTTP_EVENT_TAG)
                .build()
                .execute(dcsCallback);
    }

    @Override
    public void doGetDirectivesAsync(DcsRequestBody requestBody, DcsCallback dcsCallback) {
        LogUtil.d(TAG, "doGetDirectivesAsync");
        final long time = 60 * 60 * 1000L;
        DcsHttpManager.get()
                .url(HttpConfig.getDirectivesUrl())
                .headers(HttpConfig.getDCSHeaders())
                .tag(HttpConfig.HTTP_DIRECTIVES_TAG)
                .build()
                .connTimeOut(time)
                .readTimeOut(time)
                .writeTimeOut(time)
                .execute(dcsCallback);
    }

    @Override
    public void doGetPingAsync(DcsRequestBody requestBody, DcsCallback dcsCallback) {
        LogUtil.d(TAG, "doGetPingAsync");
        DcsHttpManager.get()
                .url(HttpConfig.getPingUrl())
                .headers(HttpConfig.getDCSHeaders())
                .tag(HttpConfig.HTTP_PING_TAG)
                .build()
                .execute(dcsCallback);
    }

    @Override
    public void cancelRequest(Object requestTag) {
        dcsHttpManager.cancelTag(requestTag);
    }
}
