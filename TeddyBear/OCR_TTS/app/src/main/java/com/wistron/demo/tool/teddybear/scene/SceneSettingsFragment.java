package com.wistron.demo.tool.teddybear.scene;


import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class SceneSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    public static final String KEY_SPEAKING_LANGUAGE = "stt_language";
    public static final String KEY_RECOGNITION_LANGUAGE = "tts_language";

    private ListPreference pref_SpeakingLanguage;
    private ListPreference pref_RecognitionLanguage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.scene_settings_preference);

        pref_SpeakingLanguage = (ListPreference) findPreference(KEY_SPEAKING_LANGUAGE);
        pref_SpeakingLanguage.setSummary(pref_SpeakingLanguage.getEntry());
        pref_SpeakingLanguage.setOnPreferenceChangeListener(this);

        pref_RecognitionLanguage = (ListPreference) findPreference(KEY_RECOGNITION_LANGUAGE);
        pref_RecognitionLanguage.setSummary(pref_RecognitionLanguage.getEntry());
        pref_RecognitionLanguage.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(KEY_SPEAKING_LANGUAGE)
                || preference.getKey().equals(KEY_RECOGNITION_LANGUAGE)) {
            ListPreference listPreference = ((ListPreference) preference);
            preference.setSummary(listPreference.getEntries()[listPreference.findIndexOfValue(String.valueOf(newValue))]);
            Log.i("King", "key = " + preference.getKey() + ", newValue = " + newValue);
            switch (preference.getKey()) {
                case KEY_SPEAKING_LANGUAGE:

                    break;
                case KEY_RECOGNITION_LANGUAGE:

                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
    }

}
