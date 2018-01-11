package com.wistron.demo.tool.teddybear.parent_side.dcs;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.BaseLoginActivity;
import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.avs.Common;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.BaiduDialog;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.BaiduDialogError;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.BaiduException;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.BaiduOauthImplictionGrant;
import com.wistron.demo.tool.teddybear.parent_side.dcs.oauth.api.OauthPreferenceUtil;
import com.wistron.demo.tool.teddybear.parent_side.dcs.user.IFetchUserInfoListener;
import com.wistron.demo.tool.teddybear.parent_side.dcs.user.UserInfoManagement;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.UDPBroadcastClient;

import org.json.JSONObject;

public class DcsLoginActivity extends BaseLoginActivity {
    private static final String CLIENT_ID = "GuU0eVnI9HycYrWViYruEj380ayOU2s2";

    private boolean isForceLogin = true;
    private boolean isConfimLogin = true;
    private BaiduOauthImplictionGrant baiduOauthImplictionGrant;

    private static final String TAG = "DcsLoginActivity";
    @Override
    protected void initView() {
        super.initView();
        btn_login.setText(R.string.bd_login);
        btn_logout.setText(R.string.bd_logout);
    }
    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()){
            case R.id.btn_login:
                startLogin();
                break;
            case R.id.btn_logout:
                startNotificationProductLogout();
                break;
        }
    }

    @Override
    protected void analyzeData(String data) {
        Log.i(TAG, data);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btn_connect.setEnabled(false);
                btn_login.setEnabled(true);
            }
        });
    }

    @Override
    protected void startLogin() {
        baiduOauthImplictionGrant = new BaiduOauthImplictionGrant(CLIENT_ID, this.getApplicationContext());
        baiduOauthImplictionGrant.authorize(this,
                isForceLogin, isConfimLogin, new BaiduDialog.BaiduDialogListener() {
                    @Override
                    public void onComplete(Bundle values) {
                        Log.i(TAG, OauthPreferenceUtil.getAccessToken(DcsLoginActivity.this));
                        Toast.makeText(DcsLoginActivity.this.getApplicationContext(), "login success", Toast.LENGTH_SHORT).show();
                        String acToken = OauthPreferenceUtil.getAccessToken(DcsLoginActivity.this);
                        Long expires = OauthPreferenceUtil.getExpires(DcsLoginActivity.this);
                        long createTime = OauthPreferenceUtil.getCreateTime(DcsLoginActivity.this);
                        JSONObject json = new JSONObject();
                        try{
                            json.put("access_token", acToken);
                            json.put("expires_in", expires);
                            json.put("create_time", createTime);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                       JSONUtil.setCommand(json, 2002);
                        if(socketClient != null){
                            socketClient.send(json.toString());
                            socketClient.release();
                            socketClient = null;
                            Log.i("Ivan_DCS", "Send authorize code finish and close socket");
                            Log.i("Ivan_DCS", json.toString());
                        }
                        UserInfoManagement user = new UserInfoManagement(new IFetchUserInfoListener() {
                            @Override
                            public void OnSuccess(String uname) {
                                setLoginSuccessVisib(uname);
                            }

                            @Override
                            public void OnFailed(String errro) {

                            }
                        });
                        user.setAccess_token(acToken);
                        user.fetchUserInfo();
                    }

                    @Override
                    public void onBaiduException(BaiduException e) {

                    }

                    @Override
                    public void onError(BaiduDialogError e) {

                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    @Override
    protected void startLogout() {
        UserInfoManagement logout = new UserInfoManagement(new UserInfoManagement.IUserLogoutListener() {
            @Override
            public void OnSuccess() {
                Log.i(TAG, "logout success!");
                dismissProgressDialog();
                setLogoutSuccessVisib();
            }
            @Override
            public void OnFailed(String errro) {

            }
        });
        logout.setAccess_token(OauthPreferenceUtil.getAccessToken(this));
        logout.logout();
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
}
