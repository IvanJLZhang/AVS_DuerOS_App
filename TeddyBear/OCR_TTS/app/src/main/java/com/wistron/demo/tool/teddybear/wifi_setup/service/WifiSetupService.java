package com.wistron.demo.tool.teddybear.wifi_setup.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.wifi_setup.util.BTUtil;
import com.wistron.demo.tool.teddybear.wifi_setup.util.CommandUtil;
import com.wistron.demo.tool.teddybear.wifi_setup.util.JSONUtil;
import com.wistron.demo.tool.teddybear.wifi_setup.util.WisBluetoothChatHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aaron on 16-9-9.
 */
public class WifiSetupService extends Service {

    private static final String TAG = "PandoBT";
    public static final int DISABLED_UNKNOWN_REASON = 0;
    public static final int DISABLED_DNS_FAILURE = 1;
    public static final int DISABLED_DHCP_FAILURE = 2;
    public static final int DISABLED_AUTH_FAILURE = 3;
    public static final int DISABLED_ASSOCIATION_REJECT = 4;
    public static final int DISABLED_BY_WIFI_MANAGER = 5;
    public static final int DISABLED_TIMEOUT = 6;
    public static final int ENABLED = 7;
    public static final int SECURITY_NONE = 1;
    public static final int SECURITY_WPA = 2;
    public static final int SECURITY_WEP = 3;
    private WisBluetoothChatHandler mChatHandler;
    private BluetoothAdapter mBTAdapter;
    private WifiManager mWifiManager;
    private boolean mScanFlag = false;
    private boolean mScanAvailableFlag = false;
    private String mClientMac;
    private String mConnectSSID = null;
    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    public void onCreate() {

        init();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void init() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(new BlueToothBroadcast(), intentFilter);

        BluetoothManager manager = BTUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        mChatHandler = new WisBluetoothChatHandler(this, mBTAdapter, WisBluetoothChatHandler.MODE_SERVER);
        mChatHandler.setOnWisBluetoothChatStateChangedListener(new BlueToothChatStateChangedListener());
        mChatHandler.setOnWisBluetoothChatDataChangedListener(new BlueToothChatDataChangedListener());
        setBluetoothNameRandom();
        //check BT state
        if (!mBTAdapter.isEnabled()) {
            mBTAdapter.enable();
        } else {
            setBluetoothDiscoverable();
            mChatHandler.startServer();
        }

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mWifiManager == null) {
            Log.i(TAG, "get wifi manager null");
        }
        //register wifi receiver
        WifiBroadcast mWifiBroadcast = new WifiBroadcast();
        IntentFilter wifiIntent = new IntentFilter();
        wifiIntent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        wifiIntent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mWifiBroadcast, wifiIntent);
    }

    private void sendScanResult() {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, CommandUtil.SCAN_WIFI_RESULT);
        JSONUtil.setScanWifiList(jsonObject, mWifiManager.getScanResults());
        Log.i(TAG, " send scan result  " + jsonObject.toString());
        mChatHandler.write(mClientMac, jsonObject.toString());
    }

    private void setBluetoothNameRandom() {
        mBTAdapter.setName(SceneCommonHelper.RESET_BT_NAME);
    }

    private void setBluetoothDiscoverable() {
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(mBTAdapter, 3600);
            setScanMode.invoke(mBTAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 3600);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean connectWifi(int securityMode, String SSID, String password, int timeout) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + SSID + "\"";
        //config.BSSID="\"" + BSSID + "\"";

        // 分为三种情况：1没有密码2用wep加密3用wpa加密
        if (securityMode == SECURITY_NONE) {// WIFICIPHER_NOPASS
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        } else if (securityMode == SECURITY_WEP) {  //  WIFICIPHER_WEP
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        } else if (securityMode == SECURITY_WPA) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        }

        WifiConfiguration tempConfig = isExists(config.SSID);
        if (tempConfig != null) {
            Log.i(TAG, "WifiController--- Remove config");
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        mConnectSSID = config.SSID;
        setTimeOut(timeout);

        int id = mWifiManager.addNetwork(config);
        return mWifiManager.enableNetwork(id, true);
    }

    private WifiConfiguration isExists(String ssid) {
        Log.i(TAG, "WifiController--- isExists ssid " + ssid);
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : list) {
            if (configuration.SSID.equals(ssid)) {
                return configuration;
            }
        }
        return null;
    }

    private void checkConnectResult() {
        List<WifiConfiguration> wifis = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifis) {
            if (mConnectSSID.equals(config.SSID)) {
                int disableReason = DISABLED_UNKNOWN_REASON;
                int status = -1;
                Class cl = config.getClass();
                Field[] fields = cl.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    field.setAccessible(true);
                    try {
                        Object value = field.get(config);
                        if (field.getName().equals("disableReason")) {
                            disableReason = (int) value;
                            Log.i(TAG, "WifiController--- checkConnectResult disableReason " + disableReason);
                        } else if (field.getName().equals("status")) {
                            status = (int) value;
                            Log.i(TAG, "WifiController--- checkConnectResult status " + status);
                        }

                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                if (status == WifiConfiguration.Status.DISABLED && disableReason != DISABLED_UNKNOWN_REASON) {
                    switch (disableReason) {
                        case DISABLED_AUTH_FAILURE:
                            sendConnectResult(DISABLED_AUTH_FAILURE);
                            break;
                        case DISABLED_DHCP_FAILURE:
                            sendConnectResult(DISABLED_DHCP_FAILURE);
                            break;
                        case DISABLED_DNS_FAILURE:
                            sendConnectResult(DISABLED_DNS_FAILURE);
                            break;
                        case DISABLED_ASSOCIATION_REJECT:
                            sendConnectResult(DISABLED_ASSOCIATION_REJECT);
                            break;

                    }
                    cancelTimeOut();
                    break;
                }
            }
        }
    }

    private void sendConnectResult(int result) {
        cancelTimeOut();
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, CommandUtil.CONNECT_WIFI_RESULT);
        JSONUtil.setConnectResult(jsonObject, result);
        mChatHandler.write(mClientMac, jsonObject.toString());
    }

    public void setTimeOut(int timeOut) {
        mTimer = new Timer();
        mTimerTask = new TimeOutTask(timeOut);
        mTimer.schedule(mTimerTask, 100, 1000);
    }

    private void cancelTimeOut() {
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
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
                sendConnectResult(DISABLED_TIMEOUT);
            } else {
                checkConnectResult();
            }
            i++;
        }
    }

    private class BlueToothBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.i(TAG, "onReceive---------STATE_TURNING_ON");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.i(TAG, "onReceive---------STATE_ON");
                            setBluetoothDiscoverable();
                            mChatHandler.startServer();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.i(TAG, "onReceive---------STATE_TURNING_OFF");
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.i(TAG, "onReceive---------STATE_OFF");
                            break;
                    }
                    break;
            }
        }
    }

    private class BlueToothChatStateChangedListener implements WisBluetoothChatHandler.OnWisBluetoothChatStateChangedListener {

        @Override
        public void onStateIsIdle() {
            Log.i(TAG, "onStateIsIdle");
        }

        @Override
        public void onStateIsConnecting() {
            Log.i(TAG, "onStateIsConnecting");
        }

        @Override
        public void onStateIsConnected(String remoteName) {
            Log.i(TAG, "onStateIsConnected");
        }

        @Override
        public void onStateIsConnectFail() {
            Log.i(TAG, "onStateIsConnectFail");
        }

        @Override
        public void onStateIsDisconnect(String remoteName) {
            Log.i(TAG, "onStateIsDisconnect");
        }

        @Override
        public void onStateIsListening() {
            Log.i(TAG, "onStateIsListening");
        }
    }

    private class BlueToothChatDataChangedListener implements WisBluetoothChatHandler.OnWisBluetoothChatDataChangedListener {

        @Override
        public void onReadMessage(String from, String msg) {
            Log.i(TAG, "msg  " + msg);
            mClientMac = from;
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(msg);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            switch (JSONUtil.getCommand(jsonObject)) {
                case CommandUtil.SCAN_WIFI:
                    if (mWifiManager.isWifiEnabled()) {
                        mWifiManager.startScan();
                        mScanAvailableFlag = true;
                        Log.i(TAG, " start scan wifi ");
                    } else {
                        mScanFlag = true;
                        mWifiManager.setWifiEnabled(true);
                    }
                    break;
                case CommandUtil.CONNECT_WIFI:
                    int securityMode = JSONUtil.getSecurityMode(jsonObject);
                    int timeout = JSONUtil.getTimeout(jsonObject);
                    String ssid = JSONUtil.getSSID(jsonObject);
                    String password = JSONUtil.getPassword(jsonObject);
                    boolean result = connectWifi(securityMode, ssid, password, timeout);
                    if (!result) {
                        sendConnectResult(DISABLED_ASSOCIATION_REJECT);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private class WifiBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.i(TAG, " wifi enabled ");
                        if (mScanFlag) {
                            mScanFlag = false;
                            mScanAvailableFlag = true;
                            mWifiManager.startScan();
                            Log.i(TAG, " start scan wifi ");
                        }
                        break;
                    default:
                        break;
                }
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                if (mScanAvailableFlag) {
                    Log.i(TAG, " scan available ");
                    mScanAvailableFlag = false;
                    sendScanResult();
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, "WifiController--- wifi state:" + info.getState());
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (mConnectSSID != null && mConnectSSID.equals(wifiInfo.getSSID())) {
                        Log.i(TAG, "WifiController--- wifi connected success ");
                        mConnectSSID = null;
                        sendConnectResult(ENABLED);
                    }
                }
            }
        }
    }
}
