package com.wistron.demo.tool.teddybear.parent_side.useless;


import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.wistron.demo.tool.teddybear.parent_side.R;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    // SMB params
    public static final String KEY_SERVER_IP_ADDRESS = "server_ip_address";
    public static final String KEY_SERVER_DOMAIN = "server_domain";
    public static final String KEY_SERVER_USER_NAME = "server_username";
    public static final String KEY_SERVER_PASSWORD = "server_password";
    public EditTextPreference pref_ServerIPAddress;
    public EditTextPreference pref_ServerDomain;
    public EditTextPreference pref_ServerUserName;
    public EditTextPreference pref_ServerPassword;

    // FTP params
    public static final String KEY_FTP_SERVER_ADDRESS = "ftp_server_address";
    public static final String KEY_FTP_SERVER_PORT = "ftp_server_port";
    public static final String KEY_FTP_SERVER_USER_NAME = "ftp_server_username";
    public static final String KEY_FTP_SERVER_PASSWORD = "ftp_server_password";
    public EditTextPreference pref_FTPServerAddress;
    public EditTextPreference pref_FTPServerPort;
    public EditTextPreference pref_FTPServerUserName;
    public EditTextPreference pref_FTPServerPassword;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preference);

        // SMB
        pref_ServerIPAddress = (EditTextPreference) findPreference(KEY_SERVER_IP_ADDRESS);
        pref_ServerIPAddress.setSummary(pref_ServerIPAddress.getText());
        pref_ServerIPAddress.setOnPreferenceChangeListener(this);

        pref_ServerDomain = (EditTextPreference) findPreference(KEY_SERVER_DOMAIN);
        pref_ServerDomain.setSummary(pref_ServerDomain.getText());
        pref_ServerDomain.setOnPreferenceChangeListener(this);

        pref_ServerUserName = (EditTextPreference) findPreference(KEY_SERVER_USER_NAME);
        pref_ServerUserName.setSummary(pref_ServerUserName.getText());
        pref_ServerUserName.setOnPreferenceChangeListener(this);

        pref_ServerPassword = (EditTextPreference) findPreference(KEY_SERVER_PASSWORD);
        setPrefPasswordSummary(pref_ServerPassword, pref_ServerPassword.getText());
        pref_ServerPassword.setOnPreferenceChangeListener(this);

        // FTP
        pref_FTPServerAddress = (EditTextPreference) findPreference(KEY_FTP_SERVER_ADDRESS);
        pref_FTPServerAddress.setSummary(pref_FTPServerAddress.getText());
        pref_FTPServerAddress.setOnPreferenceChangeListener(this);

        pref_FTPServerPort = (EditTextPreference) findPreference(KEY_FTP_SERVER_PORT);
        pref_FTPServerPort.setSummary(pref_FTPServerPort.getText());
        pref_FTPServerPort.setOnPreferenceChangeListener(this);

        pref_FTPServerUserName = (EditTextPreference) findPreference(KEY_FTP_SERVER_USER_NAME);
        pref_FTPServerUserName.setSummary(pref_FTPServerUserName.getText());
        pref_FTPServerUserName.setOnPreferenceChangeListener(this);

        pref_FTPServerPassword = (EditTextPreference) findPreference(KEY_FTP_SERVER_PASSWORD);
        setPrefPasswordSummary(pref_FTPServerPassword, pref_FTPServerPassword.getText());
        pref_FTPServerPassword.setOnPreferenceChangeListener(this);

        // remove SMB settings
        getPreferenceScreen().removePreference(pref_ServerIPAddress);
        getPreferenceScreen().removePreference(pref_ServerDomain);
        getPreferenceScreen().removePreference(pref_ServerUserName);
        getPreferenceScreen().removePreference(pref_ServerPassword);
    }

    private void setPrefPasswordSummary(EditTextPreference preference, String text) {
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            summary.append(getString(R.string.preference_server_password_format));
        }
        preference.setSummary(summary.toString());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.i("King", "key = " + preference.getKey() + ", newValue = " + newValue);
        if (preference.getKey().equals(KEY_SERVER_IP_ADDRESS)
                || preference.getKey().equals(KEY_SERVER_USER_NAME)
                || preference.getKey().equals(KEY_SERVER_DOMAIN)
                || preference.getKey().equals(KEY_FTP_SERVER_ADDRESS)
                || preference.getKey().equals(KEY_FTP_SERVER_USER_NAME)
                || preference.getKey().equals(KEY_FTP_SERVER_PORT)) {
            preference.setSummary(newValue.toString());
            return true;
        } else if (preference.getKey().equals(KEY_SERVER_PASSWORD)
                || preference.getKey().equals(KEY_FTP_SERVER_PASSWORD)) {
            setPrefPasswordSummary((EditTextPreference) preference, newValue.toString());
            return true;
        }
        return false;
    }
}
