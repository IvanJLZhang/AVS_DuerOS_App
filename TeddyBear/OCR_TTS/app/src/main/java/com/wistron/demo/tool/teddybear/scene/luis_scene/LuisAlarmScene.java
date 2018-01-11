package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;

/**
 * Created by king on 16-6-7.
 */
public class LuisAlarmScene extends SceneBase {

    public LuisAlarmScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();
        if (sceneAction.equals(LuisHelper.INTENT_ALARM_SET_ALARM)) {
            setAlarm();
        }
    }

    private void setAlarm() {
        String date = null, hour = null, minute = null;
        String weekDay = null, duration = null;
        String alarmTitle = null;
        boolean isIgnoreAlarm = true;

        try {
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                Log.i("King", "AssistantActivity:: entitiesType = " + entitiesType);
                if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_TITLE)) {
                    alarmTitle = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_DATETIME_TIME)) {
                    /* "time": "2015-10-17T04:17" */
                    /* "time": "T04:17" */
                    JSONObject resolution = sceneParams.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                    String time = resolution.getString(LuisHelper.TAG_TIME);
                    if (!time.contains(LuisHelper.SPLIT_TAG_PT) && time.contains(LuisHelper.SPLIT_TAG_T)) {
                        boolean isNeedChangeHourByTimeZone = false;
                        if (!time.startsWith(LuisHelper.SPLIT_TAG_T)) {
                            isNeedChangeHourByTimeZone = true;
                        }
                        time = time.substring(time.indexOf(LuisHelper.SPLIT_TAG_T));
                        if (!time.matches("T[0-9:]{1,}")) {
                            continue;
                        } else {
                            hour = null;
                            minute = null;
                        }
                        if (time.contains(LuisHelper.SPLIT_TAG_COLON)) {
                            hour = time.substring(1, time.indexOf(LuisHelper.SPLIT_TAG_COLON)).trim();
                            minute = time.substring(time.indexOf(LuisHelper.SPLIT_TAG_COLON) + 1).trim();
                        } else {
                            hour = time.substring(1).trim();
                        }

                        Log.i("King", "isNeedChangeHourByTimeZone = " + isNeedChangeHourByTimeZone + ", hour = " + hour);
                        if (isNeedChangeHourByTimeZone && !TextUtils.isEmpty(hour)) {
                            TimeZone timeZone = TimeZone.getDefault();
                            int hourOffset = timeZone.getRawOffset() / (1 * 60 * 60 * 1000);  // +8:00
                            hour = String.valueOf(Integer.parseInt(hour) + hourOffset);
                            Log.i("King", "new hour = " + hour);
                        }
                        isIgnoreAlarm = false;
                    }
                }
            }

            if (TextUtils.isEmpty(minute)) {
                minute = "00";
            }

            if (!isIgnoreAlarm) {
                Intent setAlarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
                setAlarmIntent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
                if (!TextUtils.isEmpty(hour)) {
                    setAlarmIntent.putExtra(AlarmClock.EXTRA_HOUR, Integer.parseInt(hour)); // 0 ~ 23
                }
                if (!TextUtils.isEmpty(minute)) {
                    setAlarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, Integer.parseInt(minute));
                }
                if (!TextUtils.isEmpty(alarmTitle)) {
                    setAlarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, alarmTitle);
                }

                if (setAlarmIntent != null && setAlarmIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(setAlarmIntent);
                    toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_success), alarmTitle != null ? alarmTitle : ""));
                } else {
                    toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_fail), alarmTitle != null ? alarmTitle : ""));
                }
            } else {
                toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_fail), alarmTitle != null ? alarmTitle : ""));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }
}
