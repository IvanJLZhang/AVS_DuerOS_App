package com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api;

import android.content.Context;
import android.text.TextUtils;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class OauthImpl implements IOauth {
    private final Context context;
    public OauthImpl(final Context context){
        this.context = context;
    }
    @Override
    public String getAccessToken() {
        return OauthPreferenceUtil.getAccessToken(context);
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
        long createTime = OauthPreferenceUtil.getCreateTime(context);
        long expires = OauthPreferenceUtil.getExpires(context) + createTime;
        long current = System.currentTimeMillis();
        return !TextUtils.isEmpty(accessToken) && expires != 0 && System.currentTimeMillis() < expires;
    }
}
