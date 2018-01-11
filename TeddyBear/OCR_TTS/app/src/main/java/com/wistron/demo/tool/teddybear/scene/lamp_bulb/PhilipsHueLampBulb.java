package com.wistron.demo.tool.teddybear.scene.lamp_bulb;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by king on 17-3-2.
 */

public class PhilipsHueLampBulb extends LampBulbBase {
    private static final String TAG = "PhilipsHue";

    public static final String PREF_NAME_HUE = "philips_hue";
    public static final String PREF_KEY_IP = "ip_address";
    public static final String PREF_KEY_NAME = "user_name";

    private PHHueSDK phHueSDK;
    private boolean isFirstSearch = true;

    private String mLightName;

    public PhilipsHueLampBulb(Context mContext) {
        super(mContext);
    }

    @Override
    public void startSearch() {
        // Gets an instance of the Hue SDK.
        phHueSDK = PHHueSDK.create();
        // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
        phHueSDK.setAppName(mContext.getString(R.string.app_name));
        phHueSDK.setDeviceName(Build.MODEL);
        Log.i(TAG, "Model = " + Build.MODEL);
        // Register the PHSDKListener to receive callbacks from the bridge.
        phHueSDK.getNotificationManager().registerSDKListener(listener);

        // Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
        Map<String, String> mLastBridgeInfo = getLastBridgeInfo();
        String lastIpAddress = mLastBridgeInfo.get(PREF_KEY_IP);
        String lastUsername = mLastBridgeInfo.get(PREF_KEY_NAME);
        Log.i(TAG, "lastIpAddress = " + lastIpAddress + ", lastUsername = " + lastUsername);

        // Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
        if (null != mListener) {
            mListener.searchStart();
        }
        if (!TextUtils.isEmpty(lastIpAddress)) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);

