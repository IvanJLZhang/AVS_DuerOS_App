package com.wistron.demo.tool.teddybear.parent_side.avs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
import com.amazon.identity.auth.device.authorization.api.AuthzConstants;
import com.google.gson.Gson;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//import com.amazon.identity.auth.device.AuthError;
//import com.amazon.identity.auth.device.authorization.api.AmazonAuthorizationManager;
//import com.amazon.identity.auth.device.authorization.api.AuthorizationListener;
//import com.amazon.identity.auth.device.authorization.api.AuthzConstants;

/**
 * Time：16-11-23 10:35
 * Author：bob
 */
public class AccessToken {

    private final static String PRODUCT_ID = "android_avs";
    private static final String[] APP_SCOPES = {"alexa:all"};
    private static final String CODE_VERIFIER = "code_verifier";

    private final static String ARG_GRANT_TYPE = "grant_type";
    private final static String ARG_CODE = "code";
    private final static String ARG_REDIRECT_URI = "redirect_uri";
    private final static String ARG_CLIENT_ID = "client_id";
    private final static String ARG_CODE_VERIFIER = "code_verifier";


    private Context context;
    private AmazonAuthorizationManager mAuthManager;

    public AccessToken(Context context) {
        this.context = context;

        // initAuthorizationManager();
    }

    private void initAuthorizationManager() {
        try {
            mAuthManager = new AmazonAuthorizationManager(context, Bundle.EMPTY);
        } catch (IllegalArgumentException e) {
            //This error will be thrown if the main project doesn't have the assets/api_key.txt file in
            // it--this contains the security credentials from Amazon
//            Util.showAuthToast(mContext, "APIKey is incorrect or does not exist.");
            Log.e(Common.TAG, "Unable to Use Amazon Authorization Manager. APIKey is incorrect or does not exist" +
                    ". Does assets/api_key.txt exist in the main application?", e);
        }
    }

    public void authorizeUser() {

        String PRODUCT_DSN = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        Bundle options = new Bundle();
        String scope_data = "{\"alexa:all\":{\"productID\":\"" + PRODUCT_ID +
                "\", \"productInstanceAttributes\":{\"deviceSerialNumber\":\"" +
                PRODUCT_DSN + "\"}}}";
        options.putString(AuthzConstants.BUNDLE_KEY.SCOPE_DATA.val, scope_data);

        options.putBoolean(AuthzConstants.BUNDLE_KEY.GET_AUTH_CODE.val, true);
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE.val, getCodeChallenge());
        options.putString(AuthzConstants.BUNDLE_KEY.CODE_CHALLENGE_METHOD.val, "S256");

        mAuthManager.authorize(APP_SCOPES, options, authListener);
    }

    /**
     * Create a String hash based on the code verifier, this is used to verify the Token exchanges
     *
     * @return
     */
    public String getCodeChallenge() {
        String verifier = getCodeVerifier();
        return base64UrlEncode(getHash(verifier));
    }

    /**
     * Return our stored code verifier, which needs to be consistent, if this doesn't exist, we create a
     * new one and store the new result
     *
     * @return the String code verifier
     */
    private String getCodeVerifier() {

        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);

        if (preferences.contains(CODE_VERIFIER)) {
            return preferences.getString(CODE_VERIFIER, "");
        }

        //no verifier found, make and store the new one
        String verifier = createCodeVerifier();
