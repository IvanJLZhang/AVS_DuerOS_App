package com.wistron.demo.tool.teddybear.dcs.oauth.api;

import android.text.TextUtils;

import com.wistron.demo.tool.teddybear.TeddyBearApplication;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class OauthImpl implements IOauth {
    @Override
    public String getAccessToken() {
        return OauthPreferenceUtil.getAccessToken(TeddyBearApplication.getContext());
    }

    @Override
    public void authorize() {
//        Intent intent = new Intent(DcsApplication.getInstance(), DcsSampleOAuthActivity.class);
//        intent.putExtra("START_TAG", "RESTART");
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        DcsApplication.getInstance().startActivity(intent);
    }

    @Override
    public boolean isSessionValid() {
        String accessToken = getAccessToken();
        long createTime = OauthPreferenceUtil.getCreateTime(TeddyBearApplication.getContext());
        long expires = OauthPreferenceUtil.getExpires(TeddyBearApplication.getContext()) + createTime;
        long current = System.currentTimeMillis();
        return !TextUtils.isEmpty(accessToken) && expires != 0 && System.currentTimeMillis() < expires;
    }
}
