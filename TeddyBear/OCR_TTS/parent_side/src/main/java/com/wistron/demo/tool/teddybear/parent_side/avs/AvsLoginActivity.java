package com.wistron.demo.tool.teddybear.parent_side.avs;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.SocketClient;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.UDPBroadcastClient;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;

import org.json.JSONException;
import org.json.JSONObject;

import static com.wistron.demo.tool.teddybear.parent_side.avs.Common.CODE_CHALLENGE_METHOD;


public class AvsLoginActivity extends AppCompatActivity implements View.OnClickListener {
    private FlatButton btnConnectProduct, btnLogin, btnLogout;
    private TextView tvShowLoginInfo;

    private RequestContext mRequestContext;
    private String serverHost = "";
    private ProgressDialog mProgressDialog;
    private SocketClient socketClient = null;

    private String PRODUCT_ID = "teddybear_avs";
    private String PRODUCT_DSN = "f330efaa0120a6e1";
    private String CODE_CHALLENGE = "kQ8I0HasW8on4sqZ0ofdUVFz9eLuweTD5RInJY2Q8Qc";

    private boolean isStartLogin = true;
    private boolean isStartAuthorize = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initial();
        setContentView(R.layout.activity_avs_login);
        initView();
    }

    private void initial() {
        mRequestContext = RequestContext.create(this);
        mRequestContext.registerListener(new AuthorizeListenerImpl());
    }

    private void initView() {
        btnConnectProduct = (FlatButton) findViewById(R.id.demo_item_avs_connect);
        btnLogin = (FlatButton) findViewById(R.id.demo_item_avs_login);

        btnLogout = (FlatButton) findViewById(R.id.demo_item_avs_logout);

        btnConnectProduct.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);

        tvShowLoginInfo = (TextView) findViewById(R.id.login_information);

    }

    private UDPBroadcastClient udpBroadcastClient = null;
    private boolean isReceiveUdpHostSuccess = true;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.demo_item_avs_connect:
                showProgressDialog(getResources().getString(R.string.avs_connect_product_wait));
                if (udpBroadcastClient == null) {
                    Log.i(Common.TAG, "UDP client is null and start new");
                    udpBroadcastClient = new UDPBroadcastClient();
                    udpBroadcastClient.setOnDataReceiveListener(new UDPBroadcastClient.onDataReceiveListener() {

                        @Override
                        public void onDataReceive(String data) {
                            Log.i(Common.TAG, "UDP receive data: " + data);
                            Message message = new Message();
                            message.what = Common.MYHANDLER_MSG_RECEIVE_HOST;
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
                break;
            case R.id.demo_item_avs_login:
                startLogin();
                break;
            case R.id.demo_item_avs_logout:
                startNotificationProductLogout();
                break;
        }
    }

    private void startLogin() {
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

    private void startLogout() {
        AuthorizationManager.signOut(AvsLoginActivity.this, new Listener<Void, AuthError>() {
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

    private void enableOrDisbleLogin(final boolean isEnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnLogin.setEnabled(isEnable);
            }
        });
    }

    private void setLoginSuccessVisib(final String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnectProduct.setVisibility(View.GONE);
                btnLogin.setVisibility(View.GONE);
                btnLogout.setVisibility(View.VISIBLE);
                tvShowLoginInfo.setVisibility(View.VISIBLE);
                tvShowLoginInfo.setText(content);
            }
        });
    }

    private void setLogoutSuccessVisib() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvShowLoginInfo.setVisibility(View.GONE);
                btnLogout.setVisibility(View.GONE);

                btnConnectProduct.setVisibility(View.VISIBLE);
                btnConnectProduct.setEnabled(true);
                btnLogin.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRequestContext.onResume();
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

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case Common.MYHANDLER_MSG_RECEIVE_HOST:
                    isReceiveUdpHostSuccess = true;
                    udpBroadcastClient.release();
                    udpBroadcastClient = null;
                    serverHost = (String) msg.obj;
                    Log.i(Common.TAG, "Handler receive data: " + serverHost);
                    connectPorduct();
                    break;
                case Common.MYHANDLER_MSG_CONNECT_PRODUCT_SUCCESS:
                    Log.i(Common.TAG, "Connect Product success");
                    if (socketClient != null) {
                        JSONObject jsonObject = new JSONObject();
                        JSONUtil.setCommand(jsonObject, 1001);
                        socketClient.send(jsonObject.toString());
                    }
                    break;
                case Common.MYHANDLER_MSG_CONNECT_PRODUCT_FAILE:
                    Log.i(Common.TAG, "Connect Product fail");
                    dismissProgressDialog();
                    Toast.makeText(AvsLoginActivity.this, "Connect Product fail,please try again", Toast.LENGTH_SHORT).show();
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_METADATA_FROM_PRODUCT:
                    dismissProgressDialog();
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_HOST_FAILE:
                    udpBroadcastClient.release();
                    udpBroadcastClient = null;
                    dismissProgressDialog();
                    Toast.makeText(AvsLoginActivity.this, "Connect Product fail,please try again", Toast.LENGTH_SHORT).show();
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_HOST_LOGOUT:
                    isReceiveUdpHostSuccess = true;
                    udpBroadcastClient.release();
                    udpBroadcastClient = null;
                    String host = (String) msg.obj;
                    Log.i(Common.TAG, "Handler receive data: " + serverHost);
                    connectPorduct(host);
                    break;
                case Common.MYHANDLER_MSG_CONNECT_PRODUCT_LOGOUT_SUCCESS:
                    Log.i(Common.TAG, "Connect Product success for logout");
                    if (socketClient != null) {
                        JSONObject jsonObject = new JSONObject();
                        JSONUtil.setCommand(jsonObject, 1005);
                        socketClient.send(jsonObject.toString());
                    }
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_CLEAR_DATA:
                    if (socketClient != null) {
                        socketClient.release();
                        socketClient = null;
                    }
                    startLogout();
                    break;
            }
            return false;
        }
    });

    private void connectPorduct() {
        if (socketClient == null) {
            socketClient = new SocketClient();
            socketClient.setOnConnectListener(new SocketClient.onConnectListener() {
                @Override
                public void onConnectSuccess() {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_CONNECT_PRODUCT_SUCCESS);
                }

                @Override
                public void onConnectFail() {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_CONNECT_PRODUCT_FAILE);
                }

                @Override
                public void onDataReceived(String data) {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_RECEIVE_METADATA_FROM_PRODUCT);
                    Log.i(Common.TAG, "Receive MetaData from Product: " + data);
                    data = data.substring(0, data.indexOf("}"));
                    Log.i(Common.TAG, "Receive MetaData from Product: " + data);
                    analyzeData(data);
                }
            });
        }

        socketClient.connect(serverHost);
    }

    private void connectPorduct(String host) {
        if (socketClient == null) {
            socketClient = new SocketClient();
            socketClient.setOnConnectListener(new SocketClient.onConnectListener() {
                @Override
                public void onConnectSuccess() {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_CONNECT_PRODUCT_LOGOUT_SUCCESS);
                }

                @Override
                public void onConnectFail() {
                    myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_CONNECT_PRODUCT_FAILE);
                }

                @Override
                public void onDataReceived(String data) {

                    Log.i(Common.TAG, "Receive clear data: " + data);
                    data = data.substring(0, data.indexOf("}"));
                    Log.i(Common.TAG, "Receive clear from Product: " + data);
                    if (data.equals("1005")) {
                        myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_RECEIVE_CLEAR_DATA);
                    }
                }
            });
        }

        socketClient.connect(host);
    }

    private void analyzeData(String data) {
        String[] datas = data.split(";");
        PRODUCT_ID = datas[0].substring(datas[0].indexOf(":") + 1, datas[0].length());
        PRODUCT_DSN = datas[1].substring(datas[1].indexOf(":") + 1, datas[1].length());
        CODE_CHALLENGE = datas[2].substring(datas[2].indexOf(":") + 1, datas[2].length());
        Log.i(Common.TAG, "id:" + PRODUCT_ID + ", dsn:" + PRODUCT_DSN + ", codeChallenge:" + CODE_CHALLENGE);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConnectProduct.setEnabled(false);
                btnLogin.setEnabled(true);
            }
        });
    }

    private void showProgressDialog(String msg) {
        mProgressDialog = ProgressDialog.show(this, null, msg);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketClient != null) {
            socketClient.release();
        }
    }
}
