package com.wistron.demo.tool.teddybear.parent_side.avs;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.amazon.identity.auth.device.AuthError;
import com.amazon.identity.auth.device.api.Listener;
import com.amazon.identity.auth.device.api.authorization.AuthCancellation;
import com.amazon.identity.auth.device.api.authorization.AuthorizationManager;
import com.amazon.identity.auth.device.api.authorization.AuthorizeListener;
import com.amazon.identity.auth.device.api.authorization.AuthorizeRequest;
import com.amazon.identity.auth.device.api.authorization.AuthorizeResult;
import com.amazon.identity.auth.device.api.authorization.ProfileScope;
import com.amazon.identity.auth.device.api.authorization.Scope;
import com.amazon.identity.auth.device.api.authorization.ScopeFactory;
import com.amazon.identity.auth.device.api.authorization.User;
import com.amazon.identity.auth.device.api.workflow.RequestContext;
import com.wistron.demo.tool.teddybear.parent_side.BaseLoginActivity;
import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.UDPBroadcastClient;

import org.json.JSONException;
import org.json.JSONObject;

import static com.wistron.demo.tool.teddybear.parent_side.avs.Common.CODE_CHALLENGE_METHOD;

public class AvsLoginActivityEx extends BaseLoginActivity {
    private boolean isStartLogin = true;
    private boolean isStartAuthorize = false;

    private RequestContext mRequestContext;


