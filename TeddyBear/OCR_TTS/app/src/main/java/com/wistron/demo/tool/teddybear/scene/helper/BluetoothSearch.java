package com.wistron.demo.tool.teddybear.scene.helper;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by king on 16-9-19.
 */

public class BluetoothSearch {
    private static final String TAG = "BluetoothSearch";

    private static BluetoothSearch instance;
    private Context context;
    private BluetoothAdapter mBTAdapter;
    private boolean isBroadcastRegisted = false;

    private ArrayList<BtFoundDevice> mTempList;
    private ArrayList<BtFoundDevice> mNewDevicesList;

    private ScheduledExecutorService mScheduledTimer;
    private ScheduledFuture<?> mTask;
    private int CHECK_TIME = 3; // seconds

    private BluetoothSearch(Context context) {
        this.context = context;
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        openBT();

        mTempList = new ArrayList<>();
        mNewDevicesList = new ArrayList<>();

        mScheduledTimer = Executors.newScheduledThreadPool(1);
    }

    public static BluetoothSearch getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothSearch(context);
        }
        return instance;
    }

    private void openBT() {
        if (null == mBTAdapter) {
            return;
        }
        if (!mBTAdapter.isEnabled()) {
            mBTAdapter.enable();
        }
        mBTAdapter.setName(SceneCommonHelper.RESET_BT_NAME);

        // set Bluetooth scan mode(Never time out)
        try {
            Method setScanMode = mBTAdapter.getClass().getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);
            boolean setScanModeSuccess = (boolean) setScanMode.invoke(mBTAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
            Log.i("King", "set bluetooth scan mode to SCAN_MODE_CONNECTABLE_DISCOVERABLE: " + setScanModeSuccess);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void registerDiscoveryBroadcast() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_FOUND);
        mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mNewDeviceReceiver, mFilter);
        isBroadcastRegisted = true;
    }

    private void unregisterDiscoveryBroadcast() {
        context.unregisterReceiver(mNewDeviceReceiver);
    }

    public void start() {
        int decodeStyle = SceneCommonHelper.getSvaSpeakerRecognitionStyle();
        if (decodeStyle == SceneCommonHelper.SVA_SPEAKER_RECOGNITION_STYLE_BT) {
            Log.i(TAG, "start searching BT devices...");
            registerDiscoveryBroadcast();
            mTask = mScheduledTimer.scheduleWithFixedDelay(checkBTSearch, 1, CHECK_TIME, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        Log.i(TAG, "stop searching BT devices...");
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        if (mBTAdapter != null && mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }
        if (isBroadcastRegisted) {
            unregisterDiscoveryBroadcast();
            isBroadcastRegisted = false;
        }
        if (mTempList != null) {
            mTempList.clear();
        }
        if (mNewDevicesList != null) {
            mNewDevicesList.clear();
        }
    }

    public BtConfigDevice getNearestPerson() {
        if (mNewDevicesList.size() <= 0) {
            Collections.sort(mTempList, new SortBTDevice());
            mNewDevicesList.addAll(mTempList);
        }

        ArrayList<BtConfigDevice> configDevices = SceneCommonHelper.getConfigDevices(context);
        for (BtFoundDevice foundDevice : mNewDevicesList) {
            for (BtConfigDevice configDevice : configDevices) {
                if (foundDevice.getMacAddress().equalsIgnoreCase(configDevice.getBtMacAddress())) {
                    return configDevice;
                }
            }
        }

        return null;
    }

    private Runnable checkBTSearch = new Runnable() {
        @Override
        public void run() {
            if (mBTAdapter != null) {
                if (!mBTAdapter.isEnabled()) {
                    mBTAdapter.enable();
                } else {
                    if (!mBTAdapter.isDiscovering()) {
                        startSearch();
                    }
                }
            }
        }
    };

    private void startSearch() {
        openBT();
        searchNewDevice();
    }

    private void searchNewDevice() {
        // TODO Auto-generated method stub
        mTempList.clear();
        if (mBTAdapter != null) {
            if (mBTAdapter.isDiscovering()) {
                mBTAdapter.cancelDiscovery();
            }
            Log.i(TAG, "BT discovery start...");
            mBTAdapter.startDiscovery();
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
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                BtFoundDevice btFoundDevice = new BtFoundDevice(name, macAddress, rssi);
                Log.i(TAG, "" + btFoundDevice);
                mTempList.add(btFoundDevice);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Collections.sort(mTempList, new SortBTDevice());
                Log.i(TAG, "BT discovery finished... result is: \n" + Arrays.toString(mTempList.toArray(new BtFoundDevice[mTempList.size()])));
                mNewDevicesList.clear();
                mNewDevicesList.addAll(mTempList);
            }
        }
    };

    private class SortBTDevice implements Comparator<BtFoundDevice> {

        @Override
        public int compare(BtFoundDevice lhs, BtFoundDevice rhs) {
            return rhs.getRssi() - lhs.getRssi();  // small -> big
        }
    }
}
