package com.wistron.demo.tool.teddybear.wifi_setup.util;

import android.net.wifi.ScanResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public static void setScanWifiList(JSONObject jsonObject, List<ScanResult> scanResults) {
        JSONArray jsonArray = new JSONArray();
        try {
            for (ScanResult result : scanResults) {
                JSONObject jb = new JSONObject();

                jb.put(SSID, result.SSID);
                jb.put(LEVEL, result.level);
                jb.put(SECURITY, result.capabilities);
                jsonArray.put(jb);
            }
            jsonObject.accumulate(WIFI_LIST_KEY, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setConnectResult(JSONObject jsonObject, int result) {
        try {
            jsonObject.put(CONNECT_RESULT_KEY, result);
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

    public static int getTimeout(JSONObject jsonObject) {
        try {
            return jsonObject.getInt(TIMEOUT_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return 20000;
        }
    }

    public static int getSecurityMode(JSONObject jsonObject) {
        try {
            return jsonObject.getInt(SECURITY_MODE_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String getSSID(JSONObject jsonObject) {
        try {
            return jsonObject.getString(SSID);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPassword(JSONObject jsonObject) {
        try {
            return jsonObject.getString(PASSWORD_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
