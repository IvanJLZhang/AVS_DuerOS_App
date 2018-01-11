package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedWifiDevice;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.BTUtil;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.CommandUtil;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.JSONUtil;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util.WisBluetoothChatHandler;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aaron on 16-9-9.
 */
public class SetupActivity extends AppCompatActivity implements ScanBluetoothFragment.ScanBluetoothFragmentUI, ScanWifiFragment.ScanWifiFragmentUI, ConnectDialog.ConnectDialogUI {

    public static final int DISABLED_UNKNOWN_REASON = 0;
    public static final int DISABLED_DNS_FAILURE = 1;
    public static final int DISABLED_DHCP_FAILURE = 2;
    public static final int DISABLED_AUTH_FAILURE = 3;
    public static final int DISABLED_ASSOCIATION_REJECT = 4;
    public static final int DISABLED_BY_WIFI_MANAGER = 5;
    public static final int DISABLED_TIMEOUT = 6;
    public static final int ENABLED = 7;
    private static final String TAG = "PandoBT";
    private static final int REQUEST_CODE_BLUETOOTH = 1;
    private BluetoothAdapter mBTAdapter;
    private BlueToothBroadcast mBluetoothBroadcast;
    private WisBluetoothChatHandler mChatHandler;
    private WisBluetoothChatHandler.OnWisBluetoothChatDataChangedListener mBTDataChangedListener;
    private WisBluetoothChatHandler.OnWisBluetoothChatStateChangedListener mBTStataeChangedListener;

    //UI
    private ProgressDialog mProgressDialog;
    private FragmentManager fragmentManager;
    private ScanBluetoothFragment mScanBluetoothFragment;
    private ScanWifiFragment mScanWifiFragment;
    private ConnectDialog mConnectDialog;
    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scan_bt_activity);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothBroadcast != null) {
            unregisterReceiver(mBluetoothBroadcast);
        }
        if (mChatHandler != null) {
            mChatHandler.stop();
        }
    }

    private void init() {
        //ui
        fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        mScanBluetoothFragment = new ScanBluetoothFragment();
        mScanWifiFragment = new ScanWifiFragment();
        fragmentTransaction.add(R.id.scan_container_layout, mScanBluetoothFragment);
        fragmentTransaction.commit();

        // BT check
        BluetoothManager manager = BTUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            finish();
            return;
        }

        mChatHandler = new WisBluetoothChatHandler(this, mBTAdapter, WisBluetoothChatHandler.MODE_CLIENT);
        mBTDataChangedListener = new BlueToothChatDataChangedListener();
        mBTStataeChangedListener = new BlueToothChatStateChangedListener();
        mChatHandler.setOnWisBluetoothChatDataChangedListener(mBTDataChangedListener);
        mChatHandler.setOnWisBluetoothChatStateChangedListener(mBTStataeChangedListener);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothBroadcast = new BlueToothBroadcast();
        registerReceiver(mBluetoothBroadcast, intentFilter);

        if (!mBTAdapter.isEnabled()) {
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(mIntent, REQUEST_CODE_BLUETOOTH);
        } else {
            scanBT();
        }

    }

    private void scanBT() {
        if (mBTAdapter != null && !mBTAdapter.isDiscovering()) {
            mBTAdapter.startDiscovery();
        }
    }

    private void stopScanBT() {
        if (mBTAdapter != null && !mBTAdapter.isDiscovering()) {
            mBTAdapter.cancelDiscovery();
        }
    }

    @Override
    public void reFreshBluetooth() {
        //Open BT, do not remind user.
        if (!mBTAdapter.isEnabled()) {
            Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(mIntent, REQUEST_CODE_BLUETOOTH);
        } else {
            scanBT();
        }
    }

    @Override
    public void connectBluetooth(BluetoothDevice device) {
        mChatHandler.connect(device);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                scanBT();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, getString(R.string.permission_dine), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void sendScanWifiCommand() {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, CommandUtil.SCAN_WIFI);
        mChatHandler.write(jsonObject.toString());
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
    public void onConnectDialogOK(int securityMode, String ssid, String password, int timeout) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setConnectCommand(jsonObject, securityMode, ssid, password, timeout);
        mChatHandler.write(jsonObject.toString());
        if (mConnectDialog != null) {
            mConnectDialog.dismiss();
            mConnectDialog = null;
        }
        showProgressDialog(getString(R.string.connecting));
    }

    @Override
    public void onConnectDialogCancel() {
        if (mConnectDialog != null) {
            mConnectDialog.dismiss();
            mConnectDialog = null;
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
                finish();
                break;
            default:
                break;
        }
    }

    private class BlueToothBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "intent " + intent.getAction());
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Update BT list
                final BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mScanBluetoothFragment.updateList(device);
                    }
                });
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mScanBluetoothFragment.setLoadingView(false);
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(TAG, "onReceive---------STATE_TURNING_ON");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(TAG, "onReceive---------STATE_ON");
                        scanBT();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(TAG, "onReceive---------STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(TAG, "onReceive---------STATE_OFF");
                        break;
                }
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
            showProgressDialog(getString(R.string.connecting));
        }

        @Override
        public void onStateIsConnected(String remoteName) {
            Log.i(TAG, "onStateIsConnected");
            dismissProgressDialog();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.scan_container_layout, mScanWifiFragment);
            fragmentTransaction.commit();
        }

        @Override
        public void onStateIsConnectFail() {
            Log.i(TAG, "onStateIsConnectFail");
            Toast.makeText(getApplicationContext(), getString(R.string.connect_fail), Toast.LENGTH_SHORT).show();
            dismissProgressDialog();
        }

        @Override
        public void onStateIsDisconnect(String remoteName) {
            Log.i(TAG, "onStateIsDisconnect");
            Toast.makeText(getApplicationContext(), getString(R.string.disconnect_bluetooth), Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onStateIsListening() {
            Log.i(TAG, "onStateIsListening");
        }
    }

    private class BlueToothChatDataChangedListener implements WisBluetoothChatHandler.OnWisBluetoothChatDataChangedListener {

        @Override
        public void onReadMessage(String from, String msg) {
            Log.i(TAG, "msg " + msg);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(msg);
            } catch (JSONException e) {
                e.printStackTrace();
                return;
            }
            switch (JSONUtil.getCommand(jsonObject)) {
                case CommandUtil.SCAN_WIFI_RESULT:
                    mScanWifiFragment.updateWifiList(JSONUtil.getWifiDeviceList(jsonObject));
                    break;
                case CommandUtil.CONNECT_WIFI_RESULT:
                    checkWifiConnectResult(jsonObject);
                    break;
                default:
                    break;
            }
        }
    }
}
