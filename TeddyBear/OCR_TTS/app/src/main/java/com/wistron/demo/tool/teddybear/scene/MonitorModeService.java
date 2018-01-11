package com.wistron.demo.tool.teddybear.scene;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.wistron.demo.tool.teddybear.SettingsFragment;
import com.wistron.demo.tool.teddybear.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Time：16-4-22 13:39
 * Author：bob
 */
public class MonitorModeService extends Service {
    private final int CONNECT_TYPE_SMB_NO_DOMAIN = 0;
    private final int CONNECT_TYPE_SMB_DOMAIN = 1;
    private final int CONNECT_TYPE_FTP = 2;

    private int mCurConntectType = CONNECT_TYPE_FTP;

    private Timer timer, delayTimer;
    private TimerTask timerTask, delayTask;
    private AudioRecordDemo audioRecordDemo;
    private String TAG = "Child_MonitorMode";

    public static final String ACTION = "com.wistron.demo.teddybear.monitormode.action";
    public static final String ACTION_NEW_MESSAGE = "com.wistron.demo.teddybear.monitormode.readingNewMessage";
    private boolean isRegisterMonitorModeReceiver = false;

//    private String mSharedFolder = "/TeddyBear/MonitorMode";
//    private String mSharedWarningPath = mSharedFolder + "/warning.txt";
//    private String mSharedSettingPath = mSharedFolder + "/settings.txt";

    private int mDB = 50;
    private int mInterruptMonitorTime = 30;
    private boolean isPromptParent = false;
    private boolean isMonitorModeStart = false;
    private int mTempDBTimes = 0;
    private double mTempDBValue = 0;
    private boolean isFirstSyncDataToParent = true;

    private String mDeviceSN = "";

    private File localSettingsFile;
    private File localWarningFile;

    private BaseTaskManager mStorageTaskManager;

    private final int ACTION_DOWNLOAD_SETTINGS = 0;
    private final int ACTION_SYNC_WARNING = 1;
    private int mCurAction = ACTION_DOWNLOAD_SETTINGS;

    private final int ACTION_DOWNLOAD_START = 0;
    private final int ACTION_DOWNLOAD_STOP = 1;
    private final int ACTION_BERLIN_DETECT_FILE = 2;
    private int mCurDownloadAction = ACTION_DOWNLOAD_START;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Teddy Bear start monitor service");

        mDeviceSN = Build.SERIAL;
//        mSharedFolder += File.separator + mDeviceSN;
//        mSharedWarningPath = mSharedFolder + "/warning.txt";
//        mSharedSettingPath = mSharedFolder + "/settings.txt";
        Log.i(TAG, "device SN:" + mDeviceSN);
//        Log.i(TAG, "mSharedFolder:" + mSharedFolder);
//        Log.i(TAG, "mSharedWarningPath:" + mSharedWarningPath);
//        Log.i(TAG, "mSharedSettingPath:" + mSharedSettingPath);

        initFTPManager();
        RegisterBroadcast();

