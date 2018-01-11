package com.wistron.demo.tool.teddybear.wifi_setup.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.wifi_setup.service.WifiDirectSetupService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aaron on 16-9-28.
 */
public class WifiController {

    private final String mShellPath = "Dalvik".equals(System.getProperty("java.vm.name")) ? "/system/bin/sh" : "/bin/sh";
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
    private static final String INTENT_WIFI_P2P_INVITATION_RECEIVED = "com.wistron.invitation.received";
    private static final String INTENT_WIFI_P2P_INVITATION_REJECT = "com.wistron.invitation.reject";
    private static final String DEVICE_NAME = "Teddy";
    private String TAG = WifiDirectSetupService.TAG;
    private WifiManager mWifiManager;
    private WifiP2pManager mP2pManager;
    private IntentFilter mIntentFilter;
    private WifiP2pManager.Channel mChannel;
    private boolean mScanAvailableFlag = false;
    private boolean discoverFlag = false;
    private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    public String mClientIp;
    private String mConnectSSID = null;
    private Timer mTimer;
    private Timer mScanTimer;
    private TimerTask mScanTimerTask;
    private TimerTask mTimerTask;
    private Context mContext;

    public WifiController(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mP2pManager.initialize(context, context.getMainLooper(), null);

        mConnectionInfoListener = new ConnectionInfoListener();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(INTENT_WIFI_P2P_INVITATION_RECEIVED);
        mIntentFilter.addAction(INTENT_WIFI_P2P_INVITATION_REJECT);
        context.registerReceiver(new wifiBroadcast(), mIntentFilter);

        //setScanTimer(1500);
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void setWifiEnabled(boolean enable) {
        discoverFlag = true;
        mWifiManager.setWifiEnabled(enable);
    }

    public void discoverPeers() {
        mP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "discover peers success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "discover peers fail, reason: " + reason);
            }
        });
    }

    public void startScan() {
        Log.i(TAG, "start scan wifi");
        mScanAvailableFlag = true;
        mWifiManager.startScan();
        //((WifiDirectSetupService) mContext).sendScanResult(mWifiManager.getScanResults());
    }

    public void reNameP2p() {
        try {
            Method method = mP2pManager.getClass().getMethod("setDeviceName", WifiP2pManager.Channel.class, String.class, WifiP2pManager.ActionListener.class);
            method.invoke(mP2pManager, mChannel, DEVICE_NAME, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "name success");
                }

                @Override
                public void onFailure(int reason) {
                    Log.i(TAG, "name success");
                }
            });

        } catch (Exception e) {
            Log.i(TAG, "Exception " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean connectWifi(int securityMode, String SSID, String password, int timeout) {
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

    public void setTimeOut(int timeOut) {
        mTimer = new Timer();
        mTimerTask = new TimeOutTask(timeOut);
        mTimer.schedule(mTimerTask, 100, 1000);
    }

    public void cancelTimeOut() {
        if (mConnectSSID != null) {
            mConnectSSID = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void release() {
        mP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, " Remove group success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, " Remove group fail " + reason);
            }
        });
        mP2pManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, " stop discovery success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, " stop discovery fail " + reason);
            }
        });
    }

    private void setScanTimer(int period) {
        mScanTimer = new Timer();
        mScanTimerTask = new ScanTimerTask();
        mScanTimer.schedule(mScanTimerTask, 1000, period);
    }

    public void cancelScanTimer() {
        if (mScanTimerTask != null) {
            mScanTimerTask.cancel();
            mScanTimerTask = null;
        }
        if (mScanTimer != null) {
            mScanTimer.cancel();
            mScanTimer = null;
        }
    }

    private class ScanTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mWifiManager != null) {
                mWifiManager.startScan();
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
                ((WifiDirectSetupService) mContext).sendConnectResult(DISABLED_TIMEOUT);
            } else {
                checkConnectResult();
            }
            i++;
        }
    }

    private void checkConnectResult() {
        List<WifiConfiguration> wifis = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : wifis) {
            if (mConnectSSID.equals(config.SSID) && mConnectSSID != null) {
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
                            ((WifiDirectSetupService) mContext).sendConnectResult(DISABLED_AUTH_FAILURE);
                            break;
                        case DISABLED_DHCP_FAILURE:
                            ((WifiDirectSetupService) mContext).sendConnectResult(DISABLED_DHCP_FAILURE);
                            break;
                        case DISABLED_DNS_FAILURE:
                            ((WifiDirectSetupService) mContext).sendConnectResult(DISABLED_DNS_FAILURE);
                            break;
                        case DISABLED_ASSOCIATION_REJECT:
                            ((WifiDirectSetupService) mContext).sendConnectResult(DISABLED_ASSOCIATION_REJECT);
                            break;

                    }
                    cancelTimeOut();
                    break;
                }
            }
        }
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

    private class ConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            Log.i(TAG, "connection info  " + info);
        }
    }

    private class wifiBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                switch (state) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        Log.i(TAG, "WIFI_STATE_ENABLED");
                        if (discoverFlag) {
                            discoverFlag = false;
                            mP2pManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                                @Override
                                public void onSuccess() {
                                    Log.i(TAG, "discover peers success");
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Log.i(TAG, "discover peers fail, reason: " + reason);
                                }
                            });
                        }
                        break;
                    default:
                        break;
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, "WifiController--- wifi state:" + info.getState());
                if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
                    WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                    if (mConnectSSID != null && mConnectSSID.equals(wifiInfo.getSSID())) {
                        Log.i(TAG, "WifiController--- wifi connected success ");
                        mConnectSSID = null;
                        ((WifiDirectSetupService) mContext).sendConnectResult(ENABLED);
                    }
                }

            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                Log.i(TAG, "SCAN_RESULTS_AVAILABLE_ACTION");
                if (mScanAvailableFlag) {
                    mScanAvailableFlag = false;
                    ((WifiDirectSetupService) mContext).sendScanResult(mWifiManager.getScanResults());
                }

            } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                Log.i(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                Log.i(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
                Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");
                mP2pManager.requestConnectionInfo(mChannel, mConnectionInfoListener);
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
                WifiP2pDevice device = (WifiP2pDevice) intent.getExtras().get(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                Log.i(TAG, "device name " + device.deviceName);
                if (!device.deviceName.equals(DEVICE_NAME)) {
                    reNameP2p();
                }
            } else if (INTENT_WIFI_P2P_INVITATION_RECEIVED.equals(action)) {
                SceneCommonHelper.playSpeakingSound(context, SceneCommonHelper.WARN_SOUND_TYPE_WARNING, false);
            } else if (INTENT_WIFI_P2P_INVITATION_REJECT.equals(action)) {

            }
        }
    }

    private void exeCmd(String cmd) {
        String cmds[] = {"/system/bin/sh", "-c", cmd};
        try {
            Process mRunCmd = new ProcessBuilder(mShellPath, "-c", cmd).start();
            mRunCmd.waitFor();
            mRunCmd.destroy();
            mRunCmd = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface Notify {
        void sendConnectResult(int result);

        void sendScanResult(List<ScanResult> results);
    }
}
