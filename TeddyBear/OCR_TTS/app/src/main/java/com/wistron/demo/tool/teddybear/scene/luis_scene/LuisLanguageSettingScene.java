package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.SceneSettingsFragment;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by king on 16-7-8.
 */
public class LuisLanguageSettingScene extends SceneBase {

    public LuisLanguageSettingScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        try {
            sceneAction = LuisHelper.ENTITIES_TYPE_LANGUAGE_SPEAKING;
            String toLanguage = null;
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_LANGUAGE_LANGUAGE.equals(entitiesType)) {
                    toLanguage = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY).toLowerCase();
                } else if (LuisHelper.ENTITIES_TYPE_LANGUAGE_SPEAKING.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_LANGUAGE_RECOGNITION.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_LANGUAGE_CURRENT.equals(entitiesType)) {
                    sceneAction = entitiesType;
                }
            }
            if (!TextUtils.isEmpty(sceneAction)) {
                if (LuisHelper.ENTITIES_TYPE_LANGUAGE_SPEAKING.equals(sceneAction)) {
                    if (TextUtils.isEmpty(toLanguage)) {
                        toSpeakThenDone(getString(R.string.luis_assistant_language_setting_language_null));
                    } else {
                        if (SceneCommonHelper.mSpeakingLanguageRegionPairs.containsKey(toLanguage)) {
                            updateLocalSettings(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE, SceneCommonHelper.mSpeakingLanguageRegionPairs.get(toLanguage));
                            toSpeakThenDone(getString(R.string.luis_assistant_language_setting_success_to_switch));
                        } else {
                            toSpeakThenDone(getString(R.string.luis_assistant_language_setting_unsupport));
                        }
                    }
                } else if (LuisHelper.ENTITIES_TYPE_LANGUAGE_RECOGNITION.equals(sceneAction)) {
                    if (TextUtils.isEmpty(toLanguage)) {
                        toSpeakThenDone(getString(R.string.luis_assistant_language_setting_language_null));
                    } else {
                        if (SceneCommonHelper.mRecognitionLanguageRegionPairs.containsKey(toLanguage)) {
                            updateLocalSettings(SceneSettingsFragment.KEY_RECOGNITION_LANGUAGE, SceneCommonHelper.mRecognitionLanguageRegionPairs.get(toLanguage));
                            toSpeakThenDone(getString(R.string.luis_assistant_language_setting_success_to_switch));
                        } else {
                            toSpeakThenDone(getString(R.string.luis_assistant_language_setting_unsupport));
                        }
                    }
                } else if (LuisHelper.ENTITIES_TYPE_LANGUAGE_CURRENT.equals(sceneAction)) {
                    toSpeakThenDone(getString(R.string.luis_assistant_language_get_current));
                }
                ((SceneActivity) context).initialSTT();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SceneCommonHelper.closeLED();
    }

    private void updateLocalSettings(String key, String value) {
        SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        //sharedPreferencesEditor.putString(key, value);  // reserved function
        sharedPreferencesEditor.putString(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE, value);
        sharedPreferencesEditor.putString(SceneSettingsFragment.KEY_RECOGNITION_LANGUAGE, value);
        sharedPreferencesEditor.apply();
        sharedPreferencesEditor.commit();
    }
}