        new Thread(new MonitorAction()).start();
    }

    private void initFTPManager() {
        if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_AZURE) {
            mStorageTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_QINIU) {
            mStorageTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mStorageTaskManager.addAzureStorageChangedListener(mResultChangedListener);
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB_SERVICE)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    if (mCurAction == ACTION_DOWNLOAD_SETTINGS) {
                        Properties properties = new Properties();
                        try {
                            properties.load(new FileInputStream(localSettingsFile));
                            Log.i(TAG, "read settings file success");
                            Log.i(TAG, "sync result = " + properties);
                            saveSettings(properties);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (mCurAction == ACTION_SYNC_WARNING) {
                        Log.i(TAG, "upload warning file success");
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    if (mCurAction == ACTION_DOWNLOAD_SETTINGS) {
                        Log.i(TAG, "read ftp settings file, can not connect ftp");
                    } else if (mCurAction == ACTION_SYNC_WARNING) {
                        Log.i(TAG, "pull warning file: can not connect ftp");
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD) {
                    if (mCurAction == ACTION_DOWNLOAD_SETTINGS) {
                        Log.i(TAG, "download settings file failed");
                    } else if (mCurAction == ACTION_SYNC_WARNING) {

                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD) {
                    if (mCurAction == ACTION_SYNC_WARNING) {
                        Log.i(TAG, "upload warning file failed");
                    }
                }
                if (mCurAction == ACTION_DOWNLOAD_SETTINGS) {
                    startRecord();
                }
            } else if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    if (mCurDownloadAction == ACTION_DOWNLOAD_START) {
                        Log.i(TAG, "download start file success");
                        Intent intent = new Intent();
                        intent.setAction(ACTION);
                        intent.putExtra("action", "start");
                        sendBroadcast(intent);
                        isWaitNotifyAction = false;
                        berlinDetectFileExists();
                    } else if (mCurDownloadAction == ACTION_DOWNLOAD_STOP) {
                        Log.i(TAG, "download stop file success");
                        Intent intent = new Intent();
                        intent.setAction(ACTION);
                        intent.putExtra("action", "stop");
                        sendBroadcast(intent);
                        isWaitNotifyAction = false;
                        berlinDetectFileExists();
                    } else if (mCurDownloadAction == ACTION_BERLIN_DETECT_FILE) {
                        Log.v("berlin", "tested===new.txt exists.");
                        Intent intent = new Intent();
                        intent.setAction(SceneActivity.Action_MemoMessage);
                        intent.putExtra("new message", true);
                        sendBroadcast(intent);
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    if (mCurDownloadAction == ACTION_DOWNLOAD_START) {
                        Log.i(TAG, "download start file, can not connect ftp");
                        isWaitNotifyAction = false;
                    } else if (mCurDownloadAction == ACTION_DOWNLOAD_STOP) {
                        Log.i(TAG, "download stop file, can not connect ftp");
                        isWaitNotifyAction = false;
                    }
                    //berlinDetectFileExists();
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD) {
                    if (mCurDownloadAction == ACTION_DOWNLOAD_START) {
                        Log.i(TAG, "download start file failed and start download stop file");
                        try {
                            File stopFile = File.createTempFile("monitor_mode_stop_",
                                    ".txt", getApplicationContext().getFilesDir());
                            mCurDownloadAction = ACTION_DOWNLOAD_STOP;
                            mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                                    BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                                    String.format("%1$s/MonitorMode/", mDeviceSN), "stop.txt",
                                    stopFile.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (mCurDownloadAction == ACTION_DOWNLOAD_STOP) {
                        Log.i(TAG, "download stop file failed");
                        isWaitNotifyAction = false;
                        berlinDetectFileExists();
                    }
                }
            }
        }
    };

    private void berlinDetectFileExists() {
        if (!isReadingNew) {
            mCurDownloadAction = ACTION_BERLIN_DETECT_FILE;
            mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                    BaseTaskManager.REQUEST_ACTION_EXISTFILE, mDeviceSN + "/Memo", "new.txt");
        }
    }

    boolean isWaitNotifyAction = true;
    boolean isCaptureAction = true;
    boolean isReadingNew = false;
    boolean isStopService = true;

    private class MonitorAction implements Runnable {

        @Override
        public void run() {
            while (isStopService) {
                while (isCaptureAction && isStopService) {
                    isWaitNotifyAction = true;
                    getStartOrStopFileFromFTP();
                    while (isWaitNotifyAction) {
                        try {
                            Thread.sleep(30 * 1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void startMonitor() {
        if (isMonitorModeStart) {
            return;
        }

        try {
            mCurAction = ACTION_DOWNLOAD_SETTINGS;
            if (null == localSettingsFile) {
                localSettingsFile = File.createTempFile("monitor_mode_settings_" + mDeviceSN + "_",
                        ".txt", getApplicationContext().getFilesDir());
            }
            mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                    BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                    String.format("%1$s/MonitorMode/", mDeviceSN), "settings.txt",
                    localSettingsFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startRecord() {
        //init data
        if (isFirstSyncDataToParent) {
            isFirstSyncDataToParent = false;
            initTimeAndDBData();
        }

        Log.i(TAG, "Start Monitor");
        audioRecordDemo = new AudioRecordDemo();
        audioRecordDemo.startRecord();
        initTimer();
        notifyBtnUI(true);

        isCaptureAction = true;
        isMonitorModeStart = true;
    }

    private void initTimeAndDBData() {
        SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        mInterruptMonitorTime = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_MONITOR_MODE_TIME, mInterruptMonitorTime + ""));
        mDB = Integer.parseInt(sharedPreferences.getString(SettingsFragment.KEY_MONITOR_MODE_DB, mDB + ""));
        Log.i(TAG, "mInterruptMonitorTime: " + mInterruptMonitorTime + "\nmDB: " + mDB);

    }

    private void initTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.obtainMessage(0).sendToTarget();
            }
        };
        timer.schedule(timerTask, 55 * 1000);
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    stopRecord();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            startRecord();
                            audioRecordDemo = new AudioRecordDemo();
                            audioRecordDemo.startRecord();
                            initTimer();
                        }
                    }, 1000);
                    break;
                case 1:
                    Log.i(TAG, "The DB more than " + mDB + " times "
                            + mTempDBTimes + " in one minutes");
                    if (mTempDBTimes >= 5) {
                        if (isPromptParent) {
                            //do nothing
                        } else {
                            //upload the file to server and
                            //prompt parent
                            Log.i(TAG, "upload file to prompt parent");

                            if (null == localWarningFile) {
                                localWarningFile = getLocalPath("noise is higher than " + mDB + " dB ");
                            }

                            mCurAction = ACTION_SYNC_WARNING;
                            mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB_SERVICE,
                                    BaseTaskManager.REQUEST_ACTION_UPLOAD,
                                    localWarningFile.getAbsolutePath(),
                                    String.format("%1$s/MonitorMode/", mDeviceSN), "warning.txt");

                            isPromptParent = true;
                            initDelayTimer();
                            stopRecord();
                            Log.i(TAG, "Stop record");
                        }
                    }
                    mTempDBTimes = 0;
                    mTempDBValue = 0;
                    break;
                case 2:
                    cancelDelayTimer();
                    Log.i(TAG, "after 30 minutes");
                    isPromptParent = false;
                    startRecord();
                    break;
            }

            return false;
        }
    });

    private void initDelayTimer() {
        delayTimer = new Timer();
        delayTask = new TimerTask() {
            @Override
            public void run() {
                if (isMonitorModeStart) {
                    handler.obtainMessage(2).sendToTarget();
                }
            }
        };
        delayTimer.schedule(delayTask, mInterruptMonitorTime * 60 * 1000);
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

    private void stopMonitor() {
        Log.i(TAG, "Stop Monitor");
        stopRecord();
        cancelDelayTimer();
        isMonitorModeStart = false;
        isPromptParent = false;
        mTempDBTimes = 0;
        mTempDBValue = 0;
        isFirstSyncDataToParent = true;
        notifyBtnUI(false);

        isCaptureAction = true;
    }

    private void notifyBtnUI(boolean action) {
        Intent intent = new Intent();
        intent.setAction(SceneActivity.UI_ACTION);
        intent.putExtra("ui", action);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service on destroy");
        stopMonitor();

        isWaitNotifyAction = false;
        isCaptureAction = false;
        isStopService = false;

        if (isRegisterMonitorModeReceiver) {
            unregisterReceiver(MonitorModeReceiver);
        }
        cancelDelayTimer();

        mStorageTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
    }

    private void stopRecord() {

        cancelTimer();
        if (audioRecordDemo != null) {
            audioRecordDemo.stopRecord();
            audioRecordDemo = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    class AudioRecordDemo {
        private int RECORDER_SAMPLERATE = 16000;
        private int RECORDER_CHANNELS = 1;
        private int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

        private BlockingQueue<byte[]> blockingQueue;
        private AudioRecord recorder = null;
        private int bufferSize = 0;
        private boolean isRecording = false;

        private Thread detectSpeakingThread = null;
        private Thread recordingThread = null;

        public AudioRecordDemo() {
            init();
        }

        private void init() {
            bufferSize = AudioRecord.getMinBufferSize
                    (RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 3;
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE,
                    RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING,
                    bufferSize);

            blockingQueue = new LinkedBlockingDeque<>();
        }

        public void startRecord() {
            int i = recorder.getState();
            if (i == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording();
            }
            isRecording = true;
            detectSpeakingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    startDetectSpeaking();
                }
            }, "Calculate Thread");
            detectSpeakingThread.start();

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
        }

        public void stopRecord() {
            if (null != recorder) {
                isRecording = false;
                int i = recorder.getState();
                if (i == AudioRecord.STATE_INITIALIZED)
                    recorder.stop();
                detectSpeakingThread = null;
                recordingThread = null;
                blockingQueue.clear();
            }
        }

        private void writeAudioDataToFile() {
            byte data[] = new byte[bufferSize];
            int read = 0;
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    // handle empty data
                    try {
                        blockingQueue.put(data);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void startDetectSpeaking() {
            while (isRecording) {
                try {
                    byte[] data = blockingQueue.take();
                    double db = countDb(Bytes2Shorts(data));

                    if (db > mDB) {
                        mTempDBTimes++;
                        Log.i(TAG, "The DB more than " + mDB + " times " + mTempDBTimes + ", DB: " + db);
                        mTempDBValue += db;
                        if (mTempDBTimes == 1) {
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (isMonitorModeStart) {
                                        handler.obtainMessage(1).sendToTarget();
                                    }
                                }
                            }, 60 * 1000);
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public short getShort(byte[] buf, boolean bBigEnding) {
            if (buf == null) {
                throw new IllegalArgumentException("byte array is null!");
            }
            if (buf.length > 2) {
                throw new IllegalArgumentException("byte array size > 2 !");
            }
            short r = 0;
            if (bBigEnding) {
                for (int i = 0; i < buf.length; i++) {
                    r <<= 8;
                    r |= (buf[i] & 0x00ff);
                }
            } else {
                for (int i = buf.length - 1; i >= 0; i--) {
                    r <<= 8;
                    r |= (buf[i] & 0x00ff);
                }
            }
            return r;
        }

        public short[] Bytes2Shorts(byte[] buf) {
            byte bLength = 2;
            short[] s = new short[buf.length / bLength];
            for (int iLoop = 0; iLoop < s.length; iLoop++) {
                byte[] temp = new byte[bLength];
                for (int jLoop = 0; jLoop < bLength; jLoop++) {
                    temp[jLoop] = buf[iLoop * bLength + jLoop];
                }
                s[iLoop] = getShort(temp, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            }
            return s;
        }

        public double countDb(short[] data) {
            float BASE = 32768f;
            float maxAmplitude = 0;
            for (int i = 0; i < data.length; i++) {
                maxAmplitude += data[i] * data[i];
            }
            maxAmplitude = (float) Math.sqrt(maxAmplitude / data.length);
            float ratio = maxAmplitude / BASE;
            float db = 0;
            if (ratio > 0) {
                db = (float) (20 * Math.log10(ratio)) + 100;
            }
            return db;
        }
    }

    private File getLocalPath(String content) {
        File file = new File(MonitorModeService.this.getFilesDir() + "/MonitorMode", "warning.txt");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
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

    private void saveSettings(Properties properties) {
        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();

        if (properties.containsKey(SettingsFragment.KEY_MONITOR_MODE_TIME)) {
            editor.putString(SettingsFragment.KEY_MONITOR_MODE_TIME,
                    (String) properties.get(SettingsFragment.KEY_MONITOR_MODE_TIME));
        }

        if (properties.containsKey(SettingsFragment.KEY_MONITOR_MODE_DB)) {
            editor.putString(SettingsFragment.KEY_MONITOR_MODE_DB,
                    (String) properties.get(SettingsFragment.KEY_MONITOR_MODE_DB));
        }
        editor.apply();
    }

    private void RegisterBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION);
        filter.addAction(ACTION_NEW_MESSAGE);
        registerReceiver(MonitorModeReceiver, filter);
        isRegisterMonitorModeReceiver = true;
    }

    private BroadcastReceiver MonitorModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION)) {
                String action = intent.getStringExtra("action");
                if (action.equals("start") && !isMonitorModeStart) {
                    isCaptureAction = false;
                    startMonitor();
                } else if (action.equals("stop") && isMonitorModeStart) {
                    isCaptureAction = false;
                    stopMonitor();
                }
            }
            if (intent.getAction().equals(ACTION_NEW_MESSAGE)) {
                isReadingNew = intent.getBooleanExtra("isreading", false);
                Log.v("berlin", "isreadingNew=" + isReadingNew);
            }
        }
    };

    private void getStartOrStopFileFromFTP() {
        try {
            File startFile = File.createTempFile("monitor_mode_start_", ".txt",
                    getApplicationContext().getFilesDir());
            mCurDownloadAction = ACTION_DOWNLOAD_START;
            mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                    BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                    String.format("%1$s/MonitorMode/", mDeviceSN), "start.txt",
                    startFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
