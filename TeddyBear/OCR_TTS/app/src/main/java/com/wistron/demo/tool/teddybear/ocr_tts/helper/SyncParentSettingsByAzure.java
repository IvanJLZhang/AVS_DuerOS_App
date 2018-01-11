package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsSettingsFragment;
import com.wistron.demo.tool.teddybear.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by king on 16-4-18.
 * sync parent settings by FTP protocol.
 */
public class SyncParentSettingsByAzure {
    private final int MSG_SYNC_ERROR = 0;
    private final int MSG_SYNC_SUCCESS = 1;

    //private final String mSharedPath = "TeddyBear/%1$s/BookReaderSettings/settings.txt";
    private String mRemoteFolder = "%1$s/BookReaderSettings/";
    private String mRemoteFileName = "settings.txt";
    private File mLocalFile;

    private Context context;
    //private ConnectTask mTask;
    private OnSyncFinishListener mOnSyncFinishListener;

    private BaseTaskManager mStorageTaskManager;

    public SyncParentSettingsByAzure(Context context) {
        this.context = context;
        if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_AZURE) {
            mStorageTaskManager = AzureStorageTaskManager.getInstance(context);
        } else if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_QINIU) {
            mStorageTaskManager = QiniuStorageTaskManager.getInstance(context);
        }
        mStorageTaskManager.addAzureStorageChangedListener(mRequestResultChangedListener);
    }

    private AzureStorageTaskManager.OnRequestResultChangedListener mRequestResultChangedListener = new AzureStorageTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_KING)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    try {
                        Properties properties = new Properties();
                        properties.load(new FileInputStream(mLocalFile));
                        Log.i("King", "sync result = " + properties);

                        updateLocalSettings(properties);

                        mMainHandler.sendEmptyMessage(MSG_SYNC_SUCCESS);
                        Log.i("King", "download success");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + context.getString(R.string.msg_cant_connect_ftp_server);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", context.getString(R.string.msg_cant_connect_ftp_server));
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD) {
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + context.getString(R.string.msg_ftp_download_fail);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", "sync failed");
                }
                if (mLocalFile != null) {
                    mLocalFile.delete();
                    mLocalFile = null;
                }
            }
        }
    };

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SYNC_ERROR:
                    if (mOnSyncFinishListener != null) {
                        mOnSyncFinishListener.onSyncParentFail(msg.obj.toString());
                    }
                    break;
                case MSG_SYNC_SUCCESS:
                    if (mOnSyncFinishListener != null) {
                        mOnSyncFinishListener.onSyncParentSuccess();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    public void startSync() {
        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String address = preferences.getString(SettingsFragment.KEY_FTP_SERVER_ADDRESS, context.getString(R.string.preference_ftp_server_ip_address_default_value));
        final String port = preferences.getString(SettingsFragment.KEY_FTP_SERVER_PORT, context.getString(R.string.preference_ftp_server_port_default_value));
        final String userName = preferences.getString(SettingsFragment.KEY_FTP_SERVER_USER_NAME, context.getString(R.string.preference_ftp_server_username_default_value));
        final String password = preferences.getString(SettingsFragment.KEY_FTP_SERVER_PASSWORD, context.getString(R.string.preference_ftp_server_password_default_value));
        Log.i("King", "FTP server address = " + address + ", port = " + port + ", userName = " + userName + ", password = " + password);*/

        /*mTask = new ConnectTask();
        mTask.execute(address, port, userName, password);*/

        String sn = Build.SERIAL;
        Log.i("King", "sn = " + sn);
        if (TextUtils.isEmpty(sn)) {
            Log.i("King", "sync error = can't get serial number");

            Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
            msg.obj = "can't get serial number";
            mMainHandler.sendMessage(msg);
        } else {
            try {
                String remoteFolder = String.format(mRemoteFolder, sn);
                if (mLocalFile == null) {
                    mLocalFile = File.createTempFile("ocr_tts_", ".txt", context.getFilesDir());
                }
                mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_KING, BaseTaskManager.REQUEST_ACTION_DOWNLOAD, remoteFolder, mRemoteFileName, mLocalFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("King", "sync error = " + e.getMessage());

                Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                msg.obj = e.getMessage();
                mMainHandler.sendMessage(msg);
            }
        }
    }

    public void stop() {
        mStorageTaskManager.removeAzureStorageChangedListener(mRequestResultChangedListener);
    }

    public void setOnSyncFinishListener(@Nullable OnSyncFinishListener l) {
        mOnSyncFinishListener = l;
    }

    public interface OnSyncFinishListener {
        void onSyncParentSuccess();

        void onSyncParentFail(String error);
    }

    private void updateLocalSettings(Properties properties) {
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        if (properties.containsKey(OcrTtsSettingsFragment.KEY_TRANSLATE_ENABLER)) {
            sharedPreferencesEditor.putBoolean(OcrTtsSettingsFragment.KEY_TRANSLATE_ENABLER, properties.get(OcrTtsSettingsFragment.KEY_TRANSLATE_ENABLER).equals("true"));
        }
        if (properties.containsKey(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE)) {
            sharedPreferencesEditor.putString(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE, (String) properties.get(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE));
        }
        if (properties.containsKey(OcrTtsSettingsFragment.KEY_VOICE_GENDER)) {
            sharedPreferencesEditor.putString(OcrTtsSettingsFragment.KEY_VOICE_GENDER, (String) properties.get(OcrTtsSettingsFragment.KEY_VOICE_GENDER));
        }
        if (properties.containsKey(OcrTtsSettingsFragment.KEY_TRANSLATOR)) {
            sharedPreferencesEditor.putString(OcrTtsSettingsFragment.KEY_TRANSLATOR, (String) properties.get(OcrTtsSettingsFragment.KEY_TRANSLATOR));
        }
        if (properties.containsKey(OcrTtsSettingsFragment.KEY_GOOGLE_VOICE_LANGUAGE)) {
            sharedPreferencesEditor.putString(OcrTtsSettingsFragment.KEY_GOOGLE_VOICE_LANGUAGE, (String) properties.get(OcrTtsSettingsFragment.KEY_GOOGLE_VOICE_LANGUAGE));
        }
        sharedPreferencesEditor.apply();
        sharedPreferencesEditor.commit();
    }
}
