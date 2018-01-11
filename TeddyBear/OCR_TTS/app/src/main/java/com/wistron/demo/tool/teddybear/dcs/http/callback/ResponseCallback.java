package com.wistron.demo.tool.teddybear.dcs.http.callback;

import org.litepal.util.LogUtil;

import okhttp3.Call;
import okhttp3.Response;

/**
 * framework 返回数据类
 * Created by ivanjlzhang on 17-9-22.
 */

public class ResponseCallback extends DcsCallback<Response> {
    private static final String TAG = "ResponseCallback";

    @Override
    public Response parseNetworkResponse(Response response, int id) throws Exception {
        return response;
    }

    @Override
    public void onError(Call call, Exception e, int id) {
        LogUtil.d(TAG, "onError:" + e.getMessage());
    }

    @Override
    public void onResponse(Response response, int id) {
        LogUtil.d(TAG, "onResponse:" + response.code());
    }
}
