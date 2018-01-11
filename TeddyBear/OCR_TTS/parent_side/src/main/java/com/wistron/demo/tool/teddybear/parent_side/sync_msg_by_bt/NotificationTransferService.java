package com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by king on 17-1-5.
 */

public class NotificationTransferService extends NotificationListenerService {
    private static final String TAG = "NotiTransferService";

    private final String NOTIFICATION_KEY_TITLE = "android.title";  // Sender
    private final String NOTIFICATION_KEY_SUBTEXT = "android.subText";  // Recipient
    private final String NOTIFICATION_KEY_TEXT = "android.text";  // Subject

    private BluetoothChatHandler mChatHandler;
    private BluetoothDevice mServerDevice;
    private String mServerAddress = "40:83:DE:BB:6A:6C";

    private boolean isConnecting = false;
    private Map<Integer, String> mNeedToSendMsg = new HashMap<>();

    private boolean isBroadcastRegisted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate...");
        mChatHandler = new BluetoothChatHandler(this, BluetoothChatHandler.MODE_CLIENT);
        if (mChatHandler.getBluetoothAdapter() == null) {
            stopSelf();
        } else {
            mChatHandler.setOnWisBluetoothChatStateChangedListener(mBTChatStateChangedListener);
            mChatHandler.setOnWisBluetoothChatDataChangedListener(mBTChatDataChangedListener);

            connectToDevice();
        }
    }

    private void connectToDevice() {
        if (!mChatHandler.getBluetoothAdapter().isEnabled()) {
            mChatHandler.getBluetoothAdapter().enable();
        }

        // search BT device
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mNewDeviceReceiver, mFilter);
        isBroadcastRegisted = true;

        if (!mChatHandler.getBluetoothAdapter().isDiscovering()) {
            searchNewDevice();
        }
    }

    private BroadcastReceiver mNewDeviceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name = device.getName();
                String macAddress = device.getAddress();
                Log.i(TAG, "---> have searched the remote device: " + name + "(" + macAddress + ")");
                if (!TextUtils.isEmpty(name) && name.equalsIgnoreCase(CommonHelper.RESET_BT_NAME)) {
                    Log.i(TAG, "have searched the remote device: " + name + "(" + macAddress + ")");
                    stopDiscovery();

                    mServerAddress = macAddress.toUpperCase();
                    mServerDevice = mChatHandler.getBluetoothAdapter().getRemoteDevice(mServerAddress);
                    // Attempt to connect to the device
                    mChatHandler.connect(mServerDevice, false);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                stopDiscovery();
            }
        }
    };

    private void searchNewDevice() {
        // TODO Auto-generated method stub
        if (mChatHandler.getBluetoothAdapter() != null) {
            if (mChatHandler.getBluetoothAdapter().isDiscovering()) {
                mChatHandler.getBluetoothAdapter().cancelDiscovery();
            }
            Log.i(TAG, "BT discovery start...");
            mChatHandler.getBluetoothAdapter().startDiscovery();
        }
    }

    public void stopDiscovery() {
        Log.i(TAG, "stop searching BT devices...");
        if (mChatHandler.getBluetoothAdapter() != null && mChatHandler.getBluetoothAdapter().isDiscovering()) {
            mChatHandler.getBluetoothAdapter().cancelDiscovery();
        }
        if (isBroadcastRegisted) {
            unregisterReceiver(mNewDeviceReceiver);
            isBroadcastRegisted = false;
        }
    }

    private BluetoothChatHandler.OnWisBluetoothChatStateChangedListener mBTChatStateChangedListener = new BluetoothChatHandler.OnWisBluetoothChatStateChangedListener() {

        @Override
        public void onStateIsListening() {
            // TODO Auto-generated method stub
            Log.i(TAG, "Listening client ");
            isConnecting = false;
        }

        @Override
        public void onStateIsIdle() {
            // TODO Auto-generated method stub
            Log.i(TAG, "onStateIsIdle...");
            isConnecting = false;
        }

        @Override
        public void onStateIsDisconnect(String remoteName) {
            // TODO Auto-generated method stub
            Log.i(TAG, "disconnect client : " + remoteName);
            isConnecting = false;
        }

        @Override
        public void onStateIsConnecting() {
            // TODO Auto-generated method stub
            Log.i(TAG, "onStateIsConnecting...");
            isConnecting = true;
        }

        @Override
        public void onStateIsConnected(String remoteName) {
            // TODO Auto-generated method stub
            Log.i(TAG, "connected client : " + remoteName);
            isConnecting = false;
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Set<Integer> keys = mNeedToSendMsg.keySet();
                    for (int key : keys) {
                        mChatHandler.write(null, mNeedToSendMsg.get(key));
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    mNeedToSendMsg.clear();
                }
            });
        }

        @Override
        public void onStateIsConnectFail() {
            // TODO Auto-generated method stub
            Log.i(TAG, "onStateIsConnectFail...");
            isConnecting = false;
        }

    };

    private BluetoothChatHandler.OnWisBluetoothChatDataChangedListener mBTChatDataChangedListener = new BluetoothChatHandler.OnWisBluetoothChatDataChangedListener() {

        @Override
        public void onReadMessage(String from, String msg) {
            // TODO Auto-generated method stub
            String log = String.format("Get data from TeddyBear(%1$s): %2$s", from, msg);
            Log.i(TAG, log);
        }
    };

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        //super.onNotificationPosted(sbn);
        Log.i(TAG, "onNotificationPosted...");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> grantedApps = sharedPreferences.getStringSet(NotificationAccessSettingsFragment.KEY_APPS, null);

        if (grantedApps.contains(sbn.getPackageName())) {
            String msg = "";
            if (sbn.getNotification().extras != null) {
                if (null != sbn.getNotification().extras.getString(NOTIFICATION_KEY_TITLE)) {
                    msg += String.format(getString(R.string.notification_msg_title_is), sbn.getNotification().extras.getString(NOTIFICATION_KEY_TITLE));
                }
                if (null != sbn.getNotification().extras.get(NOTIFICATION_KEY_TEXT)) {
                    msg += String.format(getString(R.string.notification_msg_content_is), sbn.getNotification().extras.get(NOTIFICATION_KEY_TEXT));
                }
            }
            Log.i(TAG, "msg = " + msg);
            if (TextUtils.isEmpty(msg)) {
                return;
            }

            if (isConnecting) {
                mNeedToSendMsg.put(sbn.getId(), msg);
            } else if (mChatHandler.isConnected()) {
                /*Notification notification = sbn.getNotification();
                Bundle extras = notification.extras;
                Log.i(TAG, "------------------------>>>>>>>>>---------------------------");
                Log.i(TAG, ", "+sbn.getTag()+", "+sbn.getId()+", "+(new Date(sbn.getPostTime())));
                Log.i(TAG, "bundle = start [");
                for(String key: extras.keySet()){
                    Log.i(TAG, "key = "+key+", value = "+extras.get(key));
                }
                Log.i(TAG, "] end");*/

                mChatHandler.write(null, msg);
            } else {
                mNeedToSendMsg.put(sbn.getId(), msg);

                connectToDevice();
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        //super.onNotificationRemoved(sbn);
        Log.i(TAG, "onNotificationRemoved...");
    }
}
