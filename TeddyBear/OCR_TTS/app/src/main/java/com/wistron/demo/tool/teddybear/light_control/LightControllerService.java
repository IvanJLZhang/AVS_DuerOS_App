package com.wistron.demo.tool.teddybear.light_control;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.philips.lighting.model.PHLight;
import com.wistron.demo.tool.teddybear.avs.AVSUseClass;
import com.wistron.demo.tool.teddybear.avs.Common;
import com.wistron.demo.tool.teddybear.dcs.oauth.api.OauthPreferenceUtil;
import com.wistron.demo.tool.teddybear.light_control.socket.LightControllerServer;
import com.wistron.demo.tool.teddybear.light_control.util.JSONUtil;
import com.wistron.demo.tool.teddybear.light_control.util.Light;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.LampBulbBase;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.PhilipsHueLampBulb;
import com.wistron.demo.tool.teddybear.scene.lamp_bulb.YeeLightLampBulb;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.wistron.demo.tool.teddybear.avs.Common.AUTHORIZE_CODE;
import static com.wistron.demo.tool.teddybear.avs.Common.AUTHORIZE_INFO;
import static com.wistron.demo.tool.teddybear.avs.Common.CLIENT_ID;
import static com.wistron.demo.tool.teddybear.avs.Common.CODE_CHALLENGE;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_DSN;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_ID;
import static com.wistron.demo.tool.teddybear.avs.Common.PRODUCT_MATEDATA;
import static com.wistron.demo.tool.teddybear.avs.Common.REDIRECT_URI;

/**
 * Created by aaron on 17-2-21.
 */

public class LightControllerService extends Service implements LightControllerServer.onDataReceiveListener {

