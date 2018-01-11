package com.wistron.demo.tool.teddybear.parent_side;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.ToSpeak;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Time：16-4-22 13:39
 * Author：bob
 */
public class MonitorModeService extends Service {
    private Timer timer, delayTimer = null;
    private TimerTask timerTask, delayTask;
    private String TAG = "Parent_MonitorMode";

    public static final String ACTION = "com.wistron.demo.teddybear.monitormode.parent.action";
    public static final String ACTION_FLAG = "com.wistron.demo.teddybear.monitormode.parent.action_flag";
    public static final String ACTION_BERLIN_DEL_NEWC = "com.wistron.demo.teddybear.monitormode.parent.delete_newC";


    private boolean isRegisterMonitorModeReceiver = false;

    private final String mSharedFolder = "/TeddyBear/MonitorMode";
    private final String mSharedWarningPath = mSharedFolder + "/warning.txt";

    private boolean isInterruptPlay = false;
    private int mDB = 50;
    private int mInterruptMonitorTime = 30;
    private String ipAddress;
    private String domain;
    private String userName;
    private String password;
    private String address;
    private String port;

    private final int CONNECT_TYPE_SMB_NO_DOMAIN = 0;
    private final int CONNECT_TYPE_SMB_DOMAIN = 1;
    private final int CONNECT_TYPE_FTP = 2;

    private int mCurConntectType = CONNECT_TYPE_FTP;

    private final int ACTION_DOWNLOAD_WARNING = 0;
    private final int ACTION_DELTET_WARNING = 1;
    private final int ACTION_BERLIN_DETECT_FILE = 2;

    private int mCurWarningAction = ACTION_DOWNLOAD_WARNING;

    private boolean isUpdatingmList = false, isInterruptReadWarning = false;
    private ToSpeak toSpeak;
    private ArrayList<TeddyBear> mList = new ArrayList<>();

    private BaseTaskManager mTaskManager;
    private File localWarningFile = null;
    private int mDeleteWarningFileTimes = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Parent start monitor service");
        initFTPManager();

        //berlin added
//        Log.v("berlin", "Thread 3 start");
//        DetectKidsThread = new Thread(DetectKidsRun);
//        DetectKidsThread.start();
        //end

