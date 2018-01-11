package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui.SetupWifiActivity;

/**
 * Created by aaron on 16-9-28.
 */
public class WifiController {

    private String TAG = SetupWifiActivity.TAG;
    private WifiManager mWifiManager;
    private WifiP2pManager mP2pManager;
    private IntentFilter mIntentFilter;
    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager.PeerListListener mPeerListListener;
    private WifiP2pManager.ConnectionInfoListener mConnectionInfoListener;
    private SetupWifiActivity mActivity;
    private Context mContext;
    private wifiBroadcast mBroadcast;

    public WifiController(Context context) {
        mContext = context;
        mActivity = (SetupWifiActivity) context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mP2pManager.initialize(context, Looper.getMainLooper(), null);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        mBroadcast = new wifiBroadcast();
        context.registerReceiver(mBroadcast, mIntentFilter);

        mPeerListListener = new PeersListListener();
        mConnectionInfoListener = new ConnectionInfoListener();
    }

    public boolean isDeviceSupportedWifiDirect() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT);

    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void setWifiEnabled(boolean enable) {
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

    public void cancelConnect() {
        mP2pManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "cancel connect success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "cancel connect success fail, reason: " + reason);
            }
        });
    }

    public void connectDevice(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = 0;
        mP2pManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "do connect success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "do connect fail " + reason);
            }
        });
    }

    public void release() {
//        if(mP2pManager!=null){
//            mP2pManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
//                @Override
//                public void onSuccess() {
//                    Log.i(TAG, "remove group success");
//                }
//
//                @Override
//                public void onFailure(int reason) {
//                    Log.i(TAG, "remove group fail "+reason);
//                }
//            });
//        }
        if (mBroadcast != null) {
            mContext.unregisterReceiver(mBroadcast);
        }
    }

    private class PeersListListener implements WifiP2pManager.PeerListListener {

        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            Log.i(TAG, "peers " + peers);
            ((SetupWifiActivity) mContext).notifyOnPeersAvailable(peers);
        }
    }

    private class ConnectionInfoListener implements WifiP2pManager.ConnectionInfoListener {

        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            Log.i(TAG, "connection info  " + info);
            ((SetupWifiActivity) mContext).notifyOnConnectionInfoAvailable(info);
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
                        break;
                    default:
                        break;
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {

            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {

            } else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                Log.i(TAG, "WIFI_P2P_STATE_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                Log.i(TAG, "WIFI_P2P_PEERS_CHANGED_ACTION");
                mP2pManager.requestPeers(mChannel, mPeerListListener);
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
                Log.i(TAG, "WIFI_P2P_CONNECTION_CHANGED_ACTION");

                mP2pManager.requestConnectionInfo(mChannel, mConnectionInfoListener);
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");

            } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, 0);
                switch (state) {
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
                        Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION   started");
                        ((SetupWifiActivity) mContext).notifyDiscoveryStarted();
                        break;
                    case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
                        Log.i(TAG, "WIFI_P2P_THIS_DEVICE_CHANGED_ACTION  stopped");
                        ((SetupWifiActivity) mContext).notifyDiscoveryStopped();
                        break;
                    default:
                        break;
                }

            }
        }
    }

    public interface notify {
        void notifyOnPeersAvailable(WifiP2pDeviceList peers);

        void notifyOnConnectionInfoAvailable(WifiP2pInfo info);

        void notifyDiscoveryStarted();

        void notifyDiscoveryStopped();
    }
}
