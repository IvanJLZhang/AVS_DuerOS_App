package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.util;


import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedWifiDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aaron on 16-9-12.
 */
public class JSONUtil {
    public static final String COMMAND_KEY = "command";
    public static final String WIFI_LIST_KEY = "wifi_list_key";
    public static final String SECURITY_MODE_KEY = "security_mode_key";
    public static final String PASSWORD_KEY = "password_key";
    public static final String TIMEOUT_KEY = "timeout_key";
    public static final String CONNECT_RESULT_KEY = "connect_result_key";
    public static final String SSID = "ssid";
    public static final String LEVEL = "level";
    public static final String SECURITY = "security";

    public static void setCommand(JSONObject jsonObject, int command) {
        try {
            jsonObject.put(COMMAND_KEY, command);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setConnectCommand(JSONObject jsonObject, int securityMode, String ssid, String password, int timeout) {
        try {
            jsonObject.put(COMMAND_KEY, CommandUtil.CONNECT_WIFI);
            jsonObject.put(SECURITY_MODE_KEY, securityMode);
            jsonObject.put(SSID, ssid);
            jsonObject.put(PASSWORD_KEY, password);
            jsonObject.put(TIMEOUT_KEY, timeout);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static int getCommand(JSONObject jsonObject) {
        try {
            return jsonObject.getInt(COMMAND_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return CommandUtil.ERROR;
        }
    }

    public static List<ScannedWifiDevice> getWifiDeviceList(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray(WIFI_LIST_KEY);
            List<ScannedWifiDevice> devices = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                ScannedWifiDevice device = new ScannedWifiDevice();
                device.setDisplayName(jsonArray.getJSONObject(i).getString(SSID));
                device.setLevel(jsonArray.getJSONObject(i).getInt(LEVEL));
                device.setSecurity(jsonArray.getJSONObject(i).getString(SECURITY));
                devices.add(device);
            }
            return devices;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getConnectResult(JSONObject jsonObject) {
        try {
            return jsonObject.getInt(CONNECT_RESULT_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return CommandUtil.ERROR;
        }
    }
}
