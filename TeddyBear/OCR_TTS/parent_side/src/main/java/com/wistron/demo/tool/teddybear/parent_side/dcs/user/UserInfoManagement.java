package com.wistron.demo.tool.teddybear.parent_side.dcs.user;

import com.wistron.demo.tool.teddybear.parent_side.ParentSideApplication;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.OauthPreferenceUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.util.LogUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ivanjlzhang on 17-9-28.
 */

public class UserInfoManagement implements IUser {

    private final String GET_USER_INFO_URL = "https://openapi.baidu.com/rest/2.0/passport/users/getLoggedInUser";
    private final String USER_LOGOUT_URL = "https://openapi.baidu.com/rest/2.0/passport/auth/revokeAuthorization";

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    private String access_token;

    public UserInfoManagement(IFetchUserInfoListener listener){
        this.listener = listener;
    }
    public UserInfoManagement(IUserLogoutListener listener){
        this.userLogoutListener = listener;
    }
    private IFetchUserInfoListener listener;
    private IUserLogoutListener userLogoutListener;
    private static final String TAG = "UserInfoManagement";
    @Override
    public void fetchUserInfo() {
        RequestBody body = new FormBody.Builder()
                .add("access_token", access_token)
                .build();

        Request request = new Request.Builder()
                .url(GET_USER_INFO_URL)
                .post(body)
                .build();
        OkHttpClient httpClient = new OkHttpClient();
        Call call = httpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                listener.OnFailed(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String uname = "";
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    uname = json.optString("uname");
                    String uid = json.optString("uid");
                    String portrait = json.optString("portrait");

                    UserPreferenceUtil.setuName(uname);
                    UserPreferenceUtil.setuId(uname);
                    LogUtil.d(TAG, "uname=" + uname + ";uid=" + uid);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                listener.OnSuccess(uname);
            }
        });
    }

    @Override
    public void fetchUserDetailInfo() {

    }

    @Override
    public void logout() {
        RequestBody body = new FormBody.Builder()
                .add("access_token", access_token)
                .build();

        Request request = new Request.Builder()
                .url(USER_LOGOUT_URL)
                .post(body)
                .build();
        OkHttpClient httpClient = new OkHttpClient();
        Call call = httpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                userLogoutListener.OnFailed(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    int result = json.optInt("result");
                    if(result == 1){
                        UserPreferenceUtil.ClearLoginInfo();
                        OauthPreferenceUtil.clear(ParentSideApplication.getContext());
                        userLogoutListener.OnSuccess();
                    }else{
                        userLogoutListener.OnFailed("logout fail.");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public interface IUserLogoutListener{
        void OnSuccess();

        void OnFailed(String errro);
    }
}
