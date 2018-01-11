package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedWifiDevice;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.socket.SocketController;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.CommandUtil;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.wifi.WifiController;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aaron on 16-9-28.
 */
public class SetupWifiActivity extends AppCompatActivity implements ScanWifiFragment.ScanWifiFragmentUI, ScanP2pFragment.ScanP2pFragmentUI, WifiController.notify, SocketController.Notify, ConnectDialog.ConnectDialogUI {

    public static final int DISABLED_UNKNOWN_REASON = 0;
    public static final int DISABLED_DNS_FAILURE = 1;
    public static final int DISABLED_DHCP_FAILURE = 2;
    public static final int DISABLED_AUTH_FAILURE = 3;
    public static final int DISABLED_ASSOCIATION_REJECT = 4;
    public static final int DISABLED_BY_WIFI_MANAGER = 5;
    public static final int DISABLED_TIMEOUT = 6;
    public static final int ENABLED = 7;
    public static final int MSG_P2P_CONNECTED = 8;
    public static final int SOCKET_TIMEOUT = 101;
    public static final int MSG_SOCKET_EXCEPTION = 102;
    public static final String TAG = "PandoWifi";
    private WifiController mWifiController;
    private ProgressDialog mProgressDialog;
    private ProgressDialog mConnectProgressDialog;
    private FragmentManager fragmentManager;
    private ScanP2pFragment mScanP2pFragment;
    private ScanWifiFragment mScanWifiFragment;
    private ConnectDialog mConnectDialog;
    private InetAddress mServerAddress;
    private Handler mHandler;
    private JSONObject jsonObject = null;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private SocketController mSocketController;
    private boolean connectFlag = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_wifi_activity);
        init();
    }

    private void init() {
        mSocketController = new SocketController(this);

        //wifi p2p support check
        mWifiController = new WifiController(this);
        if (!mWifiController.isDeviceSupportedWifiDirect()) {
            finish();
            return;
        }

        //ui
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mScanP2pFragment = new ScanP2pFragment();
        mScanWifiFragment = new ScanWifiFragment();
        fragmentTransaction.add(R.id.scan_container_layout, mScanP2pFragment);
        fragmentTransaction.commit();

        //wifi check
        if (mWifiController.isWifiEnabled()) {
            mWifiController.discoverPeers();
        } else {
            mWifiController.setWifiEnabled(true);
        }
        mHandler = new myHandler();

    }

    private void showP2PconnectProgress() {
        if (mConnectProgressDialog == null) {
            mConnectProgressDialog = new ProgressDialog(this);
            mConnectProgressDialog.setButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancelConnect();
                    mConnectProgressDialog.dismiss();
                }
            });
            mConnectProgressDialog.setMessage(getString(R.string.connecting));
            mConnectProgressDialog.setCancelable(false);
        }
        mConnectProgressDialog.show();
    }

    private void dismissP2PconnectProgress() {
        if (mConnectProgressDialog != null) {
            mConnectProgressDialog.dismiss();
        }
    }

    private void showProgressDialog(String msg) {
        mProgressDialog = ProgressDialog.show(this, null, msg);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    private void checkWifiConnectResult(JSONObject jsonObject) {
        dismissProgressDialog();
        int result = JSONUtil.getConnectResult(jsonObject);
        switch (result) {
            case DISABLED_AUTH_FAILURE:
                Toast.makeText(this, getString(R.string.connect_authentication_problem), Toast.LENGTH_SHORT).show();
                break;
            case DISABLED_DHCP_FAILURE:
            case DISABLED_DNS_FAILURE:
                Toast.makeText(this, getString(R.string.connect_avoided), Toast.LENGTH_SHORT).show();
                break;
            case DISABLED_ASSOCIATION_REJECT:
                Toast.makeText(this, getString(R.string.connect_disabled), Toast.LENGTH_SHORT).show();
                break;
            case DISABLED_TIMEOUT:
                Toast.makeText(this, getString(R.string.connect_timeout), Toast.LENGTH_SHORT).show();
                break;
            case ENABLED:
                Toast.makeText(this, getString(R.string.connect_success), Toast.LENGTH_SHORT).show();
                JSONObject finish = new JSONObject();
                JSONUtil.setCommand(finish, CommandUtil.CONNECT_FINISHED);
                mSocketController.send(finish.toString());
                finish();
                break;
            default:
                break;
        }
    }

    public void setTimeOut(int timeOut) {
        mTimer = new Timer();
        mTimerTask = new TimeOutTask(timeOut);
        mTimer.schedule(mTimerTask, 2000, 1000);
    }

    public void cancelTimeOut() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @Override
    public void receiveData(String data) {
        Log.i(TAG, "receive data " + data);
        try {
            jsonObject = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        mHandler.sendEmptyMessage(JSONUtil.getCommand(jsonObject));
    }

    @Override
    public void socketException() {
        if (mSocketController != null) {
            mSocketController.release();
            mHandler.sendEmptyMessage(MSG_SOCKET_EXCEPTION);
        }
    }

    @Override
    public void connectState(boolean state) {
        if (state) {
            dismissP2PconnectProgress();
            if (mScanP2pFragment.isResumed()) {
                mHandler.sendEmptyMessage(MSG_P2P_CONNECTED);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.scan_container_layout, mScanWifiFragment);
                fragmentTransaction.commit();
            }
        }
    }

    private class TimeOutTask extends TimerTask {
        int timeout = 20;
        int i = 1;

        public TimeOutTask(int time) {
            timeout = time / 1000;
        }

        @Override
        public void run() {
            Log.i(TAG, "WifiController--- time " + i);
            if (i >= timeout) {
                Log.i(TAG, "WifiController--- time out");
                mHandler.sendEmptyMessage(SOCKET_TIMEOUT);
            }
            i++;
        }
    }

    @Override
    public void onConnectDialogOK(int securityMode, String ssid, String password, int timeout) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setConnectCommand(jsonObject, securityMode, ssid, password, timeout);
        mSocketController.send(jsonObject.toString());
        if (mConnectDialog != null) {
            mConnectDialog.dismiss();
            mConnectDialog = null;
        }
        showProgressDialog(getString(R.string.connecting));
        //setTimeOut(timeout);
    }

    @Override
    public void onConnectDialogCancel() {
        if (mConnectDialog != null) {
            mConnectDialog.dismiss();
            mConnectDialog = null;
        }
    }

    private class myHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int cmd = msg.what;
            switch (cmd) {
                case CommandUtil.SCAN_WIFI_RESULT:
                    //mScanWifiFragment.setLoadingView(false);
                    mScanWifiFragment.setScanButtonClickable(true);
                    mScanWifiFragment.updateWifiList(JSONUtil.getWifiDeviceList(jsonObject));
                    break;
                case CommandUtil.CONNECT_WIFI_RESULT:
                    // cancelTimeOut();
                    checkWifiConnectResult(jsonObject);
                    break;
                case SOCKET_TIMEOUT:
                    dismissProgressDialog();
                    //cancelTimeOut();
                    Toast.makeText(getApplicationContext(), getString(R.string.socket_timeout), Toast.LENGTH_SHORT).show();
                case MSG_SOCKET_EXCEPTION:
                    Toast.makeText(getApplicationContext(), getString(R.string.socket_exception), Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case MSG_P2P_CONNECTED:
                    Toast.makeText(getApplicationContext(), getString(R.string.wifip2p_connected), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (mWifiController != null) {
            mWifiController.release();
        }
        if (mSocketController != null) {
            mSocketController.release();
        }
        super.onDestroy();
    }

    @Override
    public void sendScanWifiCommand() {
//        mSendThread = new SendUDPThread(mServerAddress);
//        JSONObject jsonObject = new JSONObject();
//        JSONUtil.setCommand(jsonObject, CommandUtil.SCAN_WIFI);
//        mSendThread.setData(jsonObject);
//        mSendThread.start();
        mScanWifiFragment.setScanButtonClickable(false);
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, CommandUtil.SCAN_WIFI);
        mSocketController.send(jsonObject.toString());
    }

    @Override
    public void showConnectDialog(ScannedWifiDevice device) {
        mConnectDialog = new ConnectDialog(this, device);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        Window dialogWindow = mConnectDialog.getWindow();
        WindowManager.LayoutParams p = dialogWindow.getAttributes(); // 获取对话框当前的参数值
        p.width = (int) (screenWidth * 0.88); // 宽度设置为屏幕的0.65

        mConnectDialog.show();
    }

    @Override
    public void connectDevice(WifiP2pDevice device) {
        showP2PconnectProgress();
        mWifiController.connectDevice(device);
    }

    @Override
    public void cancelConnect() {
        mWifiController.cancelConnect();
    }

    @Override
    public void discoverPeers() {
        mWifiController.discoverPeers();
    }

    @Override
    public void notifyOnPeersAvailable(WifiP2pDeviceList peers) {
        mScanP2pFragment.updateList(peers);
    }

    @Override
    public void notifyOnConnectionInfoAvailable(WifiP2pInfo info) {
        if (info.groupFormed) {
            if (connectFlag) {
                connectFlag = false;
                //connect socket
                String host = info.groupOwnerAddress.toString();
                mSocketController.connect(host.substring(1, host.length()));
            }
        } else {
            if (mScanWifiFragment.isResumed()) {
                dismissProgressDialog();
                Toast.makeText(this, getString(R.string.wifip2p_disconnect), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void notifyDiscoveryStarted() {
        if (mScanP2pFragment != null && mScanP2pFragment.isResumed()) {
            mScanP2pFragment.setScanButtonClickable(false);
        }
    }

    @Override
    public void notifyDiscoveryStopped() {
        if (mScanP2pFragment != null && mScanP2pFragment.isResumed()) {
            mScanP2pFragment.setScanButtonClickable(true);
        }
    }
}
