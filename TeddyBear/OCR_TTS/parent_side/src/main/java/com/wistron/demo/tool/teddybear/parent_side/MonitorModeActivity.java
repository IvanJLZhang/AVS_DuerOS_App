package com.wistron.demo.tool.teddybear.parent_side;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * Time：16-5-3 18:20
 * Author：bob
 */
public class MonitorModeActivity extends AppCompatActivity implements MonitorModeAdapter.Callback, View.OnClickListener {
    public static final String TAG = "MonitorModeActivity";
    public final static String UPDATE_WARNING_UI_ACTION = "com.wistron.teddybear.parent.update.warning.log.info";

    public static final String KEY_MONITOR_MODE_TIME = "monitor_mode_time";
    public static final String KEY_MONITOR_MODE_DB = "monitor_mode_db";

    private final int MSG_SYNC_ERROR = 0;
    private final int MSG_SYNC_SETTINGS_SUCCESS = 1;
    private final int MSG_SYNC_START_SUCCESS = 2;
    private final int MSG_SYNC_START_ERROR = 3;
    private final int MSG_SYNC_STOP_SUCCESS = 4;
    private final int MSG_SYNC_STOP_ERROR = 5;
    private final int MSG_STOP_SERVICE_SUCCESS = 6;
    private final int MSG_STOP_SERVICE_FAIL = 7;
    private final int MSG_STOP_SERVICE_NEXT_ONE = 8;

    private final int CONNECT_TYPE_SMB_NO_DOMAIN = 0;
    private final int CONNECT_TYPE_SMB_DOMAIN = 1;
    private final int CONNECT_TYPE_FTP = 2;
    private int mCurConntectType = CONNECT_TYPE_FTP;

    private final int ACTION_UPLOAD_SETTING = 0;
    private final int ACTION_UPLOAD_START = 1;
    private final int ACTION_UPLOAD_STOP = 2;
    private final int ACTION_STOP_SERVICE_ALL = 3;
    private int mcurAction = ACTION_UPLOAD_SETTING;

    private ArrayList<TeddyBear> list = new ArrayList<>();
    private MonitorModeAdapter monitorModeAdapter;
    private ListView listView;
    private Button btnStartOrStopMonitorModeService;
    private AlertDialog mDialog;
    private ProgressDialog waitingDialog;
    //    private final String mSharedPath = "TeddyBear/MonitorMode";
    private EditText etTime, etDB;
    private TeddyBear mCurTeddyBear = null;
    private int mDefaultTime = 30, mDefaultDB = 50;

    private boolean isNotifyChildStartStopError = false;

    private boolean isCurMonitorModeServiceStatus = true;
    private int mCurListIndex = 0;
    private boolean isStopMonitorService = false;
    private boolean isRegisterListenUpdateWarningUI = false;

