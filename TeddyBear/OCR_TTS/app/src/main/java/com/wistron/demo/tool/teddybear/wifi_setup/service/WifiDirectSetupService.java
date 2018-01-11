package com.wistron.demo.tool.teddybear.wifi_setup.service;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.helper.BluetoothSearch;
import com.wistron.demo.tool.teddybear.wifi_setup.socket.SocketController;
import com.wistron.demo.tool.teddybear.wifi_setup.util.CommandUtil;
import com.wistron.demo.tool.teddybear.wifi_setup.util.JSONUtil;
import com.wistron.demo.tool.teddybear.wifi_setup.wifi.WifiController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by aaron on 16-10-19.
 */
public class WifiDirectSetupService extends Service implements WifiController.Notify, SocketController.Notify {
    public static final String TAG = "PandoWifi";
    private WifiController mWifiController;
    private SocketController mSocketController;

    @Override
    public void onCreate() {
        // init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSocketController != null) {
            mSocketController.release();
        }
        if (mWifiController != null) {
            mWifiController.release();
        }
        BluetoothSearch bluetoothSearch = BluetoothSearch.getInstance(this);
        bluetoothSearch.start();
    }

    private void init() {
        if (mWifiController == null) {
            mWifiController = new WifiController(this);
        }
        //check wifi enable
        if (mWifiController.isWifiEnabled()) {
            mWifiController.discoverPeers();
        } else {
            mWifiController.setWifiEnabled(true);
        }
        if (mSocketController == null) {
            mSocketController = new SocketController(this);
        }
    }

    @Override
    public void sendConnectResult(int result) {
        if (mSocketController != null) {
            Log.i(TAG, "send connect result " + result);
            mWifiController.cancelTimeOut();
            JSONObject jsonObject = new JSONObject();
            JSONUtil.setCommand(jsonObject, CommandUtil.CONNECT_WIFI_RESULT);
            JSONUtil.setConnectResult(jsonObject, result);
            mSocketController.sendData(jsonObject.toString());
        }
    }

    @Override
    public void sendScanResult(List<ScanResult> results) {
        if (mSocketController != null) {
            JSONObject jsonObject = new JSONObject();
            JSONUtil.setCommand(jsonObject, CommandUtil.SCAN_WIFI_RESULT);
            JSONUtil.setScanWifiList(jsonObject, results);
            mSocketController.sendData(jsonObject.toString());
        }
    }

    @Override
    public void receiveData(String data) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (JSONUtil.getCommand(jsonObject)) {
            case CommandUtil.SCAN_WIFI:
                mWifiController.startScan();
                break;
            case CommandUtil.CONNECT_WIFI:
                int securityMode = JSONUtil.getSecurityMode(jsonObject);
                int timeout = JSONUtil.getTimeout(jsonObject);
                String ssid = JSONUtil.getSSID(jsonObject);
                String password = JSONUtil.getPassword(jsonObject);
                boolean result = mWifiController.connectWifi(securityMode, ssid, password, timeout);
                if (!result) {
                    sendConnectResult(mWifiController.DISABLED_ASSOCIATION_REJECT);
                }
                break;
            case CommandUtil.CONNECT_FINISHED:
                //stop service and start bt search
                stopSelf();
                break;
            default:
                break;
        }
    }
}
