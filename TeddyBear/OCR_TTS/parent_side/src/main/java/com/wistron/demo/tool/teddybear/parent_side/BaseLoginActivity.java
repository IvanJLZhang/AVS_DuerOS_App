package com.wistron.demo.tool.teddybear.parent_side;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.avs.Common;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.SocketClient;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.UDPBroadcastClient;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;

import org.json.JSONObject;

public abstract class BaseLoginActivity extends AppCompatActivity implements View.OnClickListener{
    protected FlatButton btn_connect, btn_login, btn_logout;
    protected TextView tvShowLoginInfo;

    protected String LoginType;// AVS or DCS
    private ProgressDialog mProgressDialog;
    protected UDPBroadcastClient udpBroadcastClient = null;
    protected boolean isReceiveUdpHostSuccess = true;

    private String serverHost = "";

    protected SocketClient socketClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_login);
        initView();
    }

    protected void initView(){
        btn_connect = (FlatButton) findViewById(R.id.btn_connect);
        btn_login = (FlatButton) findViewById(R.id.btn_login);
        btn_logout = (FlatButton) findViewById(R.id.btn_logout);
        btn_connect.setOnClickListener(this);
        btn_login.setOnClickListener(this);
        btn_logout.setOnClickListener(this);

        btn_connect.setText("Connect Product");
        btn_login.setText("Login");
        btn_logout.setText("Logout");

        tvShowLoginInfo = (TextView) findViewById(R.id.login_information);

        btn_login.setEnabled(false);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_connect:
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
        }
    }
    protected Handler myHandler = new Handler(new Handler.Callback() {
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
                        JSONUtil.setCommand(jsonObject, 2001);
                        socketClient.send(jsonObject.toString());
                    }
                    break;
                case Common.MYHANDLER_MSG_CONNECT_PRODUCT_FAILE:
                    Log.i(Common.TAG, "Connect Product fail");
                    dismissProgressDialog();
                    Toast.makeText(BaseLoginActivity.this, "Connect Product fail,please try again", Toast.LENGTH_SHORT).show();
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_METADATA_FROM_PRODUCT:
                    dismissProgressDialog();
                    break;
                case Common.MYHANDLER_MSG_RECEIVE_HOST_FAILE:
                    udpBroadcastClient.release();
                    udpBroadcastClient = null;
                    dismissProgressDialog();
                    Toast.makeText(BaseLoginActivity.this, "Connect Product fail,please try again", Toast.LENGTH_SHORT).show();
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
                        JSONUtil.setCommand(jsonObject, 2005);
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
                    if (data.equals("2005")) {
                        myHandler.sendEmptyMessage(Common.MYHANDLER_MSG_RECEIVE_CLEAR_DATA);
                    }
                }
            });
        }
        socketClient.connect(host);
    }
    protected abstract void analyzeData(String data);
    protected abstract void startLogin();
    protected abstract void startLogout();
    protected void enableOrDisbleLogin(final boolean isEnable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_login.setEnabled(isEnable);
            }
        });
    }
    protected void setLoginSuccessVisib(final String content) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_connect.setVisibility(View.GONE);
                btn_login.setVisibility(View.GONE);
                btn_logout.setVisibility(View.VISIBLE);
                tvShowLoginInfo.setVisibility(View.VISIBLE);
                tvShowLoginInfo.setText(content);
            }
        });
    }
    protected void setLogoutSuccessVisib() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvShowLoginInfo.setText("");
                tvShowLoginInfo.setVisibility(View.GONE);
                btn_logout.setVisibility(View.GONE);

                btn_connect.setVisibility(View.VISIBLE);
                btn_connect.setEnabled(true);
                btn_login.setVisibility(View.VISIBLE);
                btn_login.setEnabled(false);
            }
        });
    }

    protected void showProgressDialog(String msg) {
        mProgressDialog = ProgressDialog.show(this, null, msg);
    }
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketClient != null) {
            socketClient.release();
        }
    }
}
