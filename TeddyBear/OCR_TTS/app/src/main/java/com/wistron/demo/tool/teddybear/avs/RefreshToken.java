package com.wistron.demo.tool.teddybear.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.google.gson.Gson;
import com.willblaschko.android.alexa.TokenManager;
import com.willblaschko.android.alexa.connection.ClientUtil;
import com.willblaschko.android.alexa.utility.Util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.wistron.demo.tool.teddybear.avs.Common.AUTHORIZE_INFO;
import static com.wistron.demo.tool.teddybear.avs.Common.CLIENT_ID;

/**
 * Time：16-11-21 13:25
 * Author：bob
 */
public class RefreshToken {
    private final static String ARG_GRANT_TYPE = "grant_type";
    private final static String ARG_REFRESH_TOKEN = "refresh_token";
    private final static String ARG_CLIENT_ID = "client_id";

    private Context context;

    public RefreshToken(Context context) {
        this.context = context;
        Log.i(Common.TAG, "init RefreshToken");
    }

    /**
     * Check if we have a pre-existing access token, and whether that token is expired. If it is not,
     * return that token, otherwise get a refresh token and then
     * use that to get a new token.
     *
     * @param authorizationManager our AuthManager
     * @param context              local/application context
     * @param callback             the TokenCallback where we return our tokens when successful
     */
    public void getAccessToken(@NotNull AmazonAuthorizationManager authorizationManager, @NotNull Context
            context, @NotNull TokenManager.TokenCallback callback) {
        SharedPreferences preferences = Util.getPreferences(context.getApplicationContext());
        //if we have an access token
        if (preferences.contains(Common.PREF_ACCESS_TOKEN)) {

            if (preferences.getLong(Common.PREF_TOKEN_EXPIRES, 0) > System.currentTimeMillis()) {
                //if it's not expired, return the existing token
                callback.onSuccess(preferences.getString(Common.PREF_ACCESS_TOKEN, null));
                return;
            } else {
                //if it is expired but we have a refresh token, get a new token
                if (preferences.contains(Common.PREF_REFRESH_TOKEN)) {
//                    getRefreshToken(authorizationManager, context, callback, preferences.getString
// (PREF_REFRESH_TOKEN, ""));
                    return;
                }
            }
        }

        //uh oh, the user isn't logged in, we have an IllegalStateException going on!
        callback.onFailure(new IllegalStateException("User is not logged in and no refresh token found."));
    }

    /**
     * Get a new refresh token from the Amazon server to replace the expired access token that we currently
     * have
     *
     * @param refreshToken the refresh token we have stored in local cache (sharedPreferences)
     */
    public void getRefreshToken(String refreshToken) {
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        String url = "https://api.amazon.com/auth/O2/token";

        //set up our arguments for the api call, these will be the call headers
        FormBody.Builder builder = new FormBody.Builder()
                .add(ARG_GRANT_TYPE, "refresh_token")
                .add(ARG_REFRESH_TOKEN, refreshToken);

        SharedPreferences preferences = context.getSharedPreferences(AUTHORIZE_INFO, Context.MODE_PRIVATE);
        String clientId_value = preferences.getString(CLIENT_ID, "");

        builder.add(ARG_CLIENT_ID, clientId_value);

        Log.i(Common.TAG, "client id Value: " + clientId_value);

        OkHttpClient client = ClientUtil.getTLS12OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
                Log.i(Common.TAG, "refresh Token fail: " + e.toString());
                if (refreshTokenListener != null) {
                    refreshTokenListener.refreshFail();
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
//                Log.i(Common.TAG, "refresh Token success: " + s);

                //get our tokens back
                final TokenManager.TokenResponse tokenResponse = new Gson().fromJson(s, TokenManager
                        .TokenResponse.class);
//                Log.i(Common.TAG, "new access token response: " + tokenResponse.access_token);
//                Log.i(Common.TAG, "new refresh token response: " + tokenResponse.refresh_token);

                //save our tokens
                saveTokens(tokenResponse);

                if (refreshTokenListener != null) {
                    refreshTokenListener.refreshSuccess();
                }
            }
        });

    }

    private void saveTokens(TokenManager.TokenResponse tokenResponse) {
        String REFRESH_TOKEN = tokenResponse.refresh_token;
        String ACCESS_TOKEN = tokenResponse.access_token;

        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Common.PREF_ACCESS_TOKEN, ACCESS_TOKEN);
        editor.putString(Common.PREF_REFRESH_TOKEN, REFRESH_TOKEN);
        editor.putLong(Common.PREF_TOKEN_EXPIRES, (System.currentTimeMillis() + tokenResponse.expires_in *
                1000));
        editor.apply();

        Log.i(Common.TAG, "commit");

    }

    public interface RefreshTokenListener {
        void refreshFail();

        void refreshSuccess();
    }

    private RefreshTokenListener refreshTokenListener = null;

    public void setRefreshTokenListener(RefreshTokenListener listener) {
        refreshTokenListener = listener;
    }
}