    private BaseTaskManager mTaskManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.monitor_mode_list);

        registerWarningReceiver();
        getMonitorModeServiceStatus();
        initTaskManager();
        initView();
        initDialog();
        updateData();
    }

    private void initTaskManager() {
        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mTaskManager.addAzureStorageChangedListener(mResultChangedListener);
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    if (mcurAction == ACTION_UPLOAD_SETTING) {
                        mMainHandler.sendEmptyMessage(MSG_SYNC_SETTINGS_SUCCESS);
                        Log.i(TAG, "upload settings success");
                    } else if (mcurAction == ACTION_UPLOAD_START) {
                        mMainHandler.sendEmptyMessage(MSG_SYNC_START_SUCCESS);
                        Log.i(TAG, "upload start success");
                    } else if (mcurAction == ACTION_UPLOAD_STOP) {
                        if (isStopMonitorService) {
                            //stop next
                            mMainHandler.sendEmptyMessage(MSG_STOP_SERVICE_NEXT_ONE);
                            Log.i(TAG, "stop " + mCurTeddyBear.serial + "success, and will next one");
                        } else {
                            mMainHandler.sendEmptyMessage(MSG_SYNC_STOP_SUCCESS);
                            Log.i(TAG, "upload stop success");
                        }
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    if (isStopMonitorService) {
                        //stop fail,please re-try
                        Message msg = mMainHandler.obtainMessage(MSG_STOP_SERVICE_FAIL);
                        msg.obj = "Stop service fail. Please re-try it.Thanks";
                        mMainHandler.sendMessage(msg);
                    } else {
                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                        msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server) + "\nPlease re-try it.Thanks!";
                        mMainHandler.sendMessage(msg);
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD) {
                    if (mcurAction == ACTION_UPLOAD_SETTING) {
                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                        msg.obj = "sync fail: " + getString(R.string.msg_ftp_sync_fail) + "\nPlease re-try it.Thanks!";
                        mMainHandler.sendMessage(msg);
                    } else if (mcurAction == ACTION_UPLOAD_START) {
                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                        msg.obj = "sync fail:upload start file failed" + "\nPlease re-try it.Thanks!";
                        mMainHandler.sendMessage(msg);
                    } else if (mcurAction == ACTION_UPLOAD_STOP) {
                        if (isStopMonitorService) {
                            //stop fail,please re-try
                            Message msg = mMainHandler.obtainMessage(MSG_STOP_SERVICE_FAIL);
                            msg.obj = "Stop service fail. Please re-try it.Thanks";
                            mMainHandler.sendMessage(msg);
                        } else {
                            Log.i(TAG, "upload stop file failed");
                            Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                            msg.obj = "sync fail: upload stop file failed" + "\nPlease re-try it.Thanks!";
                            mMainHandler.sendMessage(msg);
                        }
                    }
                    Log.i(TAG, "sync failed");
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DELONE) {
                    if (mcurAction == ACTION_UPLOAD_START) {
                        Log.i(TAG, "delete stop file failed");
                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                        msg.obj = "sync fail:delete stop file failed" + "\nPlease re-try it.Thanks!";
                        mMainHandler.sendMessage(msg);
                    } else if (mcurAction == ACTION_UPLOAD_STOP) {
                        if (isStopMonitorService) {
                            //stop fail,please re-try
                            Message msg = mMainHandler.obtainMessage(MSG_STOP_SERVICE_FAIL);
                            msg.obj = "Stop service fail. Please re-try it.Thanks";
                            mMainHandler.sendMessage(msg);
                        } else {
                            Log.i(TAG, "delete start file failed");
                            Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                            msg.obj = "sync fail: delete start file failed" + "\nPlease re-try it.Thanks!";
                            mMainHandler.sendMessage(msg);
                        }
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS_DELONE) {
                    if (mcurAction == ACTION_UPLOAD_START) {
                        File localFile = getLocalPath(true);
                        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                                BaseTaskManager.REQUEST_ACTION_UPLOAD, localFile.getAbsolutePath(),
                                String.format("%1$s/MonitorMode/", mCurTeddyBear.serial), "start.txt");
                    } else if (mcurAction == ACTION_UPLOAD_STOP) {
                        File localFile = getLocalPath(false);
                        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                                BaseTaskManager.REQUEST_ACTION_UPLOAD, localFile.getAbsolutePath(),
                                String.format("%1$s/MonitorMode/", mCurTeddyBear.serial), "stop.txt");
                    }
                }
            }
        }
    };

    private void initView() {
        listView = (ListView) findViewById(R.id.child_list);
        monitorModeAdapter = new MonitorModeAdapter(this, list, this);
        listView.setAdapter(monitorModeAdapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mDialog.show();

                mCurTeddyBear = list.get(position);
                etTime.setText(mCurTeddyBear.time + "");
                etDB.setText(mCurTeddyBear.db + "");
                return true;
            }
        });

        btnStartOrStopMonitorModeService = (Button) findViewById(R.id.start_stop_monitor_mode_service);
        btnStartOrStopMonitorModeService.setOnClickListener(this);

        if (isCurMonitorModeServiceStatus) {
            btnStartOrStopMonitorModeService.setText("Stop Monitor Mode Service");
        } else {
            btnStartOrStopMonitorModeService.setText("Start Monitor Mode Service");
        }
        setDisplayFonts();
    }

    private void setDisplayFonts() {
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");
        btnStartOrStopMonitorModeService.setTypeface(typeface1);
        ((TextView) findViewById(R.id.monitor_mode_not_child_prompt)).setTypeface(typeface1);
    }

    private void initDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.monitor_mode_dialog, null);
        mDialog = new AlertDialog.Builder(this)
                .setTitle("Monitor mode set")
                .setView(view)
                .setPositiveButton("Sync To Child", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        sendInterruptBroadcast(true);

                        syncToChildSettings(mCurTeddyBear);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();

        etTime = (EditText) view.findViewById(R.id.dialog_monitor_mode_time);
        etDB = (EditText) view.findViewById(R.id.dialog_monitor_mode_noise);

        initProgressDialog();
    }

    private void initProgressDialog() {
        waitingDialog = new ProgressDialog(this);
        waitingDialog.setMessage("Connecting server, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);
    }

    private void showProgressDialog(String content) {
        waitingDialog.setMessage(content);
        waitingDialog.show();
    }

    private void updateData() {
        // Managed child list
        Properties properties = new Properties();
        File file = new File(getFilesDir(), ChildrenManagementActivity.CHILDREN_LIST_FILE_NAME);
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            properties.load(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (properties.size() > 0) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences warningInfoLogPreferences = getSharedPreferences("warning_info_log", Activity.MODE_PRIVATE);
            String serial = preferences.getString("serial", null);

            Iterator<Map.Entry<Object, Object>> it = properties.entrySet().iterator();
            ArrayList<String> tempList = new ArrayList<>();
            while (it.hasNext()) {
                Map.Entry<Object, Object> entry = it.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                tempList.add(String.valueOf(key));

                TeddyBear tempTeddyBear = new TeddyBear();
                tempTeddyBear.serial = String.valueOf(key).trim();
                tempTeddyBear.name = String.valueOf(value).trim();
                tempTeddyBear.time = mDefaultTime;
                tempTeddyBear.db = mDefaultDB;
                tempTeddyBear.status = false;
                tempTeddyBear.service_status = isCurMonitorModeServiceStatus;
                tempTeddyBear.warning_log_info = "";

                Log.i(TAG, "update list， serial number: " + tempTeddyBear.serial);

                if (!TextUtils.isEmpty(serial)) {
                    if (serial.contains(tempTeddyBear.serial)) {
                        String temp = preferences.getString(tempTeddyBear.serial, null);
                        if (null == temp || !temp.contains(":")) {
                            tempTeddyBear.time = mDefaultTime;
                            tempTeddyBear.db = mDefaultDB;
                            tempTeddyBear.status = false;
                        } else {
                            if (temp.contains(":")) {
                                String[] strs = temp.split(":");
                                tempTeddyBear.time = Integer.parseInt(strs[0].trim());
                                tempTeddyBear.db = Integer.parseInt(strs[1].trim());
                                tempTeddyBear.status = Boolean.parseBoolean(strs[2].trim());
                            }
                        }
                    }
                }
                long time = warningInfoLogPreferences.getLong(tempTeddyBear.serial, -1);
                if (time != -1) {
                    tempTeddyBear.warning_log_info = "Lastest Warning: " + getDateAndTimeString(time);
                }
                list.add(tempTeddyBear);
            }
        } else if (properties.size() == 0) {
            if (list.size() > 0) {
                list.clear();
            }
        }

        Log.i(TAG, "list size: " + list.size());
        if (list.size() <= 0) {
            findViewById(R.id.monitor_mode_not_child_prompt).setVisibility(View.VISIBLE);
        } else if (list.size() > 0) {
            findViewById(R.id.monitor_mode_not_child_prompt).setVisibility(View.GONE);
        }

        saveList();
        monitorModeAdapter.notifyDataSetChanged();
    }

    private void updateListWarningLogInfo() {
        if (null != list && list.size() > 0) {
            SharedPreferences preferences = getSharedPreferences("warning_info_log", Activity.MODE_PRIVATE);
            for (int i = 0; i < list.size(); i++) {
                TeddyBear teddyBear = list.get(i);
                long time = preferences.getLong(teddyBear.serial, -1);
                if (time != -1) {
                    teddyBear.warning_log_info = "Lastest Warning: " + getDateAndTimeString(time);
                }
            }
        }
    }

    private String getDateAndTimeString(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return format.format(time);
    }

    private void syncToChildSettings(final TeddyBear teddyBear) {
        final Properties properties = new Properties();
        properties.put(KEY_MONITOR_MODE_TIME, etTime.getText().toString().trim());
        properties.put(KEY_MONITOR_MODE_DB, etDB.getText().toString().trim());

        showProgressDialog("Connecting server and sync the setting to child, please wait for a second...");

        File localFile = getLocalPath(properties);
        mcurAction = ACTION_UPLOAD_SETTING;
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                BaseTaskManager.REQUEST_ACTION_UPLOAD, localFile.getAbsolutePath(),
                String.format("%1$s/MonitorMode/", teddyBear.serial), "settings.txt");
    }

    private File getLocalPath(Properties properties) {
        File file = new File(this.getFilesDir() + "/MonitorMode", "settings.txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            properties.store(new FileOutputStream(file), "Please don\'t modify this file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            sendInterruptBroadcast(false);
            switch (msg.what) {
                case MSG_SYNC_ERROR:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    if (mcurAction == ACTION_UPLOAD_START || mcurAction == ACTION_UPLOAD_STOP) {
                        isNotifyChildStartStopError = true;
                        monitorModeAdapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_SYNC_SETTINGS_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, "sync success", Toast.LENGTH_SHORT).show();

                    mCurTeddyBear.time = Integer.parseInt(etTime.getText().toString().trim());
                    mCurTeddyBear.db = Integer.parseInt(etDB.getText().toString().trim());

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MonitorModeActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(mCurTeddyBear.serial, mCurTeddyBear.time + ":" + mCurTeddyBear.db + ":" +
                            mCurTeddyBear.status);
                    editor.apply();
                    editor.commit();
                    break;
                case MSG_SYNC_START_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, "Notify child start success", Toast.LENGTH_SHORT).show();
                    mCurTeddyBear.status = true;

                    Intent startIntent = new Intent();
                    startIntent.setAction(MonitorModeService.ACTION);
                    startIntent.putExtra("serial", mCurTeddyBear.serial);
                    startIntent.putExtra("action", mCurTeddyBear.status ? "start" : "stop");
                    sendBroadcast(startIntent);

                    notifyMonitorModeStatus(mCurTeddyBear);
                    break;
                case MSG_SYNC_START_ERROR:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    isNotifyChildStartStopError = true;
                    monitorModeAdapter.notifyDataSetChanged();
                    break;
                case MSG_SYNC_STOP_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, "Notify child stop success", Toast.LENGTH_SHORT).show();
                    mCurTeddyBear.status = false;

                    Intent stopIntent = new Intent();
                    stopIntent.setAction(MonitorModeService.ACTION);
                    stopIntent.putExtra("serial", mCurTeddyBear.serial);
                    stopIntent.putExtra("action", mCurTeddyBear.status ? "start" : "stop");
                    sendBroadcast(stopIntent);

                    notifyMonitorModeStatus(mCurTeddyBear);
                    break;
                case MSG_SYNC_STOP_ERROR:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    Toast.makeText(MonitorModeActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    isNotifyChildStartStopError = true;
                    monitorModeAdapter.notifyDataSetChanged();

                    break;
                case MSG_STOP_SERVICE_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    isStopMonitorService = false;
                    mCurListIndex = 0;
                    stopMonitorModeService();
                    btnStartOrStopMonitorModeService.setText("Start Monitor Mode Service");
                    saveMonitorModeServiceStatus();
                    Toast.makeText(MonitorModeActivity.this, "Stop service success", Toast.LENGTH_SHORT).show();
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).service_status = false;
                    }
                    monitorModeAdapter.notifyDataSetChanged();
                    break;
                case MSG_STOP_SERVICE_NEXT_ONE:
                    notifyMonitorModeStatus(mCurTeddyBear);
                    list.get(mCurListIndex - 1).status = false;
                    monitorModeAdapter.notifyDataSetChanged();
                    if (mCurListIndex >= list.size()) {
                        isStopMonitorService = false;
                        mMainHandler.sendEmptyMessage(MSG_STOP_SERVICE_SUCCESS);
                    } else {
                        isStopMonitorService = true;
                        mCurTeddyBear = list.get(mCurListIndex);
                        syncChildToStop(mCurTeddyBear);
                        mCurListIndex++;
                    }
                    break;
                case MSG_STOP_SERVICE_FAIL:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    mCurListIndex = 0;
                    isStopMonitorService = false;
                    isCurMonitorModeServiceStatus = true;
                    Toast.makeText(MonitorModeActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }

        }
    };

    private void notifyMonitorModeStatus(TeddyBear teddyBear) {
        SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(teddyBear.serial, teddyBear.time + ":" + teddyBear.db + ":" +
                teddyBear.status);

        editor.apply();
        editor.commit();

        Log.i(TAG, teddyBear.serial + ", " + teddyBear.time + ":" + teddyBear.db + ":" +
                teddyBear.status);
    }


    private void syncToChildStart(final String serial) {
        showProgressDialog("Connecting server and notify child start, please wait for a second...");

        mcurAction = ACTION_UPLOAD_START;
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                BaseTaskManager.REQUEST_ACTION_DELONE,
                String.format("%1$s/MonitorMode/", serial), "stop.txt");

        /*AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                OutputStream out = null;
                try {
                    Log.i(TAG, "Sync data to parent start");
                    if (mCurConntectType == CONNECT_TYPE_SMB_DOMAIN) {
                        NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, userName, password);

                        SmbFile folderFile = new SmbFile("smb://" + ipAddress + mSharedFolder + "/"
                                + serial, authentication);
                        if (!folderFile.exists()) {
                            folderFile.mkdirs();
                        }

                        String stopPath = "smb://" + ipAddress + mSharedFolder + "/" + serial + "/stop.txt";
                        SmbFile stopFile = new SmbFile(stopPath, authentication);
                        if (stopFile.exists()) {
                            stopFile.delete();
                        }

                        String startPath = "smb://" + ipAddress + mSharedFolder + "/" + serial + "/start.txt";

                        SmbFile startFile = new SmbFile(startPath, authentication);
                        if (!startFile.exists()) {
                            startFile.createNewFile();
                        }
                        Log.i(TAG, "notify  parent to start success");
                    } else if (mCurConntectType == CONNECT_TYPE_SMB_NO_DOMAIN) {
                        SmbFile folderFile = new SmbFile("smb://" + userName + ":"
                                + password + "@" + ipAddress + mSharedFolder + "/" + serial);
                        if (!folderFile.exists()) {
                            folderFile.mkdirs();
                        }

                        String stopPath = "smb://" + userName + ":" + password + "@" +
                                ipAddress + mSharedFolder + "/" + serial + "/stop.txt";
                        SmbFile stopFile = new SmbFile(stopPath);
                        if (stopFile.exists()) {
                            stopFile.delete();
                        }

                        String startPath = "smb://" + userName + ":" + password + "@" +
                                ipAddress + mSharedFolder + "/" + serial + "/start.txt";
                        SmbFile startFile = new SmbFile(startPath);
                        if (!startFile.exists()) {
                            startFile.createNewFile();
                        }
                        Log.i(TAG, "notify  parent to start success");
                    } else if (mCurConntectType == CONNECT_TYPE_FTP) {
                        FTPHelper ftpHelper = new FTPHelper(address, Integer.parseInt(port), userName, password);
                        boolean isConnected = ftpHelper.connect();
                        if (isConnected) {
                            boolean createFolder = ftpHelper.createFolder(String.format("%1$s/MonitorMode/", serial));
                            if (createFolder) {
                                Log.i(TAG, "create folder success");
                                boolean deleteStop = ftpHelper.deleteOneFile(String.format("%1$s/MonitorMode/", serial), "stop.txt");
                                if (deleteStop) {
                                    Log.i(TAG, "delete stop file success");
                                    File localFile = getLocalPath(true);
                                    boolean pass = ftpHelper.uploadFile(localFile,
                                            String.format("%1$s/MonitorMode/", serial), "start.txt");
                                    if (pass) {
                                        mMainHandler.sendEmptyMessage(MSG_SYNC_START_SUCCESS);
                                        Log.i(TAG, "upload start file success");
                                    } else {
                                        Log.i(TAG, "upload start file failed");
                                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                                        msg.obj = "sync fail:upload start file failed"+"\nPlease re-try it.Thanks!";
                                        mMainHandler.sendMessage(msg);
                                    }
                                    localFile.delete();
                                } else {
                                    Log.i(TAG, "delete stop file failed");
                                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                                    msg.obj = "sync fail:delete stop file failed"+"\nPlease re-try it.Thanks!";
                                    mMainHandler.sendMessage(msg);
                                }

                            } else {
                                Log.i(TAG, "create folder failed");
                                Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                                msg.obj = "sync fail: Create the " + serial + " folder failed"+"\nPlease re-try it.Thanks!";
                                mMainHandler.sendMessage(msg);
                            }
                        } else {
                            Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                            msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server)+"\nPlease re-try it.Thanks!";
                            mMainHandler.sendMessage(msg);
                        }
                        ftpHelper.disconnect();
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.i(TAG, "notify  parent to start issue: " + e.toString());
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                    msg.obj = "sync fail: " + e.getMessage() +"\nPlease re-try it.Thanks!";
                    mMainHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "notify  parent to start issue: " + e.toString());
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_START_ERROR);
                    msg.obj = "sync fail: " + e.getMessage() +"\nPlease re-try it.Thanks!";
                    mMainHandler.sendMessage(msg);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });*/
    }

    private void syncChildToStop(final TeddyBear teddyBear) {
        if (!isStopMonitorService) {
            showProgressDialog("Connecting server and notify child stop, please wait for a second...");
        }
        mcurAction = ACTION_UPLOAD_STOP;
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                BaseTaskManager.REQUEST_ACTION_DELONE,
                String.format("%1$s/MonitorMode/", teddyBear.serial), "start.txt");

       /* AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = false;
                OutputStream out = null;
                try {
                    if (mCurConntectType == CONNECT_TYPE_SMB_DOMAIN) {
                        NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, userName, password);

                        SmbFile folderFile = new SmbFile("smb://" + ipAddress + mSharedFolder
                                + "/" + teddyBear.serial, authentication);
                        if (!folderFile.exists()) {
                            folderFile.mkdirs();
                        }

                        String startPath = "smb://" + ipAddress + mSharedFolder + "/" + teddyBear.serial + "/start.txt";
                        SmbFile startFile = new SmbFile(startPath, authentication);
                        if (startFile.exists()) {
                            startFile.delete();
                        }

                        String stopPath = "smb://" + ipAddress + mSharedFolder + "/" + teddyBear.serial + "/stop.txt";
                        SmbFile stopFile = new SmbFile(stopPath, authentication);
                        if (!stopFile.exists()) {
                            stopFile.createNewFile();
                        }
                        Log.i(TAG, "notify  parent to stop success");
                    } else if (mCurConntectType == CONNECT_TYPE_SMB_NO_DOMAIN) {
                        SmbFile folderFile = new SmbFile("smb://" + userName + ":" + password + "@"
                                + ipAddress + mSharedFolder + "/" + teddyBear.serial);
                        if (!folderFile.exists()) {
                            folderFile.mkdirs();
                        }

                        String startPath = "smb://" + userName + ":" + password + "@" +
                                ipAddress + mSharedFolder + "/" + teddyBear.serial + "/start.txt";
                        SmbFile startFile = new SmbFile(startPath);
                        if (startFile.exists()) {
                            startFile.delete();
                        }

                        String stopPath = "smb://" + userName + ":" + password + "@"
                                + ipAddress + mSharedFolder + "/" + teddyBear.serial + "/stop.txt";
                        SmbFile stopFile = new SmbFile(stopPath);
                        if (!stopFile.exists()) {
                            stopFile.createNewFile();
                        }
                        Log.i(TAG, "notify  parent to stop success");
                    } else if (mCurConntectType == CONNECT_TYPE_FTP) {
                        FTPHelper ftpHelper = new FTPHelper(address, Integer.parseInt(port), userName, password);
                        boolean isConnected = ftpHelper.connect();
                        if (isConnected) {
                            boolean createFolder = ftpHelper.createFolder(String.format("%1$s/MonitorMode/", teddyBear.serial));
                            if (createFolder) {
                                Log.i(TAG, "create folder success");
                                boolean deleteStop = ftpHelper.deleteOneFile(String.format("%1$s/MonitorMode/", teddyBear.serial), "start.txt");
                                if (deleteStop) {
                                    Log.i(TAG, "delete start file success");
                                    File localFile = getLocalPath(false);
                                    boolean pass = ftpHelper.uploadFile(localFile,
                                            String.format("%1$s/MonitorMode/", teddyBear.serial), "stop.txt");
                                    if (pass) {
                                        mMainHandler.sendEmptyMessage(MSG_SYNC_STOP_SUCCESS);
                                        Log.i(TAG, "upload stop file success");
                                    } else {
                                        Log.i(TAG, "upload stop file failed");
                                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                                        msg.obj = "sync fail: upload stop file failed" +"\nPlease re-try it.Thanks!";
                                        mMainHandler.sendMessage(msg);
                                    }
                                    localFile.delete();
                                } else {
                                    Log.i(TAG, "delete start file failed");
                                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                                    msg.obj = "sync fail: delete start file failed" +"\nPlease re-try it.Thanks!";
                                    mMainHandler.sendMessage(msg);
                                }
                            } else {
                                Log.i(TAG, "create folder failed");
                                Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                                msg.obj = "sync fail: Create the " + teddyBear.serial + " folder failed"+"\nPlease re-try it.Thanks!";
                                mMainHandler.sendMessage(msg);
                            }
                        } else {
                            Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                            msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server) +"\nPlease re-try it.Thanks!";
                            mMainHandler.sendMessage(msg);
                        }
                        ftpHelper.disconnect();
                    }

                    result = true;

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.i(TAG, "notify  parent to stop issue: " + e.toString());
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                    msg.obj = "sync fail: " + e.getMessage()+"\nPlease re-try it.Thanks!";
                    mMainHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "notify  parent to stop issue: " + e.toString());
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_STOP_ERROR);
                    msg.obj = "sync fail: " + e.getMessage()+"\nPlease re-try it.Thanks!";
                    mMainHandler.sendMessage(msg);
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        });*/
    }

    private File getLocalPath(boolean start) {
        File file = new File(getApplicationContext().getFilesDir() + "/MonitorMode", start ? "start.txt" : "stop.txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(("" + start).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
        if (isRegisterListenUpdateWarningUI) {
            unregisterReceiver(listenUpdateWarningReceiver);
        }
    }

    private void registerWarningReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_WARNING_UI_ACTION);
        registerReceiver(listenUpdateWarningReceiver, filter);
        isRegisterListenUpdateWarningUI = true;
    }

    private BroadcastReceiver listenUpdateWarningReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UPDATE_WARNING_UI_ACTION)) {
                updateListWarningLogInfo();
                monitorModeAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isStopMonitorService) {
            return;
        }
        if (isNotifyChildStartStopError) {
            isNotifyChildStartStopError = !isNotifyChildStartStopError;
            return;
        }
        Log.i(TAG, "onCheckedChanged: " + buttonView.getTag() + ", isChecked: " + isChecked);
        mCurTeddyBear = list.get((int) buttonView.getTag());
        sendInterruptBroadcast(true);
        if (isChecked) {
            syncToChildStart(mCurTeddyBear.serial);
        } else {
            syncChildToStop(mCurTeddyBear);
        }
    }

    private void sendInterruptBroadcast(boolean interrupt) {
        Intent intent = new Intent();
        intent.setAction(MonitorModeService.ACTION_FLAG);
        intent.putExtra("interrupt", interrupt);
        sendBroadcast(intent);
    }


    @Override
    public void onClick(View v) {
        if (v == btnStartOrStopMonitorModeService) {
            isCurMonitorModeServiceStatus = !isCurMonitorModeServiceStatus;
            if (isCurMonitorModeServiceStatus) {
                startMonitorModeService();
                btnStartOrStopMonitorModeService.setText("Stop Monitor Mode Service");
                saveMonitorModeServiceStatus();
                if (list.size() > 0) {
                    for (int i = 0; i < list.size(); i++) {
                        list.get(i).service_status = isCurMonitorModeServiceStatus;
                    }
                    monitorModeAdapter.notifyDataSetChanged();
                }
            } else {
                if (list.size() > 0) {
                    if (mCurListIndex >= list.size()) {
                        isStopMonitorService = false;
                    } else {
                        showProgressDialog("Stop service, please wait for a second...");
                        isStopMonitorService = true;
                        mCurTeddyBear = list.get(mCurListIndex);
                        syncChildToStop(mCurTeddyBear);
                        mCurListIndex++;
                    }
                } else {
                    stopMonitorModeService();
                    btnStartOrStopMonitorModeService.setText("Start Monitor Mode Service");
                    saveMonitorModeServiceStatus();
                }
            }
        }
    }

    private void getMonitorModeServiceStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("monitormodeservice", Activity.MODE_PRIVATE);
        isCurMonitorModeServiceStatus = sharedPreferences.getBoolean("monitor_mode_status", true);
    }

    private void saveMonitorModeServiceStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences("monitormodeservice", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("monitor_mode_status", isCurMonitorModeServiceStatus);
        editor.apply();
        editor.commit();
    }

    private void startMonitorModeService() {
        Intent launchMonitorModeIntent = new Intent();
        launchMonitorModeIntent.setClass(this, MonitorModeService.class);
        startService(launchMonitorModeIntent);
    }

    private void stopMonitorModeService() {
        Intent stopMonitorModeIntent = new Intent();
        stopMonitorModeIntent.setClass(this, MonitorModeService.class);
        stopService(stopMonitorModeIntent);
    }


    class TeddyBear {
        String name;
        String serial;
        boolean status;
        int time;
        int db;
        boolean service_status;
        String warning_log_info;
    }

    private void saveList() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        if (null != list && list.size() > 0) {
            TeddyBear teddyBear = list.get(0);
            String serial = teddyBear.serial;
            editor.putString(teddyBear.serial, teddyBear.time + ":" + teddyBear.db + ":" + teddyBear.status);
            editor.putString(teddyBear.serial + "_name", teddyBear.name);
            for (int i = 1; i < list.size(); i++) {
                TeddyBear tempBear = list.get(i);
                editor.putString(tempBear.serial, tempBear.time + ":"
                        + tempBear.db + ":" + tempBear.status);
                editor.putString(tempBear.serial + "_name", tempBear.name);

                serial = serial + ":" + tempBear.serial;
            }
            editor.putString("serial", serial);
            editor.apply();
            editor.commit();
        }
    }
}
