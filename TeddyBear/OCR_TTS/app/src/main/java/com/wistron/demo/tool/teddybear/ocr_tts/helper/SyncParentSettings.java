package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.SettingsFragment;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsSettingsFragment;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

/**
 * Created by king on 16-4-18.
 * sync parent settings by SMB protocol.
 */
public class SyncParentSettings {
    private final int MSG_SYNC_ERROR = 0;
    private final int MSG_SYNC_SUCCESS = 1;

    private final String mSharedPath = "TeddyBear/%1$s/BookReaderSettings/settings.txt";

    private Context context;
    private OnSyncFinishListener mOnSyncFinishListener;

    public SyncParentSettings(Context context) {
        this.context = context;
    }

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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String ipAddress = preferences.getString(SettingsFragment.KEY_SERVER_IP_ADDRESS, context.getString(R.string.preference_server_ip_address_default_value));
        String domain = preferences.getString(SettingsFragment.KEY_SERVER_DOMAIN, context.getString(R.string.preference_server_domain_default_value));
        String userName = preferences.getString(SettingsFragment.KEY_SERVER_USER_NAME, context.getString(R.string.preference_server_username_default_value));
        String password = preferences.getString(SettingsFragment.KEY_SERVER_PASSWORD, context.getString(R.string.preference_server_password_default_value));
        Log.i("King", "server address = " + ipAddress + ", domain = " + domain + ", userName = " + userName + ", password = " + password);
        if (TextUtils.isEmpty(domain)) {
            domain = null;
        }

        ConnectTask mTask = new ConnectTask();
        mTask.execute(ipAddress, domain, userName, password);
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
        sharedPreferencesEditor.apply();
        sharedPreferencesEditor.commit();
    }

    private class ConnectTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            // TODO Auto-generated method stub
            try {
                String sn = Build.SERIAL;
                Log.i("King", "sn = " + sn);
                if (TextUtils.isEmpty(sn)) {
                    Log.i("King", "sync error = can't get serial number");

                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "can't get serial number";
                    mMainHandler.sendMessage(msg);
                } else {
                    NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(params[1], params[2], params[3]);
                    SmbFile mFile = new SmbFile(String.format("smb://%1$s/%2$s", params[0], String.format(mSharedPath, sn)), authentication);
                    Log.i("King", "mFile = " + mFile.getCanonicalPath() + ", isFile = " + mFile.isFile() + ", canWrite = " + mFile.canWrite());

                    if (mFile != null) {
                        SmbFileInputStream inputStream = new SmbFileInputStream(mFile);
                        Properties properties = new Properties();
                        properties.load(inputStream);
                        Log.i("King", "sync result = " + properties);
                        updateLocalSettings(properties);

                        mMainHandler.sendEmptyMessage(MSG_SYNC_SUCCESS);
                    }
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                Log.i("King", "sync error = " + e.getMessage());

                Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                msg.obj = e.getMessage();
                mMainHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("King", "sync error = " + e.getMessage());

                Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                msg.obj = e.getMessage();
                mMainHandler.sendMessage(msg);
            }
            return 0;
        }
    }
}