//        Util.getPreferences(context).edit().putString(CODE_VERIFIER, verifier).apply();

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CODE_VERIFIER, verifier);
        editor.apply();

        return verifier;
    }

    public static String createCodeVerifier() {
        char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 128; i++) {
            char c = chars[random.nextInt(chars.length)];
            sb.append(c);
        }
        String verifier = sb.toString();
        return verifier;
    }

    /**
     * Hash a string based on the SHA-256 message digest
     *
     * @param password
     * @return
     */
    private static byte[] getHash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digest.reset();
        byte[] response = digest.digest(password.getBytes());

        return response;
    }

    /**
     * Encode a byte array into a string, while trimming off the last characters, as required by the Amazon
     * token server
     * <p>
     * See: http://brockallen.com/2014/10/17/base64url-encoding/
     *
     * @param arg our hashed string
     * @return a new Base64 encoded string based on the hashed string
     */
    private static String base64UrlEncode(byte[] arg) {
        String s = Base64.encodeToString(arg, 0); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }


    //An authorization callback to check when we get success/failure from the Amazon authentication server
    private AuthorizationListener authListener = new AuthorizationListener() {
        /**
         * Authorization was completed successfully.
         * Display the profile of the user who just completed authorization
         * @param response bundle containing authorization response. Not used.
         */
        @Override
        public void onSuccess(Bundle response) {
            String authCode = response.getString(AuthzConstants.BUNDLE_KEY.AUTHORIZATION_CODE.val);

            Log.i(Common.TAG, "Authorization successful");
//            getAccessToken(authCode, getCodeVerifier());
        }

        /**
         * There was an error during the attempt to authorize the application.
         * Log the error, and reset the profile text view.
         * @param ae the error that occurred during authorize
         */
        @Override
        public void onError(AuthError ae) {
            Log.e(Common.TAG, "AuthError during authorization", ae);
            getAccessTokenFail();
        }

        /**
         * Authorization was cancelled before it could be completed.
         * A toast is shown to the user, to confirm that the operation was cancelled, and the profile text
         * view is reset.
         * @param cause bundle containing the cause of the cancellation. Not used.
         */
        @Override
        public void onCancel(Bundle cause) {
            Log.e(Common.TAG, "User cancelled authorization");
            getAccessTokenFail();
        }
    };


    /**
     * Get an access token from the Amazon servers for the current user
     *
     * @param authCode     the authorization code supplied by the Authorization Manager
     * @param codeVerifier a randomly generated verifier, must be the same every time
     */
    public void getAccessToken(String authCode, String uri, String clientid, String codeVerifier) {
        //this url shouldn't be hardcoded, but it is, it's the Amazon auth access token endpoint
        String url = "https://api.amazon.com/auth/O2/token";

        //set up our arguments for the api call, these will be the call headers
        FormBody.Builder builder = new FormBody.Builder()
                .add(ARG_GRANT_TYPE, "authorization_code")
                .add(ARG_CODE, authCode);

        builder.add(ARG_REDIRECT_URI, uri);
        builder.add(ARG_CLIENT_ID, clientid);

        builder.add(ARG_CODE_VERIFIER, codeVerifier);

        OkHttpClient client = ClientUtil.getTLS12OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .post(builder.build())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();
                Log.i(Common.TAG, "get token faile: " + e.toString());
                getAccessTokenFail();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String s = response.body().string();
                Log.i(Common.TAG, "get token success: " + s);
                final TokenResponse tokenResponse = new Gson().fromJson(s, TokenResponse.class);
                //save our tokens to local shared preferences
                Log.i(Common.TAG, "access token: " + tokenResponse.access_token);
                Log.i(Common.TAG, "refresh token: " + tokenResponse.refresh_token);
                saveTokens(tokenResponse);

            }
        });
    }

    private void saveTokens(TokenResponse tokenResponse) {
        String REFRESH_TOKEN = tokenResponse.refresh_token;
        String ACCESS_TOKEN = tokenResponse.access_token;

//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Common.PREF_ACCESS_TOKEN, ACCESS_TOKEN);
        editor.putString(Common.PREF_REFRESH_TOKEN, REFRESH_TOKEN);
        editor.putLong(Common.PREF_TOKEN_EXPIRES, (System.currentTimeMillis() + tokenResponse.expires_in *
                1000));
        editor.apply();

        Log.i(Common.TAG, "commit");
        getAccessTokenSuccess();
    }

    //for JSON parsing of our token responses
    public static class TokenResponse {
        public String access_token;
        public String refresh_token;
        public String token_type;
        public long expires_in;
    }

    public interface AccessTokenListener {
        void success();

        void failure();
    }

    private AccessTokenListener accessTokenListener = null;

    public void setAccessTokenListener(AccessTokenListener accessTokenListener) {
        this.accessTokenListener = accessTokenListener;
    }

    private void getAccessTokenSuccess() {
        if (accessTokenListener != null) {
            accessTokenListener.success();
        }
    }

    private void getAccessTokenFail() {
        if (accessTokenListener != null) {
            accessTokenListener.failure();
        }
    }
}
