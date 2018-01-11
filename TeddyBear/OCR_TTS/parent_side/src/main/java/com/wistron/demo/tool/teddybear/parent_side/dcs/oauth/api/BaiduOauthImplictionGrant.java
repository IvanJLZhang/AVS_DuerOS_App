package com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieSyncManager;

import com.wistron.demo.tool.teddybear.parent_side.dcs.util.CommonUtil;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class BaiduOauthImplictionGrant implements Parcelable {

    private static final String TAG = "BaiduOauth";
    public static final String CANCEL_URI = "bdconnect://cancel";
    // 百度Oauth授权回调需要在DUEROS开放平台的控制平台
    // 应用编辑-->>OAUTH CONFIG URL的链接地址-->>授权回调页-->>安全设置-->>授权回调页
    // 需要注意
    public static final String SUCCESS_URI = "bdconnect://success";
    private static final String OAUTHORIZE_URL = "https://openapi.baidu.com/oauth/2.0/authorize";
    private static final String DISPLAY_STRING = "mobile";
    private static final String[] DEFAULT_PERMISSIONS = {"basic"};
    private static final String KEY_CLIENT_ID = "clientId";
    // 应用注册的api key信息
    private String cliendId;



    private AccessTokenManager accessTokenManager;

    public BaiduOauthImplictionGrant(String clientId, Context context){
        if(clientId == null){
            throw new IllegalArgumentException("client id can not be null");
        }
        this.cliendId = clientId;
        init(context);
    }

    /**
     * 从parcel流创建Baidu对象
     * @param in
     */
    public BaiduOauthImplictionGrant(Parcel in){
        Bundle bundle = Bundle.CREATOR.createFromParcel(in);
        this.cliendId = bundle.getString(KEY_CLIENT_ID);
        this.accessTokenManager = AccessTokenManager.CREATOR.createFromParcel(in);
    }


    public void init(Context context){
        if(context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            Log.w(TAG, "App miss permission android.permission.ACCESS_NETWORK_STATE! "
                    + "Some mobile's WebView don't display page!");
        }
        this.accessTokenManager = new AccessTokenManager(context);
        this.accessTokenManager.initToken();
    }


    public void authorize(Activity activity,
                          boolean isForceLogin,
                          boolean isConfirmLogin,
                          final BaiduDialog.BaiduDialogListener listener) {
        this.authorize(activity, null, isForceLogin, isConfirmLogin, listener);
    }

    // 使用匿名的BaiduDialogListener对listener进行了包装，并进行一些存储token信息和当前登录用户的逻辑，
    // 外部传进来的listener信息不需要在进行存储相关的逻辑
    private void authorize(Activity activity,
                           String[] permissions,
                           boolean isForceLogin,
                           boolean isConfigLogin,
                           final BaiduDialog.BaiduDialogListener listener){
        if(this.isSessitionValid()){
            listener.onComplete(new Bundle());
            return;
        }
        // 使用匿名的BaiduDialogListener对listener进行了包装，并进行一些存储token信息和当前登录用户的逻辑，
        // 外部传进来的listener信息不需要在进行存储相关的逻辑
        this.authorize(activity,
                permissions,
                isForceLogin,
                isConfigLogin,
                new BaiduDialog.BaiduDialogListener() {
                    @Override
                    public void onComplete(Bundle values) {
                        getAccessTokenManager().storeToken(values);

                        listener.onComplete(values);
                    }

                    @Override
                    public void onBaiduException(BaiduException e) {
                        Log.d(TAG, "BaiduException : " + e);
                        listener.onBaiduException(e);
                    }

                    @Override
                    public void onError(BaiduDialogError e) {
                        Log.d(TAG, "DialogError " + e);
                        listener.onError(e);
                    }

                    @Override
                    public void onCancel() {
                        Log.d(TAG, "login cancel");
                        listener.onCancel();
                    }
                }, SUCCESS_URI, "token");
    }

    /**
     * 通过Dialog UI展示用户登录、授权页
     * @param activity
     * @param permissions 需要请求的环境
     * @param isForceLogin
     * @param isConfirmLogin
     * @param listener 用于回调的listener接口
     * @param redirectUrl 回调地址
     * @param responseType 授权请求的类型
     */
    private void authorize(Activity activity,
                           String[] permissions,
                           boolean isForceLogin,
                           boolean isConfirmLogin,
                           final BaiduDialog.BaiduDialogListener listener,
                           String redirectUrl, String responseType){
        CookieSyncManager.createInstance(activity);
        Bundle params = new Bundle();
        params.putString("client_id", this.cliendId);
        params.putString("redirect_uri", redirectUrl);
        params.putString("response_type", responseType);
        params.putString("display", DISPLAY_STRING);
        if(isForceLogin){
            params.putString("force_login", "1");
        }
        if(isConfirmLogin){
            params.putString("confirm_login", "1");
        }
        if(permissions == null){
            permissions = DEFAULT_PERMISSIONS;
        }
        if(permissions != null && permissions.length > 0){
            String scope = TextUtils.join(" ", permissions);
            params.putString("scope", scope);
        }

        String url = OAUTHORIZE_URL + "?" + CommonUtil.encodeUrl(params);
        Log.d(TAG, "url: " + url);
        if(activity.checkCallingOrSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            CommonUtil.showAlert(activity, "no permission", "app need permission internet.");
        }else{
            new BaiduDialog(activity, url, listener).show();
        }
    }

    public String getAccessToken(){return  accessTokenManager.getAccessToken();}

    public void clearAccessToken(){
        if(this.accessTokenManager != null){
            this.accessTokenManager.clearToken();
            this.accessTokenManager = null;
        }
    }

    public boolean isSessitionValid(){ return this.accessTokenManager.isSessionValid();}
    public AccessTokenManager getAccessTokenManager() {
        return accessTokenManager;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Creator<BaiduOauthImplictionGrant> CREATOR = new Creator<BaiduOauthImplictionGrant>() {
        @Override
        public BaiduOauthImplictionGrant createFromParcel(Parcel parcel) {
            return new BaiduOauthImplictionGrant(parcel);
        }

        @Override
        public BaiduOauthImplictionGrant[] newArray(int i) {
            return new BaiduOauthImplictionGrant[i];
        }
    };
}

