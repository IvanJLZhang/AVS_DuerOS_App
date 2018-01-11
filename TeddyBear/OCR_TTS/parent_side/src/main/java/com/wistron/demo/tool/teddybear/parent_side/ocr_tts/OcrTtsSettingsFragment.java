package com.wistron.demo.tool.teddybear.parent_side.ocr_tts;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.Child;
import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.useless.SettingsFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Properties;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by king on 16-3-10.
 */
public class OcrTtsSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final String KEY_VOICE_LANGUAGE = "voice_language";
    public static final String KEY_VOICE_GENDER = "voice_gender";
    public static final String KEY_TRANSLATE_ENABLER = "translate_enabler";
    public static final String KEY_TRANSLATE_LANGUAGE = "translate_language";  // reserved
    public static final String KEY_TRANSLATOR = "translator";
    public static final String KEY_GOOGLE_VOICE_LANGUAGE = "google_voice_language";

    private SwitchPreference pref_TranslateEnabler;
    private ListPreference pref_Translator;
    private ListPreference pref_VoiceGender;
    private ListPreference pref_VoiceLanguage;
    private ListPreference pref_GoogleVoiceLanguage;
    private ListPreference pref_TranslateLanguage; // reserved

    private Spinner spinner_ChildrenList;
    private Button btn_SyncToChild;

    private PackageManager mPackageManager;

    //private String mCurAudioLanguage;

    private final int MSG_SYNC_ERROR = 0;
    private final int MSG_SYNC_SUCCESS = 1;
    private final int MSG_SHOW_WAITING_DIALOG = 2;
    private final String mSharedPath = "TeddyBear/%1$s/BookReaderSettings/settings.txt";  // reserved
    private ProgressDialog waitingDialog;
    private ArrayList<Child> children;
    private boolean isChildChanged = false;

    private String mRemoteFolder = "%1$s/BookReaderSettings/";
    private String mRemoteFileName = "settings.txt";
    private BaseTaskManager mTaskManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("King", "onCreate...");
        addPreferencesFromResource(R.xml.ocr_tts_settings_preference);

        //mCurAudioLanguage = getArguments().getString(CommonHelper.EXTRA_AUDIO_LANGUAGE);

        pref_VoiceGender = (ListPreference) findPreference(KEY_VOICE_GENDER);
        pref_VoiceGender.setSummary(pref_VoiceGender.getEntry());
        pref_VoiceGender.setOnPreferenceChangeListener(this);

        pref_VoiceLanguage = (ListPreference) findPreference(KEY_VOICE_LANGUAGE);
        pref_VoiceLanguage.setSummary(pref_VoiceLanguage.getEntry());
        pref_VoiceLanguage.setOnPreferenceChangeListener(this);

        pref_TranslateEnabler = (SwitchPreference) findPreference(KEY_TRANSLATE_ENABLER);
        pref_TranslateEnabler.setOnPreferenceChangeListener(this);
        pref_TranslateEnabler.setOnPreferenceClickListener(this);

        // reserved this item
        pref_TranslateLanguage = (ListPreference) findPreference(KEY_TRANSLATE_LANGUAGE);
        getPreferenceScreen().removePreference(pref_TranslateLanguage);

        pref_Translator = (ListPreference) findPreference(KEY_TRANSLATOR);
        pref_Translator.setSummary(pref_Translator.getEntry());
        pref_Translator.setOnPreferenceChangeListener(this);

        pref_GoogleVoiceLanguage = (ListPreference) findPreference(KEY_GOOGLE_VOICE_LANGUAGE);
        pref_GoogleVoiceLanguage.setSummary(pref_GoogleVoiceLanguage.getEntry());
        pref_GoogleVoiceLanguage.setOnPreferenceChangeListener(this);

        mPackageManager = getActivity().getPackageManager();
        waitingDialog = new ProgressDialog(getActivity());
        waitingDialog.setMessage("Connecting server, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);

        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(getActivity());
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(getActivity());
        }
        mTaskManager.addAzureStorageChangedListener(mResultChangedListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_KING)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    mMainHandler.sendEmptyMessage(MSG_SYNC_SUCCESS);
                    Log.i("King", "upload success");
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", getString(R.string.msg_cant_connect_ftp_server));
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD) {
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + getString(R.string.msg_ftp_sync_fail);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", "sync failed");
                }
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.i("King", "onStart...");
        /*boolean translateEnabler = pref_TranslateEnabler.isChecked();
        if (translateEnabler) {
            updateVoiceGender(pref_VoiceLanguage.getValue(), true);
        } else {
            updateVoiceGender(CommonHelper.mLanguageRegionPair.get(mCurAudioLanguage), true);
        }*/

        String currentTranslator = pref_Translator.getValue();
        updatePreferenceByTranslatorChanged(currentTranslator);
    }

    private void updatePreferenceByTranslatorChanged(String value) {
        Log.i("King", "updatePreferenceByTranslatorChanged newValue = " + value);
        if (value.equalsIgnoreCase(getString(R.string.translator_google))) { // Google
            getPreferenceScreen().removePreference(pref_VoiceLanguage);
            getPreferenceScreen().removePreference(pref_VoiceGender);
            getPreferenceScreen().addPreference(pref_GoogleVoiceLanguage);
        } else { // Baidu || Microsoft
            getPreferenceScreen().removePreference(pref_GoogleVoiceLanguage);
            getPreferenceScreen().addPreference(pref_VoiceLanguage);
            getPreferenceScreen().addPreference(pref_VoiceGender);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i("King", "onCreateView...");
        View view = inflater.inflate(R.layout.ocr_tts_settings_layout, null);
        try {
            PackageInfo packinfo = mPackageManager.getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version_info)).setText(String.format(getActivity().getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
            view.findViewById(R.id.version_info).setVisibility(View.GONE);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        spinner_ChildrenList = (Spinner) view.findViewById(R.id.ocr_tts_children_list);
        children = CommonHelper.getChildrenList(getActivity());
        ArrayAdapter<Child> adapter = new ArrayAdapter<>(getActivity(), R.layout.children_spinner_layout, children);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        spinner_ChildrenList.setAdapter(adapter);
        spinner_ChildrenList.setOnItemSelectedListener(onItemSelectedListener);
        view.findViewById(R.id.ocr_tts_no_child_warning).setVisibility(children.size() > 0 ? View.GONE : View.VISIBLE);

        view.findViewById(R.id.ocr_tts_sync_layout).setVisibility(children.size() > 0 ? View.VISIBLE : View.GONE);

        btn_SyncToChild = (Button) view.findViewById(R.id.ocr_tts_sync_to_child);
        btn_SyncToChild.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // syncToChildBySmb();  // reserved
                syncToChildByFTP();
            }
        });

        Typeface typeface1 = Typeface.createFromAsset(getActivity().getAssets(), "fonts/calibril.ttf");
        ((TextView) view.findViewById(R.id.ocr_tts_no_child_warning)).setTypeface(typeface1);
        ((TextView) view.findViewById(R.id.ocr_tts_child_title)).setTypeface(typeface1);

        return view;
    }

    private void updatePreference() {
        int index = spinner_ChildrenList.getSelectedItemPosition();
        Log.i("King", "updatePreference index = " + index);
        if (index >= 0) {
            String sn = children.get(index).getSn();

            File mFile = getLocalPath(sn);
            Properties properties = new Properties();
            try {
                properties.load(new FileInputStream(mFile));
            } catch (IOException e) {
                e.printStackTrace();
            }

            boolean translateEnabler = Boolean.parseBoolean(properties.getProperty(KEY_TRANSLATE_ENABLER, getString(R.string.preference_translate_enabler_default_value)));
            if (translateEnabler != pref_TranslateEnabler.isChecked()) {
                Log.i("King", "update to new vaue");
                isChildChanged = true;
            }
            pref_TranslateEnabler.setChecked(translateEnabler);

            String languageRegion = properties.getProperty(KEY_VOICE_LANGUAGE, getString(R.string.preference_voice_language_default_value));
            pref_VoiceLanguage.setValue(languageRegion);

            String gender = properties.getProperty(KEY_VOICE_GENDER, getString(R.string.preference_voice_gender_default_value));
            updateVoiceGender(languageRegion, false, gender);

            String translator = properties.getProperty(KEY_TRANSLATOR, getString(R.string.preference_translator_default_value));
            pref_Translator.setValue(translator);

            String google_voiceLangauge = properties.getProperty(KEY_GOOGLE_VOICE_LANGUAGE, getString(R.string.preference_google_voice_language_default_value));
            pref_GoogleVoiceLanguage.setValue(google_voiceLangauge);
            Log.i("King", "languageRegion = " + languageRegion + ", gender = " + gender + ", translator = " + translator);

            updateSummary();
        }
    }

    private File getLocalPath(String sn) {
        File file = new File(getActivity().getFilesDir() + "/BookReaderSettings", String.format("%1$s.txt", sn));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    private void storeToLocalSetting() {
        int index = spinner_ChildrenList.getSelectedItemPosition();
        Log.i("King", "storeToLocalSetting index = " + index);
        if (index >= 0) {
            String sn = children.get(index).getSn();

            File mFile = getLocalPath(sn);
            Log.i("King", "mFile = " + mFile.getAbsolutePath());
            Properties properties = new Properties();
            properties.put(KEY_TRANSLATE_ENABLER, String.valueOf(pref_TranslateEnabler.isChecked()));
            properties.put(KEY_TRANSLATOR, pref_Translator.getValue());
            properties.put(KEY_VOICE_LANGUAGE, pref_VoiceLanguage.getValue());
            properties.put(KEY_VOICE_GENDER, pref_VoiceGender.getValue());
            properties.put(KEY_GOOGLE_VOICE_LANGUAGE, pref_GoogleVoiceLanguage.getValue());
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(mFile);
                properties.store(out, getString(R.string.msg_dont_modify_the_file));
            } catch (IOException e) {
                e.printStackTrace();
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
    }

    private void updateSummary() {
        pref_VoiceLanguage.setSummary(pref_VoiceLanguage.getEntry());
        pref_VoiceGender.setSummary(pref_VoiceGender.getEntry());
        pref_Translator.setSummary(pref_Translator.getEntry());
        pref_GoogleVoiceLanguage.setSummary(pref_GoogleVoiceLanguage.getEntry());
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            updatePreference();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

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
                    Toast.makeText(getActivity(), msg.obj.toString(), Toast.LENGTH_LONG).show();
                    break;
                case MSG_SYNC_SUCCESS:
                    if (waitingDialog.isShowing()) {
                        waitingDialog.dismiss();
                    }
                    btn_SyncToChild.setEnabled(true);
                    Toast.makeText(getActivity(), "sync success", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    private void syncToChildByFTP() {
        btn_SyncToChild.setEnabled(false);
        waitingDialog.show();

        int index = spinner_ChildrenList.getSelectedItemPosition();
        String sn = null;
        if (index >= 0) {
            sn = children.get(index).getSn();
            File localFile = getLocalPath(sn);
            if (!localFile.exists()) {
                storeToLocalSetting();
            }
            mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_KING, BaseTaskManager.REQUEST_ACTION_UPLOAD, localFile.getAbsolutePath(), String.format(mRemoteFolder, sn), mRemoteFileName);
        } else {
            Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
            msg.obj = "sync fail: " + getString(R.string.msg_sn_invalid);
            mMainHandler.sendMessage(msg);

            Log.i("King", getString(R.string.msg_sn_invalid));
        }

        /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String address = preferences.getString(SettingsFragment.KEY_FTP_SERVER_ADDRESS, getActivity().getString(R.string.preference_ftp_server_ip_address_default_value));
        final String port = preferences.getString(SettingsFragment.KEY_FTP_SERVER_PORT, getActivity().getString(R.string.preference_ftp_server_port_default_value));
        final String userName = preferences.getString(SettingsFragment.KEY_FTP_SERVER_USER_NAME, getActivity().getString(R.string.preference_ftp_server_username_default_value));
        final String password = preferences.getString(SettingsFragment.KEY_FTP_SERVER_PASSWORD, getActivity().getString(R.string.preference_ftp_server_password_default_value));
        Log.i("King", "FTP server address = " + address + ", port = " + port + ", userName = " + userName + ", password = " + password);*/

        /*AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                FTPHelper ftpHelper = new FTPHelper(address, Integer.parseInt(port), userName,password);
                boolean isConnected = ftpHelper.connect();
                if (!isConnected){
                    Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                    msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", getString(R.string.msg_cant_connect_ftp_server));
                }else {
                    int index = spinner_ChildrenList.getSelectedItemPosition();
                    String sn = null;
                    if (index >= 0) {
                        sn = children.get(index).getSn();
                        File localFile = getLocalPath(sn);
                        if (!localFile.exists()){
                            storeToLocalSetting();
                        }
                        boolean pass = ftpHelper.uploadFile(localFile, String.format("%1$s/BookReaderSettings/",sn), "settings.txt");
                        if (pass){
                            mMainHandler.sendEmptyMessage(MSG_SYNC_SUCCESS);
                            Log.i("King", "upload success");
                        }else {
                            Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                            msg.obj = "sync fail: " + getString(R.string.msg_ftp_sync_fail);
                            mMainHandler.sendMessage(msg);

                            Log.i("King", "sync failed");
                        }
                    }else {
                        Message msg = mMainHandler.obtainMessage(MSG_SYNC_ERROR);
                        msg.obj = "sync fail: " + getString(R.string.msg_sn_invalid);
                        mMainHandler.sendMessage(msg);

                        Log.i("King", getString(R.string.msg_sn_invalid));
                    }
                }
                ftpHelper.disconnect();
            }
        });*/
    }

    // reserved --> FTP
    private void syncToChildBySmb() {
        final Properties properties = new Properties();
        properties.put(KEY_TRANSLATE_ENABLER, String.valueOf(pref_TranslateEnabler.isChecked()));
        properties.put(KEY_VOICE_LANGUAGE, pref_VoiceLanguage.getValue());
        properties.put(KEY_VOICE_GENDER, pref_VoiceGender.getValue());
        properties.put(KEY_TRANSLATOR, pref_Translator.getValue());
        properties.put(KEY_GOOGLE_VOICE_LANGUAGE, pref_GoogleVoiceLanguage.getValue());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final String ipAddress = preferences.getString(SettingsFragment.KEY_SERVER_IP_ADDRESS, getActivity().getString(R.string.preference_server_ip_address_default_value));
        String domainSettings = preferences.getString(SettingsFragment.KEY_SERVER_DOMAIN, getString(R.string.preference_server_domain_default_value));
        final String domain = TextUtils.isEmpty(domainSettings) ? null : domainSettings;
        final String userName = preferences.getString(SettingsFragment.KEY_SERVER_USER_NAME, getActivity().getString(R.string.preference_server_username_default_value));
        final String password = preferences.getString(SettingsFragment.KEY_SERVER_PASSWORD, getActivity().getString(R.string.preference_server_password_default_value));
        Log.i("King", "server address = " + ipAddress + ", domain = " + domain + ", userName = " + userName + ", password = " + password);

        btn_SyncToChild.setEnabled(false);
        waitingDialog.show();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    int index = spinner_ChildrenList.getSelectedItemPosition();
                    String sn = null;
                    if (index >= 0) {
                        sn = children.get(index).getSn();
                    } else {
                        return;
                    }

                    NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(domain, userName, password);
                    SmbFile mFile = new SmbFile(String.format("smb://%1$s/%2$s", ipAddress, String.format(mSharedPath, sn)), authentication);
                    //SmbFile mFile = new SmbFile(String.format("smb://%1$s:%2$s@%3$s/%4$s", userName, password, ipAddress, String.format(mSharedPath, sn)));
                    Log.i("King", "mFile = " + mFile.getCanonicalPath() + ", isFile = " + mFile.isFile() + ", canWrite = " + mFile.canWrite());
                    if (!mFile.exists()) {
                        SmbFile parentFolder = new SmbFile(mFile.getParent(), authentication);
                        //SmbFile parentFolder = new SmbFile(mFile.getParent());
                        Log.i("King", "to create folder");
                        parentFolder.mkdirs();

                        mFile.createNewFile();
                    }

                    properties.store(new SmbFileOutputStream(mFile), getString(R.string.msg_dont_modify_the_file));
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
        Log.i("King", "onPreferenceChange...");
        if (isChildChanged) {
            isChildChanged = false;
            return true;
        }
        Log.i("King", "key = " + preference.getKey() + ", newValue = " + newValue);
        if (preference.getKey().equals(KEY_VOICE_GENDER) ||
                preference.getKey().equals(KEY_VOICE_LANGUAGE) ||
                preference.getKey().equals(KEY_TRANSLATE_LANGUAGE) ||
                preference.getKey().equals(KEY_TRANSLATOR) ||
                preference.getKey().equals(KEY_GOOGLE_VOICE_LANGUAGE)) {
            ListPreference listPreference = ((ListPreference) preference);
            preference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(String.valueOf(newValue))]);
            switch (preference.getKey()) {
                case KEY_VOICE_LANGUAGE:
                    String oldVoiceLanguageValue = pref_VoiceLanguage.getValue();
                    if (!oldVoiceLanguageValue.equals(newValue)) {
                        updateVoiceGender((String) newValue, false);
                    }
                    break;
                case KEY_TRANSLATE_LANGUAGE:
                    String oldTranslateLanguageValue = pref_TranslateLanguage.getValue();
                    if (!oldTranslateLanguageValue.equals(newValue)) {
                        updateLanguageRegion((String) newValue, false);
                    }
                    break;
                case KEY_TRANSLATOR:
                    updatePreferenceByTranslatorChanged((String) newValue);
                    break;
            }

            listPreference.setValue((String) newValue);
        } else if (preference.getKey().equals(KEY_TRANSLATE_ENABLER)) {
            boolean enabler = (boolean) newValue;
            //if (enabler) {
            updateVoiceGender(pref_VoiceLanguage.getValue(), false);
            /*} else {
                Log.i("King", "mCurAudioLanguage = " + mCurAudioLanguage);
                updateVoiceGender(CommonHelper.mLanguageRegionPair.get(mCurAudioLanguage), true);
            }*/

            ((SwitchPreference) preference).setChecked(enabler);
        }

        storeToLocalSetting();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.i("King", "onPreferenceTreeClick...");
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateVoiceGender(String languageRegion, boolean useOldValue, String value) {
        if (languageRegion == null) {
            languageRegion = CommonHelper.LanguageRegion.REGION_ENGLISH_US;
        }

        CharSequence[] entries = null;
        CharSequence[] values = null;
        if (pref_TranslateEnabler.isChecked()) {
            switch (languageRegion) {
                case CommonHelper.LanguageRegion.REGION_ENGLISH_AU:
                case CommonHelper.LanguageRegion.REGION_ENGLISH_CA:
                case CommonHelper.LanguageRegion.REGION_FRANCE_CA:
                    entries = getResources().getStringArray(R.array.preference_voice_gender_entries_female);
                    values = getResources().getStringArray(R.array.preference_voice_gender_values_female);
                    break;
                case CommonHelper.LanguageRegion.REGION_ENGLISH_IN:
                case CommonHelper.LanguageRegion.REGION_ESPANOL_MX:
                case CommonHelper.LanguageRegion.REGION_ITALIA:
                case CommonHelper.LanguageRegion.REGION_PORTUGUESE:
                    entries = getResources().getStringArray(R.array.preference_voice_gender_entries_male);
                    values = getResources().getStringArray(R.array.preference_voice_gender_values_male);
                    break;
                default:
                    entries = getResources().getStringArray(R.array.preference_voice_gender_entries_both);
                    values = getResources().getStringArray(R.array.preference_voice_gender_values_both);
                    break;
            }
        } else {
            entries = getResources().getStringArray(R.array.preference_voice_gender_entries_both);
            values = getResources().getStringArray(R.array.preference_voice_gender_values_both);
        }
        if (entries != null && values != null) {
            pref_VoiceGender.setEntries(entries);
            pref_VoiceGender.setEntryValues(values);
            if (value != null) {
                pref_VoiceGender.setValue(value);
            } else {
                pref_VoiceGender.setValue(useOldValue && pref_VoiceGender.isEnabled() ? pref_VoiceGender.getValue() :
                        values.length <= 1 ? values[0].toString() : values[1].toString());
            }
            pref_VoiceGender.setSummary(pref_VoiceGender.getEntry());
        }
    }

    private void updateVoiceGender(String languageRegion, boolean useOldValue) {
        updateVoiceGender(languageRegion, useOldValue, null);
    }

    private void updateLanguageRegion(String translateLanguageValue, boolean useOldValue) {
        /*String translator = pref_Translator.getValue();
        if (TranslateBase.TRANSLATOR_BAIDU.equals(translator)) {
            CharSequence[] entries = null;
            CharSequence[] values = null;
            switch (translateLanguageValue) {
                case BaiduTranslate.BAIDU_LANGUAGE_ENGLISH:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_english);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_english);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_CHINESE_SIMPLIFIED:
                case BaiduTranslate.BAIDU_LANGUAGE_CHINESE_TRADITIONAL:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_chinese);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_chinese);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_JAPANESE:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_japan);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_japan);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_GERMANY:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_germany);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_germany);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_FRANCE:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_france);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_france);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_PORTUGUESE:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_portuguese);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_portuguese);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_ITALIANO:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_italia);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_italia);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_ESPANOL:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_espanol);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_espanol);
                    break;
                case BaiduTranslate.BAIDU_LANGUAGE_RUSSIAN:
                    entries = getResources().getStringArray(R.array.preference_voice_language_entries_russian);
                    values = getResources().getStringArray(R.array.preference_voice_language_values_russian);
                    break;
            }
            if (entries != null && values != null) {
                pref_VoiceLanguage.setEntries(entries);
                pref_VoiceLanguage.setEntryValues(values);
                Log.i("King", "useOldValue = " + useOldValue + ", oldValue = " + pref_VoiceLanguage.getValue());
                pref_VoiceLanguage.setValue(useOldValue ? pref_VoiceLanguage.getValue() : values[0].toString());
                pref_VoiceLanguage.setSummary(pref_VoiceLanguage.getEntry());

                Log.i("King", "pref_VoiceLanguage.getValue = " + pref_VoiceLanguage.getValue());
                updateVoiceGender(pref_VoiceLanguage.getValue(), useOldValue);
            }
        }*/
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.i("King", "onPreferenceClick...");
        return false;
    }
}