    private static final String TAG = "Light";
    private static String mYeeLightsArray = "first, second, third, fourth, fifth";
    public static Context context;
    private LightControllerServer mServer;
    private boolean isYeeLightSearching = false;
    private boolean isPhilipsHueSearching = false;
    private List<HashMap<String, String>> mYeelightDeviceList;
    List<PHLight> mHueDeviceList;
    private List<Light> lights;
    private LampBulbBase mYeeLightInstance;
    private LampBulbBase mPhilipsHueInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        context = this;
        mServer = new LightControllerServer();
        mServer.setOnDataReceiveListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDataReceive(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            switchCommand(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void switchCommand(JSONObject jsonObject) {
        int cmd = JSONUtil.getCommand(jsonObject);
        Log.i(TAG, " cmd " + cmd);
        switch (cmd) {
            case Light.CMD_GET_LIGHTS:
                if (!isPhilipsHueSearching && !isYeeLightSearching) {

                    if (lights == null)
                        lights = new ArrayList<>();
                    lights.clear();
                    if (mYeelightDeviceList != null)
                        mYeelightDeviceList.clear();
                    isPhilipsHueSearching = true;
                    isYeeLightSearching = true;
                    new Thread(new YeeLightRunnable()).start();
                    new Thread(new PhilipsHueRunnable()).start();
                    Log.i(TAG, "get lights");
                }
                break;
            case Light.CMD_LIGHT_ON:
                Light light_on = JSONUtil.getLight(jsonObject);
                turnLightState(light_on, true);
                break;
            case Light.CMD_LIGHT_OFF:
                Light light_off = JSONUtil.getLight(jsonObject);
                turnLightState(light_off, false);
                break;
            case 1001:
                Log.i("Bob_AVS", "receive data from parent");
                getMetaDataAndSendToParent();
                break;
            case 1002:
                Log.i("Bob_AVS", "receive authorize data");
                try {
                    String authorize_cdoe = jsonObject.getString("Code");
                    String client_id = jsonObject.getString("id");
                    String redirect_uri = jsonObject.getString("uri");
                    Log.i("Bob_AVS", "authorize code: " + authorize_cdoe);
                    Log.i("Bob_AVS", "client id: " + client_id);
                    Log.i("Bob_AVS", "redirect URI: " + redirect_uri);
                    saveAuthorizeInforAndNotificationGetToken(authorize_cdoe, client_id, redirect_uri);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case 1005:
                clearStatusAndLogout();
                break;
            case 2001:
                Log.i("Ivan_DCS", "receive data from parent");
                mServer.sendString(OauthPreferenceUtil.getAccessToken(context));
                break;
            case 2002:// receive dcs accesstoken info.
                Log.i("Ivan_DCS", jsonObject.toString());
                //{"access_token":"23.ec07237823ad4fb51c6a39d7e0bfd007.2592000.1508549131.2754435378-10112354","expires_in":2592000000,"create_time":1505957131223,"command":2002}}:wistron
                try{
                    OauthPreferenceUtil.clearAllOauth(context);
                    OauthPreferenceUtil.setAccessToken(context, jsonObject.getString("access_token"));
                    OauthPreferenceUtil.setExpires(context, jsonObject.getLong("expires_in"));
                    OauthPreferenceUtil.setCreateTime(context, jsonObject.getLong("create_time"));
                }catch (JSONException e){
                    e.printStackTrace();
                }
                break;
            case 2005:
                OauthPreferenceUtil.clearAllOauth(context);// 清空授权数据
                mServer.sendString("2005");
                break;
            default:
                break;
        }
    }

    /**
     * Get AVS MetaData and send to parent
     */
    private void getMetaDataAndSendToParent() {
        StringBuffer buffer = new StringBuffer();
        SharedPreferences preferences = context.getSharedPreferences(PRODUCT_MATEDATA, Context.MODE_PRIVATE);
        if (preferences.contains(PRODUCT_ID)) {
            buffer.append("clientId:" + preferences.getString(PRODUCT_ID, null));
        }
        buffer.append(";");
        if (preferences.contains(PRODUCT_DSN)) {
            buffer.append("dsn:" + preferences.getString(PRODUCT_DSN, null));
        }
        buffer.append(";");
        if (preferences.contains(CODE_CHALLENGE)) {
            buffer.append("codeChallenge:" + preferences.getString(CODE_CHALLENGE, null));
        }
        Log.i("Bob_AVS", "prepare send data: " + buffer.toString());
        mServer.sendString(buffer.toString());
    }

    private void saveAuthorizeInforAndNotificationGetToken(String Authorizecode, String clientId, String redirectUri) {
        SharedPreferences preferences = context.getSharedPreferences(AUTHORIZE_INFO, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(AUTHORIZE_CODE, Authorizecode);
        editor.putString(CLIENT_ID, clientId);
        editor.putString(REDIRECT_URI, redirectUri);
        editor.apply();
        editor.commit();

        //start notification get Access token
        AVSUseClass.getInstance(getApplicationContext()).startGetAccessToken();
    }

    private void clearStatusAndLogout() {
        SharedPreferences preferences = context.getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Common.PREF_ACCESS_TOKEN, "");
        editor.putString(Common.PREF_REFRESH_TOKEN, "");
        editor.apply();
        Log.i(Common.TAG, "clear success and logout");

        mServer.sendString("1005");
        AVSUseClass.getInstance(getApplicationContext()).cancelRefreshToken();
    }

    private void sendLightsList() {
        if (!isPhilipsHueSearching && !isYeeLightSearching) {
            Log.i(TAG, "send lights list");
            mServer.sendLightsList(lights);
        }
    }

    private void turnLightState(Light light, boolean b) {
        Log.i(TAG, "type " + light.getType());
        if (Light.YEELIGHT.equals(light.getType())) {
            Log.i(TAG, "  " + light.getId() + "  " + b);
            mYeeLightInstance.destroy();
            ((YeeLightLampBulb) mYeeLightInstance).setControlLightIndex(light.getId());
            mYeeLightInstance.turnLightState(b);
        } else if (Light.PHILIPSHUE.equals(light.getType())) {
            Log.i(TAG, "  " + light.getId() + "  " + b);
            ((PhilipsHueLampBulb) mPhilipsHueInstance).setControlLightName(light.getName());
            mPhilipsHueInstance.turnLightState(b);
        }
    }

    private String getYeeLightName(int i) {
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

        return lightsName[i];
    }

    private class YeeLightRunnable implements Runnable, LampBulbBase.onBulbStateChangedListener {

        public YeeLightRunnable() {
            if (mYeeLightInstance == null)
                mYeeLightInstance = new YeeLightLampBulb(context);
            mYeeLightInstance.setOnBulbStateChangedListener(this);
        }

        @Override
        public void run() {
            mYeeLightInstance.startSearch();
        }

        @Override
        public void searchStart() {

        }

        @Override
        public void searchEnd() {
            mYeelightDeviceList = ((YeeLightLampBulb) mYeeLightInstance).getYeelightList();
            if (mYeelightDeviceList != null && mYeelightDeviceList.size() > 0) {
                //startYeeLightsControl();
                for (int i = 0; i < mYeelightDeviceList.size(); i++) {
                    Light light = new Light();
                    light.setId(i);
                    light.setName(getYeeLightName(i));
                    light.setType(Light.YEELIGHT);
                    HashMap<String, String> bulbInfo = mYeelightDeviceList.get(i);
                    String power = bulbInfo.get("power");
                    Log.i(TAG, "power:" + power);
                    if (power.equals(" on")) {
                        light.setState(Light.POWER_ON);
                    } else {
                        light.setState(Light.POWER_OFF);
                    }
                    lights.add(light);
                }

            }
            isYeeLightSearching = false;
            sendLightsList();
        }

        @Override
        public void updateBulbLog(String log) {

        }

        @Override
        public void updateBulbLogAndSpeak(String log) {

        }
    }

    private class PhilipsHueRunnable implements Runnable, LampBulbBase.onBulbStateChangedListener {
        public PhilipsHueRunnable() {
            mPhilipsHueInstance = new PhilipsHueLampBulb(context);
            mPhilipsHueInstance.setOnBulbStateChangedListener(this);
        }

        @Override
        public void run() {
            mPhilipsHueInstance.startSearch();
        }

        @Override
        public void searchStart() {

        }

        @Override
        public void searchEnd() {
            mHueDeviceList = ((PhilipsHueLampBulb) mPhilipsHueInstance).getPhilipsHueLightList();
            if (mHueDeviceList != null && mHueDeviceList.size() > 0) {
                for (int i = 0; i < mHueDeviceList.size(); i++) {
                    Light light = new Light();
                    PHLight phLight = mHueDeviceList.get(i);
                    light.setId(i);
                    light.setName(phLight.getName());
                    light.setType(Light.PHILIPSHUE);
                    if (phLight.getLastKnownLightState().isOn()) {
                        light.setState(Light.POWER_ON);
                    } else {
                        light.setState(Light.POWER_OFF);
                    }
                    lights.add(light);
                }
            }
            isPhilipsHueSearching = false;
            sendLightsList();
        }

        @Override
        public void updateBulbLog(String log) {

        }

        @Override
        public void updateBulbLogAndSpeak(String log) {

        }
    }
}