        getTheListForOnlyService();
        updateList();
        registerBroadcast();
        registerUpdateChildClientReceiver();
        toSpeak = new ToSpeak(this);


    }

    private void initFTPManager() {
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
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB_SERVICE)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    if (mCurWarningAction == ACTION_DOWNLOAD_WARNING) {
                        InputStream in = null;
                        StringBuffer stringBuffer = new StringBuffer();

                        try {
                            in = new BufferedInputStream(new FileInputStream(localWarningFile));
                            byte[] buffer = new byte[1024];

                            int length = 0;
                            while ((length = in.read(buffer)) != -1) {
                                stringBuffer.append(new String(buffer, 0, length));
                            }
                            Log.i(TAG, "read warning file success");
                            Log.i(TAG, "string buffer content: " + stringBuffer.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.i(TAG, "download warning file success,read file exception: " + e.toString());
                        } finally {
                            if (null != in) {
                                try {
                                    in.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (null != localWarningFile) {
                            localWarningFile.delete();
                        }

                        Message message = new Message();
                        message.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("serial", mCurTeddyBear.serialnumber);
                        bundle.putString("name", mCurTeddyBear.name);
                        bundle.putString("content", "Detect " + mCurTeddyBear.name + "'s " + stringBuffer.toString());
                        message.setData(bundle);
                        handler.sendMessage(message);
                        mCurTeddyBear.interrupt = true;
                        mCurTeddyBear.start_interrupt_time = System.currentTimeMillis();

                        mCurWarningAction = ACTION_DELTET_WARNING;
                        //delete warning file from ftp
                        mDeleteWarningFileTimes++;
                        Log.i(TAG, "download warning success and start delete warning file from ftp");
                        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                                BaseTaskManager.REQUEST_ACTION_DELONE,
                                String.format("%1$s/MonitorMode/", mCurTeddyBear.serialnumber), "warning.txt");

                    } else if (mCurWarningAction == ACTION_DELTET_WARNING) {
                        //berlin added
                        berlinDetectFileExists();
                    } else if (mCurWarningAction == ACTION_BERLIN_DETECT_FILE) {
                        gotMessageToNotify();
                    }
                    //end
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    if (mCurWarningAction == ACTION_DOWNLOAD_WARNING) {
                        Log.i(TAG, "read ftp warning file, can not connect ftp");
                        handler.sendEmptyMessage(3);
                    } else if (mCurWarningAction == ACTION_DELTET_WARNING) {
                        if (mDeleteWarningFileTimes >= 2) {
                            Log.i(TAG, "delete warning file, can not connect ftp. times: " + mDeleteWarningFileTimes);
                            handler.sendEmptyMessage(3);
                        } else {
                            Log.i(TAG, "can not connect ftp during delete warning and re-delete");
                            mDeleteWarningFileTimes++;
                            mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                                    BaseTaskManager.REQUEST_ACTION_DELONE,
                                    String.format("%1$s/MonitorMode/", mCurTeddyBear.serialnumber), "warning.txt");
                        }
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD) {
                    if (mCurWarningAction == ACTION_DOWNLOAD_WARNING) {
                        Log.i(TAG, "download warning file failed");
                        handler.sendEmptyMessage(3);
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DELONE) {
                    if (mDeleteWarningFileTimes >= 2) {
                        Log.i(TAG, "delete warning file failed. times: " + mDeleteWarningFileTimes);
                        handler.sendEmptyMessage(3);
                    } else {
                        Log.i(TAG, "delete warning failed and re-delete");
                        mDeleteWarningFileTimes++;
                        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                                BaseTaskManager.REQUEST_ACTION_DELONE,
                                String.format("%1$s/MonitorMode/", mCurTeddyBear.serialnumber), "warning.txt");
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS_DELONE) {
                    Log.i(tag, "delete warning file success");
                    handler.sendEmptyMessage(3);
                }

            }
        }
    };

    boolean isWaitNotifyAction = true;
    private boolean isThreadDetecting = true;
    private boolean isNoticeClicked = false;

    private void berlinDetectFileExists() {
        mCurWarningAction = ACTION_BERLIN_DETECT_FILE;
        if (isNoticeClicked) {
            Log.v("berlin", "to delete newC.txt");
            mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                    BaseTaskManager.REQUEST_ACTION_DELONE,
                    CommonHelper.REMOTE_FOLDER_COMMON,
                    "newC.txt");
            isNoticeClicked = false;
            return;
        }

        Log.v("berlin", "detecting...Action=" + mCurWarningAction);
        File notifyParentTxt = new File(String.valueOf(getApplicationContext().getFilesDir()) + "/newC.txt");
        if (!notifyParentTxt.exists()) {
            try {
                notifyParentTxt.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                CommonHelper.REMOTE_FOLDER_COMMON,
                "newC.txt",
                String.valueOf(notifyParentTxt)
        );
    }


    //berlin added.
    private Thread DetectKidsThread;

    private Runnable DetectKidsRun = new Runnable() {
        @Override
        public void run() {

            isThreadDetecting = true;
            while (isThreadDetecting) {

                Log.v("berlin", "detecting..newC.txt.");
                berlinDetectFileExists();
                try {
                    Thread.sleep(55 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.v("berlin", e.toString());
                    Log.v("berlin", "interupted DetectKidsThread.");
                }
            }
            Log.v("berlin", "interupted while.");
        }

    };

    private void gotMessageToNotify() {
        String aa = getChildrenNumber();
        aa = aa.replaceAll("\\n", "");
        String[] sn_list = aa.split("0x567:");
        StringBuilder nameOfKids = new StringBuilder("");
        Log.v("berlin", "sn list length = " + sn_list.length);
        for (int j = 1; j < sn_list.length; j++) {
            Log.v("berlin", "sn 1 =" + sn_list[j]);
            Log.v("berlin", "mList size  = " + mList.size());
            for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                TeddyBear teddyBear = mList.get(i);
                Log.v("berin", "sn 2 =" + teddyBear.serialnumber);
                Log.v("berin", "name = " + teddyBear.name);
                if (teddyBear.serialnumber.equals(sn_list[j])) {
                    nameOfKids.append(teddyBear.name);
                    nameOfKids.append(".");
                    Log.i("berlin", "nameOfKids=" + nameOfKids);
                }
            }
        }
        if (0 < nameOfKids.length()) {
            notification("You got a message from " + nameOfKids);
            Log.v("berlin", "notification was run");
        }
    }

    private String getChildrenNumber() {
        String message_from_file = "";
        String path_messageTxt = getFilesDir() + "/newC.txt";
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(path_messageTxt);
            int file_message_length = fileInputStream.available();
            byte[] buffer = new byte[file_message_length];
            while (-1 < fileInputStream.read(buffer)) {
                message_from_file = new String(buffer, "UTF-8");
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message_from_file;
    }
    //end

    private void startMonitor(TeddyBear teddyBear) {

        initTimeAndDBData(teddyBear);
        if (timer == null) {
            initTimer();
        }

        notification(teddyBear.name + " start monitor mode");
        saveTheListForOnlyService();
    }

    private void initTimeAndDBData(TeddyBear teddyBear) {
        SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());

        String content = sharedPreferences.getString(teddyBear.serialnumber, null);
        if (null == content || !content.contains(":")) {
            teddyBear.time = mInterruptMonitorTime;
            teddyBear.db = mDB;
        } else {
            String[] strs = content.split(":");
            teddyBear.time = Integer.parseInt(strs[0].trim());
            teddyBear.db = Integer.parseInt(strs[1].trim());
        }

    }

    private void initTimer() {
        //berlin added
        isThreadDetecting = false;
        if (null != DetectKidsThread)
            DetectKidsThread.interrupt();
        //end

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (!isInterruptPlay) {
//                    handler.obtainMessage(0).sendToTarget();
                    handler.obtainMessage(3).sendToTarget();
                }
            }
        };
        timer.schedule(timerTask, 1000, 20 * 1000);
    }

    private void cancelTimer() {
        Log.i(TAG, "cancel timer");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        Log.v("berlin", "Thread 2 start");
        DetectKidsThread = new Thread(DetectKidsRun);
        DetectKidsThread.start();
    }

    int mCurVisitWarnNumbers = 0;
    TeddyBear mCurTeddyBear = null;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    /*isInterruptPlay = true;

                    SMBVisitTask smbVisitTask = new SMBVisitTask();
                    smbVisitTask.execute("");

                    boolean isCancelTimer = true;
                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        if (teddyBear.status) {
                            isCancelTimer = false;
                            break;
                        }
                    }
                    if (isCancelTimer) {
                        cancelTimer();
                    }*/
                    break;
                case 1:
                    Bundle bundle = msg.getData();
                    String name = bundle.getString("name", "");
                    String content = bundle.getString("content", "");
                    String serial = bundle.getString("serial", "");

                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        if (teddyBear.name.equals(name)) {
                            Log.i(TAG, "name: " + name + ",the warning file content: " + content);
                            //play the warning and delay detect
                            toSpeak.toSpeak(content);

                            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();

                            saveTheWarningLog(serial);

                            notification(content);
                        }
                    }
                    saveTheListForOnlyService();
                    if (delayTimer == null) {
                        Log.i(TAG, "start init delay timer");
                        initDelayTimer();
                    }
                    break;
                case 2:
                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        if (teddyBear.start_interrupt_time != 0) {
                            long cur_time = System.currentTimeMillis();
                            long time = cur_time - teddyBear.start_interrupt_time;
                            Log.i(TAG, "interrupt time: " + teddyBear.time);
                            Log.i(TAG, "start interrupt time: " + teddyBear.start_interrupt_time);
                            if ((time / (60 * 1000)) >= teddyBear.time) {
                                teddyBear.interrupt = false;
                                teddyBear.start_interrupt_time = 0;
                                saveTheListForOnlyService();
                            }
                        }
                    }

                    boolean isAllChildStop = true;
                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        if (teddyBear.status) {
                            isAllChildStop = false;
                            break;
                        }
                    }
                    if (isAllChildStop) {
                        cancelDelayTimer();
                    }
                    break;
                case 3:
                    Log.i(TAG, "Start listener");
                    isInterruptPlay = true;
                    if (mCurVisitWarnNumbers < mList.size() && !isUpdatingmList && !isInterruptReadWarning) {
                        mCurTeddyBear = mList.get(mCurVisitWarnNumbers);
                        mCurVisitWarnNumbers++;
                        if (mCurTeddyBear.interrupt) {
                            handler.sendEmptyMessage(3);
                        } else {
                            Log.i(TAG, "mCurTeddyBear= " + mCurTeddyBear.serialnumber + ", mCurVisitWarnNumbers= " + mCurVisitWarnNumbers);
                            try {
                                if (null == localWarningFile) {
                                    localWarningFile = File.createTempFile("warning_" + mCurTeddyBear.serialnumber
                                            , ".txt",
                                            getApplicationContext().getFilesDir());
                                }
                                mCurWarningAction = ACTION_DOWNLOAD_WARNING;
                                mDeleteWarningFileTimes = 0;
                                Log.i(TAG, "download local file: " + localWarningFile.getAbsolutePath());
                                mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                                        BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                                        String.format("%1$s/MonitorMode/", mCurTeddyBear.serialnumber),
                                        "warning.txt", localWarningFile.getAbsolutePath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        mCurVisitWarnNumbers = 0;
//                        mCurTeddyBear = null;
                        isInterruptPlay = false;

                        boolean isStopTimer = true;
                        for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                            TeddyBear teddyBear = mList.get(i);
                            if (teddyBear.status) {
                                isStopTimer = false;
                                break;
                            }
                        }
                        if (isStopTimer) {
                            cancelTimer();
                        } else {
                            berlinDetectFileExists();
                        }
                    }
                    break;
            }
            return false;
        }
    });

    private void saveTheWarningLog(String serial) {
        SharedPreferences sharedPreferences = getSharedPreferences("warning_info_log", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(serial, System.currentTimeMillis());
        editor.apply();
        editor.commit();

        //need send broadcast
        Intent intent = new Intent();
        intent.setAction(MonitorModeActivity.UPDATE_WARNING_UI_ACTION);
        sendBroadcast(intent);
    }

    private void initDelayTimer() {
        delayTimer = new Timer();
        delayTask = new TimerTask() {
            @Override
            public void run() {
                handler.obtainMessage(2).sendToTarget();
            }
        };
        delayTimer.schedule(delayTask, 60 * 1000, 60 * 1000);
    }

    private void cancelDelayTimer() {
        if (delayTimer != null) {
            delayTimer.cancel();
            delayTimer = null;
        }
        if (delayTask != null) {
            delayTask.cancel();
            delayTask = null;
        }
    }

    private void stopMonitor(TeddyBear teddyBear) {
        notification(teddyBear.name + " stop monitor mode");

        teddyBear.status = false;
        teddyBear.interrupt = false;
        teddyBear.start_interrupt_time = 0;
        saveTheListForOnlyService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service on destroy");

        isWaitNotifyAction = false;

        if (isRegisterMonitorModeReceiver) {
            unregisterReceiver(MonitorModeReceiver);
        }

        if (isRegisterChildClient) {
            unregisterReceiver(updateChildClinetReceiver);
        }

        mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);


        String serial = "";
        if (mList.size() > 0) {
            serial = mList.get(0).serialnumber;
            for (int i = 1; i < mList.size() && !isUpdatingmList; i++) {
                serial = serial + ":" + mList.get(i).serialnumber;
            }
        }
        SharedPreferences sharedPreferences = getSharedPreferences("user", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("serial", serial);
        if (mList.size() > 0) {
            for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                TeddyBear teddyBear = mList.get(i);
                editor.putString(teddyBear.serialnumber,
                        teddyBear.name + ":" +
                                false + ":" +
                                false + ":" +
                                teddyBear.time + ":" +
                                teddyBear.db + ":" +
                                0);
            }
        }

        editor.apply();
        editor.commit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /*private class SMBVisitTask extends AsyncTask<String, String, String> {

        private int type = 1;

        public SMBVisitTask() {
//            initIpAddress();
        }

        @Override
        protected String doInBackground(String... params) {
            if (type == 1) {
                for (int i = 0; i < mList.size() && !isUpdatingmList && !isInterruptReadWarning; i++) {
                    TeddyBear teddyBear = mList.get(i);
                    if (teddyBear.status && !teddyBear.interrupt) {
                        int number = 2;
                        while (number > 0 && !isInterruptReadWarning) {
                            if (smbGet(number, teddyBear)) {
                                break;
                            }
                            number--;
                        }
                    }
                }
            }
            return null;
        }

        private boolean smbGet(int number, TeddyBear teddyBear) {
            boolean result = false;
            InputStream in = null;
            SmbFile remoteFile = null;
            try {
                StringBuffer stringBuffer = new StringBuffer();
                if (mCurConntectType == CONNECT_TYPE_SMB_DOMAIN) {
                    NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, userName, password);
                    String folderPath = "smb://" + ipAddress + mSharedFolder + "/" + teddyBear.serialnumber;
                    Log.i(TAG, "SMB GET folder path: " + folderPath);
                    SmbFile sharedFolder = new SmbFile(folderPath, authentication);
                    if (sharedFolder == null || !sharedFolder.exists()) {
                        return true;
                    }

                    remoteFile = new SmbFile(folderPath + "/warning.txt", authentication);
                    if (remoteFile == null || !remoteFile.exists()) {
                        return true;
                    }
                    in = new BufferedInputStream(new SmbFileInputStream(remoteFile));
                    byte[] buffer = new byte[1024];

                    int length = 0;
                    while ((length = in.read(buffer)) != -1) {
                        stringBuffer.append(new String(buffer, 0, length));
                    }
                } else if (mCurConntectType == CONNECT_TYPE_SMB_NO_DOMAIN) {
                    String folderPath = "smb://" + userName + ":" + password + "@" +
                            ipAddress + mSharedFolder + "/" + teddyBear.serialnumber;
                    Log.i(TAG, "SMB GET folder path: " + folderPath);
                    SmbFile sharedFolder = new SmbFile(folderPath);
                    if (sharedFolder == null || !sharedFolder.exists()) {
                        return true;
                    }

                    remoteFile = new SmbFile(folderPath + "/warning.txt");
                    if (remoteFile == null || !remoteFile.exists()) {
                        return true;
                    }
                    in = new BufferedInputStream(new SmbFileInputStream(remoteFile));
                    byte[] buffer = new byte[1024];

                    int length = 0;
                    while ((length = in.read(buffer)) != -1) {
                        stringBuffer.append(new String(buffer, 0, length));
                    }
                } else if (mCurConntectType == CONNECT_TYPE_FTP) {
                    FTPHelper ftpHelper = new FTPHelper(address, Integer.parseInt(port), userName, password);
                    boolean isConnected = ftpHelper.connect();
                    if (!isConnected) {
                        Log.i(TAG, "read ftp warning file, can not connect ftp");
                        return false;
                    } else {
                        File localFile = File.createTempFile("monitor_mode_warning", ".txt", getApplicationContext().getFilesDir());
                        boolean pass = ftpHelper.downloadFile(String.format("%1$s/MonitorMode/", teddyBear.serialnumber), "warning.txt", localFile);
                        if (pass) {
                            in = new BufferedInputStream(new FileInputStream(localFile));
                            byte[] buffer = new byte[1024];

                            int length = 0;
                            while ((length = in.read(buffer)) != -1) {
                                stringBuffer.append(new String(buffer, 0, length));
                            }
                            Log.i(TAG, "read warning file success");
                        } else {
                            Log.i(TAG, "read warning file failed");
                            return false;
                        }
                        localFile.delete();
                    }
                    ftpHelper.disconnect();
                }
                Log.i(TAG, "Read warning file success"
                        + "\n" + stringBuffer.toString());

                Message message = new Message();
                message.what = 1;
                Bundle bundle = new Bundle();
                bundle.putString("name", teddyBear.name);
                bundle.putString("content", "Detect " + teddyBear.name + "'s " + stringBuffer.toString());
                message.setData(bundle);
                handler.sendMessage(message);
                result = true;
                teddyBear.interrupt = true;
                teddyBear.start_interrupt_time = System.currentTimeMillis();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, e.toString());
                return false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if ((result || number == 1) && mCurConntectType != CONNECT_TYPE_FTP) {
                    try {
                        remoteFile.delete();
                    } catch (SmbException e) {
                        e.printStackTrace();
                    }
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (type == 1) {
                isInterruptPlay = false;
            } else if (type == 2) {

            }
        }
    }*/

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);
        filter.addAction(ACTION_FLAG);
        filter.addAction(ACTION_BERLIN_DEL_NEWC);
        registerReceiver(MonitorModeReceiver, filter);
        isRegisterMonitorModeReceiver = true;
    }

    private BroadcastReceiver MonitorModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION)) {
                String action = intent.getStringExtra("action");
                String serial = intent.getStringExtra("serial");
                Log.i(TAG, "Receive broadcast,action: " + action + ", serial: " + serial + ", mList size: " + mList.size());
                if (action.equals("start")) {
                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        Log.i(TAG, "serial: " + teddyBear.serialnumber + ", status: " + teddyBear.status);
                        if (teddyBear.serialnumber.equals(serial) && !teddyBear.status) {
                            teddyBear.status = true;
                            startMonitor(teddyBear);
                            break;
                        }
                    }
                } else if (action.equals("stop")) {
                    for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                        TeddyBear teddyBear = mList.get(i);
                        Log.i(TAG, "serial: " + teddyBear.serialnumber + ", status: " + teddyBear.status);
                        if (teddyBear.serialnumber.equals(serial) && teddyBear.status) {
                            stopMonitor(teddyBear);
                            break;
                        }
                    }
                }
            } else if (intent.getAction().equals(ACTION_FLAG)) {

                boolean isInterrup = intent.getBooleanExtra("interrupt", false);
                Log.i(TAG, "Receive broadcast,action: " + intent.getAction() + "\n" + "isInterrupt: " + isInterrup);
                if (isInterrup) {
                    isInterruptReadWarning = true;
                } else {
                    isInterruptReadWarning = false;
                }
            }
            if (intent.getAction().equals(ACTION_BERLIN_DEL_NEWC)) {
                Log.v("berlin", "get action to delete newC.txt");
                isNoticeClicked = true;
            }
        }
    };

    private void notification(String content) {

        //berlin added
        Intent clickIntent = new Intent();
        clickIntent.setAction(ACTION_BERLIN_DEL_NEWC);
        Log.v("berlin", "click intent ....");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                1,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        //end

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.monitor_mode_warning)
                .setWhen(System.currentTimeMillis())
                .setTicker("Teddy Bear Notification information")
                .setContentTitle("Monitor Mode")
                //berlin added next a line
                .setContentIntent(pendingIntent)
                .setContentText(content).build();
        notificationManager.notify(R.drawable.children_head_image, notification);


    }

    private void registerUpdateChildClientReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(CHILD_ACTION);
        registerReceiver(updateChildClinetReceiver, filter);
        isRegisterChildClient = true;
    }

    public static final String CHILD_ACTION = "com.wistron.demo.teddybear.parent.user.info";
    private boolean isRegisterChildClient = false;
    private BroadcastReceiver updateChildClinetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CHILD_ACTION)) {
                isUpdatingmList = true;
                updateList();
            }
        }
    };

    private void updateList() {
        //update list
        Properties properties = new Properties();
        File file = new File(getFilesDir(), ChildrenManagementActivity.CHILDREN_LIST_FILE_NAME);
        if (file.exists()) {
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
        }

        if (properties.size() > 0) {

            Iterator<Map.Entry<Object, Object>> it = properties.entrySet().iterator();

            ArrayList<String> tempList = new ArrayList<>();
            while (it.hasNext()) {
                Map.Entry<Object, Object> entry = it.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                tempList.add(String.valueOf(key));

                TeddyBear tempTeddyBear = new TeddyBear();
                tempTeddyBear.serialnumber = String.valueOf(key).trim();
                tempTeddyBear.start_interrupt_time = 0;
                tempTeddyBear.name = String.valueOf(value).trim();
                tempTeddyBear.time = mInterruptMonitorTime;
                tempTeddyBear.db = mDB;
                tempTeddyBear.interrupt = false;
                tempTeddyBear.status = false;

                Log.i(TAG, "update list， serial number: " + tempTeddyBear.serialnumber);

                if (mList.size() <= 0) {
                    mList.add(tempTeddyBear);
                } else {
                    boolean isExist = false;
                    for (TeddyBear temp : mList) {
                        if (temp.serialnumber.equals(tempTeddyBear.serialnumber)) {
                            isExist = true;
                            break;
                        }
                    }
                    if (!isExist) {
                        mList.add(tempTeddyBear);
                    }
                }
            }

            for (int i = 0; i < mList.size(); i++) {
                TeddyBear teddyBear = mList.get(i);
                boolean isRemove = false;
                for (int j = 0; j < tempList.size(); j++) {
                    if (tempList.get(j).equals(teddyBear.serialnumber)) {
                        isRemove = true;
                        break;
                    }
                }
                if (!isRemove) {
                    mList.remove(i);
                }
            }
        } else if (properties.size() == 0) {
            if (mList.size() > 0) {
                mList.clear();
            }
        }

        saveTheListForOnlyService();


        for (int i = 0; i < mList.size(); i++) {
            Log.i(TAG, "serial : " + mList.get(i).serialnumber);
        }
        isUpdatingmList = false;
    }

    private void saveList() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        if (null != mList && mList.size() > 0) {
            TeddyBear teddyBear = mList.get(0);
            String serial = teddyBear.serialnumber;
            editor.putString(teddyBear.serialnumber, teddyBear.time + ":" + teddyBear.db + ":" + teddyBear.status);
            editor.putString(teddyBear.serialnumber + "_name", teddyBear.name);
            for (int i = 1; i < mList.size(); i++) {
                TeddyBear tempBear = mList.get(i);
                editor.putString(tempBear.serialnumber, tempBear.time + ":"
                        + tempBear.db + ":" + tempBear.status);
                editor.putString(tempBear.serialnumber + "_name", tempBear.name);

                serial = serial + ":" + tempBear.serialnumber;
            }
            editor.putString("serial", serial);
            editor.apply();
            editor.commit();

            saveTheListForOnlyService();
        }

    }

    private void saveTheListForOnlyService() {
        String serial = "";
        if (mList.size() > 0) {
            serial = mList.get(0).serialnumber;
            for (int i = 1; i < mList.size() && !isUpdatingmList; i++) {
                serial = serial + ":" + mList.get(i).serialnumber;
            }
        }
        SharedPreferences sharedPreferences = getSharedPreferences("user", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("serial", serial);
        if (mList.size() > 0) {
            for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                TeddyBear teddyBear = mList.get(i);
                editor.putString(teddyBear.serialnumber,
                        teddyBear.name + ":" +
                                teddyBear.interrupt + ":" +
                                teddyBear.status + ":" +
                                teddyBear.time + ":" +
                                teddyBear.db + ":" +
                                teddyBear.start_interrupt_time);
            }
        }

        editor.apply();
        editor.commit();
    }

    private void getTheListForOnlyService() {
        SharedPreferences sharedPreferences = getSharedPreferences("user", Activity.MODE_PRIVATE);
        String serial = sharedPreferences.getString("serial", null);
        Log.i(TAG, "get list for service: " + serial);
        if (null != serial) {
            if (serial.contains(":")) {
                String[] serials = serial.split(":");
                for (String tempSerial : serials) {
                    String content = sharedPreferences.getString(tempSerial, null);
                    Log.i(TAG, "content: " + content);
                    if (null != content && content.contains(":")) {
                        String[] strs = content.split(":");
                        TeddyBear teddyBear = new TeddyBear();
                        teddyBear.name = strs[0].trim();
                        teddyBear.serialnumber = tempSerial.trim();
                        teddyBear.interrupt = Boolean.parseBoolean(strs[1]);
                        teddyBear.status = Boolean.parseBoolean(strs[2]);
                        teddyBear.time = Integer.parseInt(strs[3]);
                        teddyBear.db = Integer.parseInt(strs[4]);
                        teddyBear.start_interrupt_time = Long.parseLong(strs[5]);
                        mList.add(teddyBear);
                    }
                }
            } else {
                String content = sharedPreferences.getString(serial, null);
                Log.i(TAG, "content: " + content);
                if (null != content && content.contains(":")) {
                    String[] strs = content.split(":");
                    TeddyBear teddyBear = new TeddyBear();
                    teddyBear.name = strs[0].trim();
                    teddyBear.serialnumber = serial.trim();
                    teddyBear.interrupt = Boolean.parseBoolean(strs[1]);
                    teddyBear.status = Boolean.parseBoolean(strs[2]);
                    teddyBear.time = Integer.parseInt(strs[3]);
                    teddyBear.db = Integer.parseInt(strs[4]);
                    teddyBear.start_interrupt_time = Long.parseLong(strs[5]);
                    mList.add(teddyBear);
                }
            }

            if (mList.size() > 0) {
                for (int i = 0; i < mList.size() && !isUpdatingmList; i++) {
                    TeddyBear teddyBear = mList.get(i);
                    if (teddyBear.status && timer == null) {
                        initTimer();
                    } else {
                        Log.v("berlin", "Thread 3 start");
                        DetectKidsThread = new Thread(DetectKidsRun);
                        DetectKidsThread.start();
                    }
                    if (teddyBear.interrupt && delayTimer == null) {
                        initDelayTimer();
                    }
                }
            } else {
                Log.v("berlin", "Thread 4 start");
                DetectKidsThread = new Thread(DetectKidsRun);
                DetectKidsThread.start();
            }
        }
    }

    static class TeddyBear {
        String name;
        String serialnumber;
        boolean interrupt;
        boolean status;
        int time;
        int db;
        long start_interrupt_time;
    }
}