            if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
                if (null != mListener) {
                    mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_connecting));
                }
                phHueSDK.connect(lastAccessPoint);
            } else {
                endDiscovery();
            }
        } else {  // First time use, so perform a bridge search.
            isFirstSearch = false;
            doBridgeSearch();
        }
    }

    @Override
    public void turnLightState(boolean on) {
        if (TextUtils.isEmpty(mLightName)) {
            return;
        }

        mBlubsAction = on ? BlubsAction.TURN_ON_LIGHT : BlubsAction.TURN_OFF_LIGHT;
        Log.i(TAG, "turnLightState to " + on);
        startLightsControl();
    }

    public Map<String, String> getLastBridgeInfo() {
        SharedPreferences preferences = mContext.getSharedPreferences(PREF_NAME_HUE, Context.MODE_PRIVATE);
        String lastIpAddress = preferences.getString(PREF_KEY_IP, null);
        String lastUsername = preferences.getString(PREF_KEY_NAME, null);

        Map<String, String> bridgeInfo = new HashMap<>();
        bridgeInfo.put(PREF_KEY_IP, lastIpAddress);
        bridgeInfo.put(PREF_KEY_NAME, lastUsername);
        return bridgeInfo;
    }

    public void setControlLightName(String name) {
        mLightName = name;
    }

    public List<PHLight> getPhilipsHueLightList() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge == null || bridge.getResourceCache() == null) {
            return null;
        } else {
            return bridge.getResourceCache().getAllLights();
        }
    }

    @Override
    public void destroy() {
        if (phHueSDK != null) {  // Philips Hue
            if (listener != null) {
                phHueSDK.getNotificationManager().unregisterSDKListener(listener);
            }

            PHBridge bridge = phHueSDK.getSelectedBridge();
            if (bridge != null) {
                if (phHueSDK.isHeartbeatEnabled(bridge)) {
                    phHueSDK.disableHeartbeat(bridge);
                }
                phHueSDK.disconnect(bridge);
            }
            phHueSDK.disableAllHeartbeat();
        }
    }

    private void doBridgeSearch() {
        if (null != mListener) {
            mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_searching));
        }
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        // Start the UPNP Searching of local bridges.
        sm.search(true, true);
    }

    // Local SDK Listener
    private PHSDKListener listener = new PHSDKListener() {

        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
            Log.w(TAG, "Access Points Found. " + accessPoint.size());
            if (null != mListener) {
                mListener.updateBulbLog(String.format(mContext.getString(R.string.luis_log_philips_hue_access_point_found), accessPoint.size()));
            }

            if (accessPoint != null && accessPoint.size() > 0) {
                phHueSDK.getAccessPointsFound().clear();
                phHueSDK.getAccessPointsFound().addAll(accessPoint);

                connectFirstBridge(accessPoint.get(0));
            }
        }

        private void connectFirstBridge(PHAccessPoint accessPoint) {
            PHBridge connectedBridge = phHueSDK.getSelectedBridge();

            if (connectedBridge != null) {
                String connectedIP = connectedBridge.getResourceCache().getBridgeConfiguration().getIpAddress();
                if (connectedIP != null) {   // We are already connected here:-
                    phHueSDK.disableHeartbeat(connectedBridge);
                    phHueSDK.disconnect(connectedBridge);
                }
            }
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_connecting));
            }
            phHueSDK.connect(accessPoint);
            Log.i(TAG, "start to connect a accessPoint... " + accessPoint.getIpAddress() + ", " + accessPoint.getMacAddress() + ", " + accessPoint.getUsername());
        }

        @Override
        public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
            Log.w(TAG, "On CacheUpdated");
        }

        @Override
        public void onBridgeConnected(PHBridge b, String username) {
            Log.i(TAG, "onBridgeConnected...");
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_bridge_connected));
            }

            phHueSDK.setSelectedBridge(b);
            phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
            phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());

            SharedPreferences.Editor prefEditor = mContext.getSharedPreferences(PREF_NAME_HUE, Context.MODE_PRIVATE).edit();
            prefEditor.putString(PREF_KEY_IP, b.getResourceCache().getBridgeConfiguration().getIpAddress());
            prefEditor.putString(PREF_KEY_NAME, username);
            prefEditor.commit();

            endDiscovery();
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            Log.w(TAG, "Authentication Required.");
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_authentication_required));
            }

            phHueSDK.startPushlinkAuthentication(accessPoint);
            SceneCommonHelper.playSpeakingSound(mContext, SceneCommonHelper.WARN_SOUND_TYPE_WARNING, false);
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            if (((Activity) mContext).isFinishing())
                return;

            Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_connection_resumed));
            }

            phHueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
            for (int i = 0; i < phHueSDK.getDisconnectedAccessPoint().size(); i++) {
                if (phHueSDK.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    phHueSDK.getDisconnectedAccessPoint().remove(i);
                }
            }
        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_connection_lost));
            }

            if (!phHueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
                phHueSDK.getDisconnectedAccessPoint().add(accessPoint);
            }
        }

        @Override
        public void onError(int code, final String message) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);
            if (null != mListener) {
                mListener.updateBulbLog(String.format(mContext.getString(R.string.luis_log_philips_hue_error_happen), code, message));
            }

            if (code == PHHueError.NO_CONNECTION) {
                Log.w(TAG, "On No Connection");
            } else if (code == PHHueError.AUTHENTICATION_FAILED || code == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
                //
            } else if (code == PHHueError.BRIDGE_NOT_RESPONDING) {
                Log.w(TAG, "Bridge Not Responding . . . ");
                //..
                if (isFirstSearch) {  // Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
                    /*phHueSDK = PHHueSDK.getInstance();
                    PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
                    sm.search(false, false, true);*/
                    doBridgeSearch();
                    isFirstSearch = false;
                    return;
                } else {
                    if (null != mListener) {
                        mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_philips_hue_bridge_not_found));
                        endDiscovery();
                    }
                }
            } else if (code == PHMessageType.BRIDGE_NOT_FOUND) {
                if (isFirstSearch) {  // Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
                    /*phHueSDK = PHHueSDK.getInstance();
                    PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
                    sm.search(false, false, true);*/
                    doBridgeSearch();
                    isFirstSearch = false;
                    return;
                } else {
                    /*PHWizardAlertDialog.getInstance().closeProgressDialog();
                    PHHomeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            PHWizardAlertDialog.showErrorDialog(PHHomeActivity.this, message, R.string.btn_ok);
                        }
                    });*/
                    if (null != mListener) {
                        mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_philips_hue_bridge_not_found));
                        endDiscovery();
                    }
                }
            }
            SceneCommonHelper.closeLED();
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
            if (null != mListener) {
                mListener.updateBulbLog(mContext.getString(R.string.luis_log_philips_hue_parsing_errors));
            }

            for (PHHueParsingError parsingError : parsingErrorsList) {
                Log.e(TAG, "ParsingError : " + parsingError.getMessage());
            }
        }
    };

    private void endDiscovery() {
        if (listener != null) {
            phHueSDK.getNotificationManager().unregisterSDKListener(listener);
        }
        phHueSDK.disableAllHeartbeat();
        Log.i(TAG, "endDiscovery...   disableAllHeartbeat");

        if (null != mListener) {
            mListener.searchEnd();
        }
    }

    /* Connected, start to control lights */
    private int requestNumber, responseNumber, passNumber;

    private void startLightsControl() {
        requestNumber = 0;
        responseNumber = 0;
        passNumber = 0;
        Log.i(TAG, "startLightsControl...  start");

        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        Log.i(TAG, "Light size = " + allLights.size());

        boolean isFoundLight = false, isNoNeedUpdate = true;
        for (PHLight light : allLights) {
            {
                if (light.getName().equals("Hue Lamp 1")
                        || light.getName().equals("lamp one")) {
                    light.setName("bedroom");
                    bridge.updateLight(light, null);
                }
                if (light.getName().equals("Hue Lamp 2")
                        || light.getName().equals("lamp two")) {
                    light.setName("kitchen");
                    bridge.updateLight(light, null);
                }
                if (light.getName().equals("Hue Lamp 3")
                        || light.getName().equals("lamp three")) {
                    light.setName("bathroom");
                    bridge.updateLight(light, null);
                }
            }

            PHLightState lightState = new PHLightState();
            boolean isLightOn = light.getLastKnownLightState().isOn();
            Log.i(TAG, "Light = " + light.getName() + " , isOn = " + isLightOn + ", toTurnOn = " + mBlubsAction + ", mLightName = " + mLightName);
            if (mBlubsAction == BlubsAction.TURN_ON_LIGHT) { // Open light
                if (mLightName.contains("all") || mLightName.contains("any")
                        || mLightName.contains("each") || mLightName.contains("every")
                        || mLightName.contains(light.getName().toLowerCase())) {
                    isFoundLight = true;
                    if (!isLightOn) {
                        isNoNeedUpdate = false;
                        requestNumber++;
                        lightState.setOn(true);
                        bridge.updateLightState(light, lightState, lightListener);
                    }
                }
            } else if (mBlubsAction == BlubsAction.TURN_OFF_LIGHT) {  // Close light
                if (mLightName.contains("all") || mLightName.contains("any")
                        || mLightName.contains("each") || mLightName.contains("every")
                        || mLightName.contains(light.getName().toLowerCase())) {
                    isFoundLight = true;
                    if (isLightOn) {
                        isNoNeedUpdate = false;
                        requestNumber++;
                        lightState.setOn(false);
                        bridge.updateLightState(light, lightState, lightListener);
                    }
                }
            }
            /*PHLightState lightState = light.getLastKnownLightState();
            int colorTemp = lightState.getCt();

            boolean isOn = light.getLastKnownLightState().isOn();

            lightState.setCt(300);
            lightState.setOn(!isOn);*/

            // To validate your lightstate is valid (before sending to the bridge) you can use:
            // String validState = lightState.validateState();
            //bridge.updateLightState(light, lightState, lightListener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
        Log.i(TAG, "startLightsControl... isFoundLight = " + isFoundLight + ", isNoNeedUpdate = " + isNoNeedUpdate);

        if (!isFoundLight) {
            if (null != mListener) {
                mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_philips_hue_light_not_found));
            }
        } else {
            if (isNoNeedUpdate) {
                if (null != mListener) {
                    mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_philips_hue_no_need_update));
                }
            }
        }
        SceneCommonHelper.closeLED();
    }

    // If you want to handle the response from the bridge, create a PHLightListener object.
    private PHLightListener lightListener = new PHLightListener() {
        private Object tempObject = new Object();

        @Override
        public void onSuccess() {
            synchronized (tempObject) {
                responseNumber++;
                passNumber++;
                if (null != mListener) {
                    mListener.updateBulbLog("lightListener:: light set success.");
                }
                Log.w(TAG, "PHLightListener:: onSuccess -> requestNumber = " + requestNumber + ", responseNumber = " + responseNumber + ", passNumber = " + passNumber);
                if (responseNumber == requestNumber) {
                    if (passNumber == requestNumber) {
                        hueSetSuccess();
                    } else {
                        hueSetFail();
                    }
                }
            }
        }

        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
            Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {
            synchronized (tempObject) {
                Log.w(TAG, "onError " + arg0 + ", " + arg1);
                responseNumber++;
                if (null != mListener) {
                    mListener.updateBulbLog("lightListener:: onError " + arg0 + ", " + arg1);
                }
                Log.w(TAG, "PHLightListener:: onError " + arg0 + ", " + arg1 + " -> requestNumber = " + requestNumber + ", responseNumber = " + responseNumber + ", passNumber = " + passNumber);
                if (responseNumber == requestNumber) {
                    if (passNumber == requestNumber) {
                        hueSetSuccess();
                    } else {
                        hueSetFail();
                    }
                }
            }
        }

        @Override
        public void onReceivingLightDetails(PHLight arg0) {
        }

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {
        }

        @Override
        public void onSearchComplete() {
        }

        private void hueSetSuccess() {
            if (null != mListener) {
                mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_operate_success));
            }
        }

        private void hueSetFail() {
            if (null != mListener) {
                mListener.updateBulbLogAndSpeak(SceneCommonHelper.getString(mContext, R.string.luis_assistant_operate_fail));
            }
        }
    };
}
