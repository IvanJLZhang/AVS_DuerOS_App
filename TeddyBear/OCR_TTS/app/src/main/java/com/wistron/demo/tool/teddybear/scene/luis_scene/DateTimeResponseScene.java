package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.NetworkAccessHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;

/**
 * Created by king on 16-11-17.
 */

public class DateTimeResponseScene extends SceneBase {
    private static final String DATETIME_ACTION_DATE_RESPONSE = "date";
    private static final String DATETIME_ACTION_TIME_RESPONSE = "time";

    private static final String URL_TO_QUERY_DATETIME = "http://api.geonames.org/timezoneJSON?lat=%1$f&lng=%2$f&username=WksKing";

    private DateTimeQueryTask mDateTimeQueryTask;

    public DateTimeResponseScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        sceneAction = DATETIME_ACTION_DATE_RESPONSE;
        String city = null;
        try {
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_PLACES_FROM_CITY.equals(entitiesType)
                        || LuisHelper.ENTITIES_TYPE_GEOGRAPHY_CITY.equals(entitiesType)) {
                    city = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else if (LuisHelper.ENTITIES_TYPE_COMMON_RESERVED_1.equals(entitiesType)) {
                    String action = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                    if (action.contains(DATETIME_ACTION_TIME_RESPONSE)) {
                        sceneAction = DATETIME_ACTION_TIME_RESPONSE;
                    }
                }
            }
            mDateTimeQueryTask = new DateTimeQueryTask();
            mDateTimeQueryTask.execute(city);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "DateTimeQueryTask error result = " + e.getMessage());
            toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (null != mDateTimeQueryTask) {
            mDateTimeQueryTask.cancel(true);
        }
    }

    private class DateTimeQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (CommonHelper.isNetworkAvailable(context)) {  // Network time
                String cityName = params[0];
                double latitude = 0, longitude = 0;
                if (TextUtils.isEmpty(cityName)) {
                    String queryLocationResult = NetworkAccessHelper.invokeNetworkGet(NetworkAccessHelper.IPAPI_QUERY_LOCATION);
                    if (!TextUtils.isEmpty(queryLocationResult)) {
                        try {
                            JSONObject toJason = new JSONObject(queryLocationResult);
                            if (toJason.has(LuisHelper.IPAPI_TAG_CITY)) {
                                cityName = toJason.getString(LuisHelper.IPAPI_TAG_CITY);
                                latitude = toJason.getDouble(LuisHelper.IPAPI_TAG_LATITUDE);
                                longitude = toJason.getDouble(LuisHelper.IPAPI_TAG_LONGITUDE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    // city to coordinate
                    String queryDestination = NetworkAccessHelper.invokeNetworkGet(String.format(NetworkAccessHelper.GET_COORDINATE_FROM_CITY, cityName.replaceAll("\\s", "%20")));
                    if (!TextUtils.isEmpty(queryDestination)) {
                        try {
                            JSONObject toJason = new JSONObject(queryDestination);
                            if (toJason.has(LuisHelper.GEONAMES_TAG_GEONAMES)) {
                                JSONObject geonamesData = toJason.getJSONArray(LuisHelper.GEONAMES_TAG_GEONAMES).getJSONObject(0);
                                latitude = Double.parseDouble(geonamesData.getString(LuisHelper.GEONAMES_TAG_LAT));
                                longitude = Double.parseDouble(geonamesData.getString(LuisHelper.GEONAMES_TAG_LNG));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    String url = String.format(URL_TO_QUERY_DATETIME, latitude, longitude);
                    Log.i("King", "DateTimeResponseScene url = " + url);
                    String getWeatherData = NetworkAccessHelper.invokeNetworkGet(url);
                    JSONObject toJason = new JSONObject(getWeatherData);
                    // "time":"2017-03-17 10:39"
                    if (toJason.has("time")) {
                        String dateTime = toJason.getString("time");
                        if (sceneAction.equals(DATETIME_ACTION_TIME_RESPONSE)) {
                            dateTime = dateTime.substring(dateTime.indexOf(" ") + 1);
                        }
                        toSpeakThenDone(String.format(getString(R.string.luis_assistant_datetime_datetime_read), dateTime, cityName));
                    } else {
                        if (sceneAction.equals(DATETIME_ACTION_TIME_RESPONSE)) {
                            toSpeakThenDone(String.format(getString(R.string.luis_assistant_datetime_datetime_time_error), cityName));
                        } else {
                            toSpeakThenDone(String.format(getString(R.string.luis_assistant_datetime_datetime_date_error), cityName));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("King", "DateTimeQueryTask error result = " + e.getMessage());
                    toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
                }
            } else { // Local time  <yyyy-mm-dd hh:mm:ss.fffffffff>
                Timestamp mTimestamp = new Timestamp(System.currentTimeMillis());
                String mTime = mTimestamp.toString();
                String dateTime = mTime.substring(0, mTime.lastIndexOf(":"));
                if (sceneAction.equals(DATETIME_ACTION_TIME_RESPONSE)) {
                    dateTime = dateTime.substring(dateTime.indexOf(" ") + 1);
                }
                toSpeakThenDone(String.format(getString(R.string.luis_assistant_datetime_local_datetime_read), dateTime));
            }
            return null;
        }
    }
}
