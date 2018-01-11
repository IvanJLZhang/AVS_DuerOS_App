package com.wistron.demo.tool.teddybear.light_control.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aaron on 17-2-14.
 */

public class JSONUtil {

    private static final int BASE = 0;
    public static final String COMMAND_KEY = "command";
    public static final String LIGHTS_LIST_KEY = "lights_list_key";
    public static final String STATE_KEY = "state";
    public static final String NAME_KEY = "name";
    public static final String ID_KEY = "id";
    public static final String TYPE_KEY = "type";

    private JSONUtil() {
    }

    public static void setCommand(JSONObject jsonObject, int command) {
        try {
            jsonObject.put(COMMAND_KEY, command);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static int getCommand(JSONObject jsonObject) {
        try {
            return jsonObject.getInt(COMMAND_KEY);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static void setLight(JSONObject jsonObject, Light light) {
        JSONObject jb = new JSONObject();
        try {
            jb.put(STATE_KEY, light.getState());
            jb.put(ID_KEY, light.getId());
            jb.put(NAME_KEY, light.getName());
            jb.put(TYPE_KEY, light.getType());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void setLightList(JSONObject jsonObject, List<Light> lights) {
        JSONArray jsonArray = new JSONArray();
        try {
            for (Light light : lights) {
                JSONObject jb = new JSONObject();
                jb.put(STATE_KEY, light.getState());
                jb.put(ID_KEY, light.getId());
                jb.put(NAME_KEY, light.getName());
                jb.put(TYPE_KEY, light.getType());
                jsonArray.put(jb);
            }
            jsonObject.accumulate(LIGHTS_LIST_KEY, jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static Light getLight(JSONObject jsonObject) {
        Light light = new Light();
        try {
            light.setState(jsonObject.getInt(STATE_KEY));
            light.setId(jsonObject.getInt(ID_KEY));
            light.setName(jsonObject.getString(NAME_KEY));
            light.setType(jsonObject.getString(TYPE_KEY));
            return light;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Light> getLightList(JSONObject jsonObject) {
        try {
            List<Light> lights = new ArrayList<>();
            JSONArray jsonArray = jsonObject.getJSONArray(LIGHTS_LIST_KEY);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonLight = jsonArray.getJSONObject(i);
                    Light light = new Light();
                    light.setState(jsonLight.getInt(STATE_KEY));
                    light.setId(jsonLight.getInt(ID_KEY));
                    light.setName(jsonLight.getString(NAME_KEY));
                    light.setType(jsonLight.getString(TYPE_KEY));
                    lights.add(light);
                }
            }
            return lights;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
