package com.wistron.demo.tool.teddybear.dcs.oauth.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import static com.wistron.demo.tool.teddybear.dcs.oauth.api.OauthPreferenceUtil.BAIDU_OAUTH_CONFIG;

/**
 * manage token key info.
 * Created by ivanjlzhang on 17-9-19.
 */

public class AccessTokenManager implements Parcelable {
    // access token
    private String accessToken;
    // expire time
    private long expireTime = 0;
    private Context context;

    public AccessTokenManager(Context context){
        this.context = context;
        compareWithConfig();
    }
    public AccessTokenManager(Parcel in) {
        Bundle bundle = Bundle.CREATOR.createFromParcel(in);
        if(bundle != null){
            this.accessToken = bundle.getString(OauthConfig.BundleKey.KEY_ACCESS_TOKEN);
            this.expireTime = bundle.getLong(OauthConfig.BundleKey.KEY_EXPIRE_TIME);
        }
        compareWithConfig();
    }

    private void compareWithConfig(){
        if(this.context == null)
            return;

        final SharedPreferences sp = this.context.getSharedPreferences(BAIDU_OAUTH_CONFIG, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                String acToken = sp.getString(OauthConfig.PrefenenceKey.SP_ACCESS_TOKEN, null);
                if(accessToken != null && !accessToken.equals(acToken)){
                    initToken();
                }

            }
        });
    }

    protected void initToken(){
        this.accessToken = OauthPreferenceUtil.getAccessToken(context);
        long expires = OauthPreferenceUtil.getExpires(context);
        long createTime = OauthPreferenceUtil.getCreateTime(context);
        long current = System.currentTimeMillis();
        this.expireTime = createTime + expires;
        if(expireTime != 0 && expireTime < current){
            clearToken();
        }
    }

    protected void clearToken(){
        OauthPreferenceUtil.clearAllOauth(context);
        this.accessToken = null;
        this.expireTime = 0;
    }

    /**
     *
     * @param values
     */
    protected void storeToken(Bundle values){
       if(values == null || values.isEmpty())
           return;

        this.accessToken = values.getString("access_token");
        // expires_in 返回值为秒
        long expiresin = Long.parseLong(values.getString("expires_in")) * 1000;
        this.expireTime = System.currentTimeMillis() + expiresin;

        OauthPreferenceUtil.setAccessToken(context, this.accessToken);
        OauthPreferenceUtil.setCreateTime(context, System.currentTimeMillis());
        OauthPreferenceUtil.setExpires(context, expiresin);
    }

    /**
     * 判断当前token是否有效
     * @return
     */
    protected boolean isSessionValid(){
        if(this.accessToken == null || this.expireTime == 0){
            initToken();
        }
        return this.accessToken != null && this.expireTime != 0 && System.currentTimeMillis() < this.expireTime;
    }

    /**
     * 获取accesstoken info.
     * @return
     */
    public String getAccessToken(){
        if(this.accessToken == null)
            initToken();
        return this.accessToken;
    }

    public static final Creator<AccessTokenManager> CREATOR = new Creator<AccessTokenManager>() {
        @Override
        public AccessTokenManager createFromParcel(Parcel in) {
            return new AccessTokenManager(in);
        }

        @Override
        public AccessTokenManager[] newArray(int size) {
            return new AccessTokenManager[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