    private String PRODUCT_ID = "teddybear_avs";
    private String PRODUCT_DSN = "f330efaa0120a6e1";
    private String CODE_CHALLENGE = "kQ8I0HasW8on4sqZ0ofdUVFz9eLuweTD5RInJY2Q8Qc";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initial();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Scope[] scopes = {ProfileScope.profile(), ProfileScope.postalCode()};
        AuthorizationManager.getToken(this, scopes, new Listener<AuthorizeResult, AuthError>() {
            @Override
            public void onSuccess(AuthorizeResult result) {
                if (result.getAccessToken() != null) {

                    Log.i(Common.TAG, "accessToken: " + result.getAccessToken());

                    /* The user is signed in */
                    setLoginSuccessVisib("");
                    fetchUserProfile();
                } else {
                    /* The user is not signed in */
                    enableOrDisbleLogin(false);
                }
            }

            @Override
            public void onError(AuthError ae) {
                /* The user is not signed in */
                enableOrDisbleLogin(false);
            }
        });
    }

    private void initial() {
        mRequestContext = RequestContext.create(this);
        mRequestContext.registerListener(new AuthorizeListenerImpl());
    }

    @Override
    protected void initView() {
        super.initView();
        btn_login.setText(R.string.avs_login);
        btn_logout.setText(R.string.avs_logout);
    }

    @Override
    protected void analyzeData(String data) {
        String[] datas = data.split(";");
        PRODUCT_ID = datas[0].substring(datas[0].indexOf(":") + 1, datas[0].length());
        PRODUCT_DSN = datas[1].substring(datas[1].indexOf(":") + 1, datas[1].length());
        CODE_CHALLENGE = datas[2].substring(datas[2].indexOf(":") + 1, datas[2].length());
        Log.i(Common.TAG, "id:" + PRODUCT_ID + ", dsn:" + PRODUCT_DSN + ", codeChallenge:" + CODE_CHALLENGE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_connect.setEnabled(false);
                btn_login.setEnabled(true);
            }
        });
    }

    @Override
    protected void startLogout() {
        AuthorizationManager.signOut(AvsLoginActivityEx.this, new Listener<Void, AuthError>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(Common.TAG, "Logout success");
                dismissProgressDialog();
                setLogoutSuccessVisib();
            }

            @Override
            public void onError(AuthError authError) {
                Log.i(Common.TAG, "Logout authError");
            }
        });
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.demo_item_avs_login:
                startLogin();
                break;
            case R.id.demo_item_avs_logout:
                startNotificationProductLogout();
                break;
        }
    }

    @Override
    protected void startLogin() {
        isStartLogin = true;
        AuthorizationManager.authorize(
                new AuthorizeRequest.Builder(mRequestContext)
                        .addScopes(ProfileScope.profile(), ProfileScope.postalCode())
                        .build()
        );
    }

    private void startNotificationProductLogout() {
        showProgressDialog(getResources().getString(R.string.avs_connect_product_wait));
        if (udpBroadcastClient == null) {
            Log.i(Common.TAG, "UDP client is null and start new");
            udpBroadcastClient = new UDPBroadcastClient();
            udpBroadcastClient.setOnDataReceiveListener(new UDPBroadcastClient.onDataReceiveListener() {

                @Override
                public void onDataReceive(String data) {
                    Log.i(Common.TAG, "UDP receive data: " + data);
                    Message message = new Message();
                    message.what = Common.MYHANDLER_MSG_RECEIVE_HOST_LOGOUT;
                    message.obj = data;
                    myHandler.sendMessage(message);
                }
            });
        }
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isReceiveUdpHostSuccess) {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_RECEIVE_HOST_FAILE);
                }
            }
        }, 5000);
        isReceiveUdpHostSuccess = false;
        udpBroadcastClient.sendUDPBroadcast();
    }

    private class AuthorizeListenerImpl extends AuthorizeListener {

        @Override
        public void onSuccess(AuthorizeResult authorizeResult) {
            if (isStartLogin) {
                setLoginSuccessVisib("");

                isStartLogin = false;
                isStartAuthorize = true;
                final JSONObject scopeData = new JSONObject();
                final JSONObject productInstanceAttributes = new JSONObject();

                Log.i(Common.TAG, "id:" + PRODUCT_ID + ", dsn:" + PRODUCT_DSN + ", codeChallenge:" + CODE_CHALLENGE);

                try {
                    productInstanceAttributes.put("deviceSerialNumber", PRODUCT_DSN);
                    scopeData.put("productInstanceAttributes", productInstanceAttributes);
                    scopeData.put("productID", PRODUCT_ID);

                    AuthorizationManager.authorize(new AuthorizeRequest.Builder(mRequestContext)
                            .addScope(ScopeFactory.scopeNamed("alexa:all", scopeData))
                            .forGrantType(AuthorizeRequest.GrantType.AUTHORIZATION_CODE)
                            .withProofKeyParameters(CODE_CHALLENGE, CODE_CHALLENGE_METHOD)
                            .build());
                } catch (JSONException e) {
                    // handle exception here
                    Log.i(Common.TAG, "get author code faile: " + e.toString());
                }
            } else {
                String authorizationCode = authorizeResult.getAuthorizationCode();
                String redirectUri = authorizeResult.getRedirectURI();
                String clientId = authorizeResult.getClientId();
                Log.i(Common.TAG, "Authorization Code: " + authorizationCode);
                Log.i(Common.TAG, "redirect URI: " + redirectUri);
                Log.i(Common.TAG, "Client ID: " + clientId);

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("Code", authorizationCode);
                    jsonObject.put("id", clientId);
                    jsonObject.put("uri", redirectUri);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JSONUtil.setCommand(jsonObject, 1002);
                Log.i(Common.TAG, jsonObject.toString());
                if (socketClient != null) {
                    socketClient.send(jsonObject.toString());
                    socketClient.release();
                    socketClient = null;
                    Log.i(Common.TAG, "Send authorize code finish and close socket");
                }

                String accessToken = authorizeResult.getAccessToken();
                Log.i(Common.TAG, "accessToken: " + accessToken);

                fetchUserProfile();
            }
        }

        @Override
        public void onError(AuthError authError) {
            Log.i(Common.TAG, "onError: " + authError.toString());
        }

        @Override
        public void onCancel(AuthCancellation authCancellation) {
            Log.i(Common.TAG, "onCancel: " + authCancellation.toString());
        }
    }

    private void fetchUserProfile() {
        User.fetch(this, new Listener<User, AuthError>() {

            /* fetch completed successfully. */
            @Override
            public void onSuccess(User user) {
                final String name = user.getUserName();
                final String email = user.getUserEmail();
                final String account = user.getUserId();
                final String zipcode = user.getUserPostalCode();
                Log.i(Common.TAG, "Name: " + name + ", email: " + email + ", account: " + account);

                StringBuffer buffer = new StringBuffer();
                buffer.append(String.format("Welcome, %s!\n", name));
                buffer.append(String.format("Your email is %s\n", email));
//                buffer.append(String.format("Your Account is %s\n", account));
                setLoginSuccessVisib(buffer.toString());
            }

            /* There was an error during the attempt to get the profile. */
            @Override
            public void onError(AuthError ae) {
     /* Retry or inform the user of the error */
                Log.i(Common.TAG, "user profile error: " + ae.toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
    }
}
