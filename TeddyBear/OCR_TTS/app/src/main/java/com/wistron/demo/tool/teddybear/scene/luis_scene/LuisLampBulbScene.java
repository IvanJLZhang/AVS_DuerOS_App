package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.philips.lighting.model.PHLight;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.LampBulbBase;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.PhilipsHueLampBulb;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.YeeLightLampBulb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by king on 16-8-3.
 */

public class LuisLampBulbScene extends SceneBase {
    private enum LightProvider {
        HUE,
        YEELIGHT,

    }

    private static String mYeeLightsArray = "first, second, third, fourth, fifth";

    private String mLightName;  // For PhilipsHue test
    private int mYeeLightIndex = -1; // For YeeLight test

    private LampBulbBase mLightInstance;
    private LightProvider mLightProider;

    public LuisLampBulbScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    /*
    *
    * ENTITIES_TYPE_ON_OFF_START  open light
    * ENTITIES_TYPE_ON_OFF_STOP  close light
    * ENTITIES_TYPE_BOOK_STORY_TITLE  light name
    *
    * */
    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();
        try {
            sceneAction = null;
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_BOOK_STORY_TITLE.equals(entitiesType)) {
                    mLightName = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY).toLowerCase();
                } else if (LuisHelper.ENTITIES_TYPE_ON_OFF_START.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_ON_OFF_STOP.equals(entitiesType)) {
                    sceneAction = entitiesType;
                }
            }

            preValid();
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }

        // Debug
        /*sceneAction = LuisHelper.ENTITIES_TYPE_ON_OFF_START;
        mLightName = "test light 1";
        preValid();*/
    }

    private void preValid() {
        if (TextUtils.isEmpty(sceneAction)) {
            toSpeakThenDone(getString(R.string.luis_assistant_cmd_empty));
        } else {
            if (TextUtils.isEmpty(mLightName)) {
                toSpeak(getString(R.string.luis_assistant_philips_hue_which_light_to_control), false);
                ((SceneActivity) context).startToListenCmd(false, getString(R.string.luis_assistant_philips_hue_which_light_to_control));
            } else {
                if (isYeeLight()) { //YeeLight
                    Log.i(TAG, "YeeLightTest...");

                    mLightProider = LightProvider.YEELIGHT;
                    mLightInstance = new YeeLightLampBulb(context);
                    mLightInstance.setOnBulbStateChangedListener(mLightStateChangedListener);
                    mLightInstance.startSearch();
                } else { // Philips Hue
                    Log.i(TAG, "Philips Hue Test...");

                    mLightProider = LightProvider.HUE;
                    mLightInstance = new PhilipsHueLampBulb(context);
                    mLightInstance.setOnBulbStateChangedListener(mLightStateChangedListener);
                    mLightInstance.startSearch();
                }
            }
        }
    }

    private boolean isYeeLight() {
        String mYeeLightGroup = mYeeLightsArray;
        Map<String, String> mParametersList = SceneCommonHelper.getSingleParameters(SceneCommonHelper.STORAGE_CONFIG_FILE_PATH);
        if (mParametersList != null && mParametersList.size() > 0) {
            for (String key : mParametersList.keySet()) {
                if (key.equals(SceneCommonHelper.CONFIG_KEY_YEE_LIGHT_GROUP)) {
                    mYeeLightGroup = mParametersList.get(key);
                    break;
                }
            }
        }
        String[] lightsName = mYeeLightGroup.split(",");
        Log.i(TAG, "mLightsName = " + Arrays.toString(lightsName));
        for (int i = 0; i < lightsName.length; i++) {
            if (lightsName[i].trim().equalsIgnoreCase(mLightName)) {
                mYeeLightIndex = i;
                Log.i(TAG, "mYeeLightIndex = " + mYeeLightIndex);
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateSttResult(String result) {
        super.updateSttResult(result);
        try {
            JSONObject toJason = new JSONObject(result);
            if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                mLightName = toJason.getString(LuisHelper.TAG_QUERY);
            }

            preValid();
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mLightInstance != null) {
            mLightInstance.destroy();
        }
    }

    private LampBulbBase.onBulbStateChangedListener mLightStateChangedListener = new LampBulbBase.onBulbStateChangedListener() {
        @Override
        public void searchStart() {

        }

        @Override
        public void searchEnd() {
            switch (mLightProider) {
                case HUE:
                    List<PHLight> mHueDeviceList = ((PhilipsHueLampBulb) mLightInstance).getPhilipsHueLightList();
                    if (mHueDeviceList != null) {
                        if (mHueDeviceList.size() > 0) {
                            ((PhilipsHueLampBulb) mLightInstance).setControlLightName(mLightName);
                            if (LuisHelper.ENTITIES_TYPE_ON_OFF_START.equals(sceneAction)) { // Open light
                                mLightInstance.turnLightState(true);
                            } else if (LuisHelper.ENTITIES_TYPE_ON_OFF_STOP.equals(sceneAction)) {  // Close light
                                mLightInstance.turnLightState(false);
                            }
                        }
                    }
                    break;
                case YEELIGHT:
                    List<HashMap<String, String>> mYeelightDeviceList = ((YeeLightLampBulb) mLightInstance).getYeelightList();
                    if (mYeelightDeviceList != null) {
                        if (mYeelightDeviceList.size() > 0 && mYeelightDeviceList.size() > mYeeLightIndex) {
                            ((YeeLightLampBulb) mLightInstance).setControlLightIndex(mYeeLightIndex);
                            if (LuisHelper.ENTITIES_TYPE_ON_OFF_START.equals(sceneAction)) { // Open light
                                mLightInstance.turnLightState(true);
                            } else if (LuisHelper.ENTITIES_TYPE_ON_OFF_STOP.equals(sceneAction)) {  // Close light
                                mLightInstance.turnLightState(false);
                            }
                        } else {
                            updateLog(getString(R.string.luis_assistant_philips_hue_light_not_found));
                            toSpeak(getString(R.string.luis_assistant_philips_hue_light_not_found), false);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void updateBulbLog(String log) {
            updateLog(log);
        }

        @Override
        public void updateBulbLogAndSpeak(String log) {
            updateLog(log);
            toSpeak(log, false);
        }
    };
}
