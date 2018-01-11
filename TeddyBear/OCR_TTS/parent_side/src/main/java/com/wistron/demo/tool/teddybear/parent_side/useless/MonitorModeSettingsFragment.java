package com.wistron.demo.tool.teddybear.parent_side.useless;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.MonitorModeService;
import com.wistron.demo.tool.teddybear.parent_side.R;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by king on 16-3-10.
 */
public class MonitorModeSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String KEY_MONITOR_MODE_TIME = "monitor_mode_time";
    public static final String KEY_MONITOR_MODE_DB = "monitor_mode_db";
    public static final String KEY_START_MONITOR_MODE_STATUS = "status";

    private EditTextPreference pref_MonitorModeTime;
    private EditTextPreference pref_MonitorModeDB;
    private Button btn_SyncToChild;

    private PackageManager mPackageManager;

    //private String mCurAudioLanguage;

    private final int MSG_SYNC_ERROR = 0;
    private final int MSG_SYNC_SUCCESS = 1;
    private final int MSG_SHOW_WAITING_DIALOG = 2;
    private final String mSharedPath = "TeddyBear/MonitorMode/settings.txt";
    private ProgressDialog waitingDialog;


    private boolean isStartMonitorMode = false;
    private Button btnStartAndStopMonitorMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.monitor_mode_settings_preference);

        pref_MonitorModeTime = (EditTextPreference) findPreference(KEY_MONITOR_MODE_TIME);
        pref_MonitorModeTime.setSummary(pref_MonitorModeTime.getText());
        pref_MonitorModeTime.setOnPreferenceChangeListener(this);

        pref_MonitorModeDB = (EditTextPreference) findPreference(KEY_MONITOR_MODE_DB);
        pref_MonitorModeDB.setSummary(pref_MonitorModeDB.getText());
        pref_MonitorModeDB.setOnPreferenceChangeListener(this);

        getPreferenceScreen().removePreference(pref_MonitorModeTime);
        getPreferenceScreen().removePreference(pref_MonitorModeDB);

        mPackageManager = getActivity().getPackageManager();
        waitingDialog = new ProgressDialog(getActivity());
        waitingDialog.setMessage("Connecting server, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);

        SwitchPreference switchPreference = new SwitchPreference(getActivity());
        switchPreference.setSwitchTextOn("ON");
        switchPreference.setSwitchTextOff("OFF");
        switchPreference.setTitle("ABC");
        getPreferenceScreen().addPreference(switchPreference);


        switchPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.i("W", preference.getKey());
                return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.monitor_mode_settings_layout, null);
        try {
            PackageInfo packinfo = mPackageManager.getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version_info)).setText(String.format(getActivity().getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
            view.findViewById(R.id.version_info).setVisibility(View.GONE);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        btn_SyncToChild = (Button) view.findViewById(R.id.ocr_tts_sync_to_child);
        btn_SyncToChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                syncToChild();
            }
        });

        btnStartAndStopMonitorMode = (Button) view.findViewById(R.id.start_stop_monitor_mode);
        if (getMonitorModeStatus()) {
            isStartMonitorMode = true;
            btnStartAndStopMonitorMode.setText(getString(R.string.stop_monitor_mode));
        } else {
            isStartMonitorMode = false;
            btnStartAndStopMonitorMode.setText(getString(R.string.start_monitor_mode));
        }
        btnStartAndStopMonitorMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStartMonitorMode = !isStartMonitorMode;
                if (isStartMonitorMode) {
                    Intent intent = new Intent();
                    intent.setAction(MonitorModeService.ACTION);
                    intent.putExtra("action", "start");
                    getActivity().sendBroadcast(intent);
                    btnStartAndStopMonitorMode.setText(getString(R.string.stop_monitor_mode));
                } else {
                    Intent intent = new Intent();
                    intent.setAction(MonitorModeService.ACTION);
                    intent.putExtra("action", "stop");
                    getActivity().sendBroadcast(intent);
                    btnStartAndStopMonitorMode.setText(getString(R.string.start_monitor_mode));
                }
                getActivity().finish();
            }
        });

        btn_SyncToChild.setVisibility(View.GONE);
        btnStartAndStopMonitorMode.setVisibility(View.GONE);

        return view;
    }

    private List<String> getData() {

        List<String> data = new ArrayList<String>();
        data.add("测试数据1");
        data.add("测试数据2");
        data.add("测试数据3");
        data.add("测试数据4");

        return data;
    }

    private boolean getMonitorModeStatus() {
        SharedPreferences sharedPreferences = PreferenceManager.
                getDefaultSharedPreferences(getActivity());
        return sharedPreferences.getBoolean(KEY_START_MONITOR_MODE_STATUS, false);
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SYNC_ERROR:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    btn_SyncToChild.setEnabled(true);
                    Toast.makeText(getActivity(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                    break;
                case MSG_SYNC_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    btn_SyncToChild.setEnabled(true);
                    Toast.makeText(getActivity(), "sync success", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                    break;
                default:
                    break;
            }
        }
    };

    private void syncToChild() {
        final Properties properties = new Properties();
        properties.put(KEY_MONITOR_MODE_TIME, pref_MonitorModeTime.getText());
        properties.put(KEY_MONITOR_MODE_DB, pref_MonitorModeDB.getText());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String ipAddress = preferences.getString(SettingsFragment.KEY_SERVER_IP_ADDRESS, getActivity().getString(R.string.preference_server_ip_address_default_value));
        final String domain = preferences.getString(SettingsFragment.KEY_SERVER_DOMAIN, getString(R.string.preference_server_domain_default_value));
        final String userName = preferences.getString(SettingsFragment.KEY_SERVER_USER_NAME, getActivity().getString(R.string.preference_server_username_default_value));
        final String password = preferences.getString(SettingsFragment.KEY_SERVER_PASSWORD, getActivity().getString(R.string.preference_server_password_default_value));
        Log.i("W", "server address = " + ipAddress + ", domain = " + domain + ", userName = " + userName + ", password = " + password);

        btn_SyncToChild.setEnabled(false);
        waitingDialog.show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, userName, password);
                    SmbFile mFile = new SmbFile(String.format("smb://%1$s/%2$s", ipAddress, mSharedPath), authentication);
                    Log.i("W", "mFile = " + mFile.getCanonicalPath() + ", isFile = " + mFile.isFile() + ", canWrite = " + mFile.canWrite());

                    SmbFile parentFolder = new SmbFile(mFile.getParent(), authentication);
                    if (!parentFolder.exists()) {
                        parentFolder.mkdirs();
                        Log.i("W", "to create folder");
                    }

                    if (!mFile.exists()) {
                        mFile.createNewFile();
                    }

                    properties.store(new SmbFileOutputStream(mFile), "Please don\'t modify this file.");
                    mMainHandler.sendEmptyMessage(MSG_SYNC_SUCCESS);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + e.getMessage();
                    mMainHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + e.getMessage();
                    mMainHandler.sendMessage(msg);
                }
            }
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.i("W", preference.getKey());
        if (preference.getKey().equals(KEY_MONITOR_MODE_TIME)) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            editTextPreference.setSummary(newValue.toString());
            return true;
        } else if (preference.getKey().equals(KEY_MONITOR_MODE_DB)) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;
            editTextPreference.setSummary(newValue.toString());
            return true;
        }
        return false;
    }


}
