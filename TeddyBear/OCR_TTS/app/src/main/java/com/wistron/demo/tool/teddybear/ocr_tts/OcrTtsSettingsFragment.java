package com.wistron.demo.tool.teddybear.ocr_tts;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.BaiduTranslate;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.TranslateBase;

/**
 * Created by king on 16-3-10.
 */
public class OcrTtsSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String KEY_PARENT_CONTROL = "parent_control";
    public static final String KEY_VOICE_LANGUAGE = "voice_language";
    public static final String KEY_VOICE_GENDER = "voice_gender";
    public static final String KEY_TRANSLATE_ENABLER = "translate_enabler";
    public static final String KEY_TRANSLATE_LANGUAGE = "translate_language";
    public static final String KEY_TRANSLATOR = "translator";
    public static final String KEY_GOOGLE_VOICE_LANGUAGE = "google_voice_language";

    private CheckBoxPreference pref_ParentControl;
    private SwitchPreference pref_TranslateEnabler;
    private ListPreference pref_Translator;
    private ListPreference pref_VoiceGender;
    private ListPreference pref_VoiceLanguage;
    private ListPreference pref_GoogleVoiceLanguage;
    private ListPreference pref_TranslateLanguage; // reserved

    private PackageManager mPackageManager;

    private String mCurAudioLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ocr_tts_settings_preference);

        mCurAudioLanguage = getArguments().getString(CommonHelper.EXTRA_AUDIO_LANGUAGE);

        pref_ParentControl = (CheckBoxPreference) findPreference(KEY_PARENT_CONTROL);
        pref_ParentControl.setOnPreferenceChangeListener(this);

        pref_VoiceGender = (ListPreference) findPreference(KEY_VOICE_GENDER);
        pref_VoiceGender.setSummary(pref_VoiceGender.getEntry());
        pref_VoiceGender.setOnPreferenceChangeListener(this);

        pref_VoiceLanguage = (ListPreference) findPreference(KEY_VOICE_LANGUAGE);
        Log.i("King", "onCreate pref_VoiceLanguage.getEntry = " + pref_VoiceLanguage.getEntry());
        pref_VoiceLanguage.setSummary(pref_VoiceLanguage.getEntry());
        pref_VoiceLanguage.setOnPreferenceChangeListener(this);

        pref_TranslateEnabler = (SwitchPreference) findPreference(KEY_TRANSLATE_ENABLER);
        pref_TranslateEnabler.setOnPreferenceChangeListener(this);

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
    }

    @Override
    public void onStart() {
        super.onStart();
        boolean translateEnabler = pref_TranslateEnabler.isChecked();
        if (translateEnabler) {
            updateVoiceGender(pref_VoiceLanguage.getValue(), true);
        } else {
            updateVoiceGender(CommonHelper.mLanguageRegionPair.get(mCurAudioLanguage), true);
        }

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
        /*View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(Color.LTGRAY);*/
        View view = inflater.inflate(R.layout.ocr_tts_settings_layout, null);
        try {
            PackageInfo packinfo = mPackageManager.getPackageInfo(getActivity().getPackageName(), 0);
            ((TextView) view.findViewById(R.id.version_info)).setText(String.format(getActivity().getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
            view.findViewById(R.id.version_info).setVisibility(View.GONE);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(KEY_VOICE_GENDER) ||
                preference.getKey().equals(KEY_VOICE_LANGUAGE) ||
                preference.getKey().equals(KEY_TRANSLATE_LANGUAGE) ||
                preference.getKey().equals(KEY_TRANSLATOR) ||
                preference.getKey().equals(KEY_GOOGLE_VOICE_LANGUAGE)) {
            ListPreference listPreference = ((ListPreference) preference);
            preference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(String.valueOf(newValue))]);
            Log.i("King", "key = " + preference.getKey() + ", newValue = " + newValue);
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
            return true;
        } else if (preference.getKey().equals(KEY_TRANSLATE_ENABLER)) {
            boolean enabler = (boolean) newValue;
            if (enabler) {
                updateVoiceGender(pref_VoiceLanguage.getValue(), false);
            } else {
                Log.i("King", "mCurAudioLanguage = " + mCurAudioLanguage);
                updateVoiceGender(CommonHelper.mLanguageRegionPair.get(mCurAudioLanguage), true);
            }
            return true;
        } else if (preference.getKey().equals(KEY_PARENT_CONTROL)) {
            return true;
        }
        return false;
    }

    private void updateVoiceGender(String languageRegion, boolean useOldValue) {
        if (languageRegion == null) {
            languageRegion = CommonHelper.LanguageRegion.REGION_ENGLISH_US;
        }

        CharSequence[] entries = null;
        CharSequence[] values = null;
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
        if (entries != null && values != null) {
            pref_VoiceGender.setEntries(entries);
            pref_VoiceGender.setEntryValues(values);
            pref_VoiceGender.setValue(useOldValue && pref_VoiceGender.isEnabled() ? pref_VoiceGender.getValue() :
                    values.length <= 1 ? values[0].toString() : values[1].toString());
            pref_VoiceGender.setSummary(pref_VoiceGender.getEntry());
        }
    }

    private void updateLanguageRegion(String translateLanguageValue, boolean useOldValue) {
        String translator = pref_Translator.getValue();
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
        }
    }
}
