package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by king on 16-6-7.
 */
public class LuisPlacesScene_Backup extends SceneBase {
    private PlaceQueryTask mPlaceQueryTask;

    public LuisPlacesScene_Backup(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        sceneAction = null;
        String fromCity = null;
        String toCity = null;
        try {
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_PLACES_FROM_CITY.equals(entitiesType)) {
                    fromCity = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else if (LuisHelper.ENTITIES_TYPE_PLACES_TO_CITY.equals(entitiesType)) {
                    toCity = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else {
                    if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_PLACES_GET_DISTANCE)
                            || entitiesType.equals(LuisHelper.ENTITIES_TYPE_PLACES_GET_TRAVEL_TIME)
                            || entitiesType.equals(LuisHelper.ENTITIES_TYPE_PLACES_CURRENT_LOCATION)) {
                        sceneAction = entitiesType;
                    }
                }
            }
            if (sceneAction != null) {
                if (!sceneAction.equals(LuisHelper.ENTITIES_TYPE_PLACES_CURRENT_LOCATION) && TextUtils.isEmpty(toCity)) {
                    toSpeakThenDone(getString(R.string.luis_assistant_places_to_is_empty));
                } else {
                    mPlaceQueryTask = new PlaceQueryTask();
                    mPlaceQueryTask.execute(sceneAction, fromCity, toCity);
                }
            } else {
                toSpeakThenDone(getString(R.string.luis_assistant_cmd_empty));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mPlaceQueryTask != null) {
            mPlaceQueryTask.cancel(true);
            mPlaceQueryTask = null;
        }
    }

    // Places
    private static final String WU_QUERY_LOCATION = "http://api.wunderground.com/api/%1$s/geolookup/q/autoip.json"; // /q/autoip for auto
    private static final String Driving_Route_URL = "http://dev.virtualearth.net/REST/V1/Routes/Driving?wp.0=%1$s&wp.1=%2$s&avoid=minimizeTolls&key=%3$s";
    private static final String DRIVING_ROUTE_TAG_STATUS = "statusCode";
    private static final String DRIVING_ROUTE_TAG_ERROR_DETAILS = "errorDetails";
    private static final String DRIVING_ROUTE_TAG_RESOURCE_SETS = "resourceSets";
    private static final String DRIVING_ROUTE_TAG_RESOURCES = "resources";
    private static final String DRIVING_ROUTE_TAG_TRAVEL_DISTANCE = "travelDistance";
    private static final String DRIVING_ROUTE_TAG_TRAVEL_DURATION = "travelDuration";

    private class PlaceQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String intent = params[0];
            String fromCity = params[1];
            String toCity = params[2];
            String bingMapkey = context.getString(R.string.bing_map_subscription_key);
            Log.i("King", "PlaceQueryTask intent = " + intent + ", fromCity = " + fromCity + ", toCity = " + toCity);

            String decodeCityQuery = null;
            HttpClient httpclient = new DefaultHttpClient();
            try {
                String weatherCityName = null;
                if (intent.equals(LuisHelper.ENTITIES_TYPE_PLACES_CURRENT_LOCATION) || TextUtils.isEmpty(fromCity)) {
                    HttpGet weatherRequest = new HttpGet(String.format(WU_QUERY_LOCATION, context.getString(R.string.wunderground_subscription_key)));
                    HttpResponse weatherResponse = httpclient.execute(weatherRequest);
                    int weatherResponseCode = weatherResponse.getStatusLine().getStatusCode();
                    Log.i("King", "PlaceQueryTask Weather responseCode = " + weatherResponseCode);
                    if (weatherResponseCode == 200) {
                        HttpEntity entity = weatherResponse.getEntity();
                        if (entity != null) {
                            StringBuilder outputBuilder = new StringBuilder();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                            String line;
                            while (null != (line = reader.readLine())) {
                                outputBuilder.append(line);
                            }
                            reader.close();
                            String result = outputBuilder.toString();
                            Log.i("King", "PlaceQueryTask Weather result = " + result);

                            JSONObject toJason = new JSONObject(result);
                            StringBuilder builder = new StringBuilder();
                            if (toJason.has(LuisHelper.WU_WEATHER_TAG_LOCATION)) {
                                /*location object*/
                                JSONObject locationObject = toJason.getJSONObject(LuisHelper.WU_WEATHER_TAG_LOCATION);
                                weatherCityName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_CITY);
                            }
                        }
                    }
                }

                if (intent.equals(LuisHelper.ENTITIES_TYPE_PLACES_CURRENT_LOCATION)) {
                    if (TextUtils.isEmpty(weatherCityName)) {
                        toSpeakThenDone(getString(R.string.luis_assistant_places_find_place_no_position_to_read));
                    } else {
                        toSpeakThenDone(String.format(getString(R.string.luis_assistant_places_find_place_to_read), weatherCityName));
                    }
                    return null;
                } else if (TextUtils.isEmpty(fromCity)) {
                    if (TextUtils.isEmpty(weatherCityName)) {
                        toSpeakThenDone(getString(R.string.luis_assistant_places_from_is_empty));
                        return null;
                    } else {
                        fromCity = weatherCityName;
                        if (fromCity.toLowerCase().endsWith(" shi")) {
                            fromCity = fromCity.substring(0, fromCity.toLowerCase().lastIndexOf("shi")).trim();
                        }
                    }
                }
                Log.i("King", "PlaceQueryTask fromCity = " + fromCity);

                String uri = String.format(Driving_Route_URL, fromCity.replaceAll("\\s", "%2C"), toCity.replaceAll("\\s", "%2C"), bingMapkey);
                Log.i("King", "PlaceQueryTask requestUri = " + uri);
                HttpGet request = new HttpGet(uri);
                HttpResponse response = httpclient.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();
                Log.i("King", "PlaceQueryTask responseCode = " + responseCode + ", entity = " + response.getEntity().toString() + ", reason = " + response.getStatusLine().getReasonPhrase());
                //if (responseCode == 200) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    StringBuilder outputBuilder = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                    String line;
                    while (null != (line = reader.readLine())) {
                        outputBuilder.append(line);
                    }
                    reader.close();
                    String result = outputBuilder.toString();
                    Log.i("King", "PlaceQueryTask result = " + result);

                    JSONObject toJason = new JSONObject(result);
                    if (toJason.get(DRIVING_ROUTE_TAG_STATUS) != null) {
                        if (toJason.getInt(DRIVING_ROUTE_TAG_STATUS) == 200) {
                            JSONArray resourceSets = toJason.getJSONArray(DRIVING_ROUTE_TAG_RESOURCE_SETS);
                            if (resourceSets.length() >= 1) {
                                JSONArray resources = resourceSets.getJSONObject(0).getJSONArray(DRIVING_ROUTE_TAG_RESOURCES);
                                if (resources.length() >= 1) {
                                    Double travelDistance = resources.getJSONObject(0).getDouble(DRIVING_ROUTE_TAG_TRAVEL_DISTANCE);
                                    int travelDuration = resources.getJSONObject(0).getInt(DRIVING_ROUTE_TAG_TRAVEL_DURATION);
                                    Log.i("King", "PlaceQueryTask travelDistance = " + travelDistance + ", travelDuration = " + travelDuration);
                                    StringBuilder builder = new StringBuilder();
                                    if (intent.equals(LuisHelper.ENTITIES_TYPE_PLACES_GET_DISTANCE)) {
                                        builder.append(String.format(getString(R.string.luis_assistant_places_to_read_distance), fromCity, toCity, travelDistance.intValue()));
                                    } else {
                                        builder.append(String.format(getString(R.string.luis_assistant_places_to_read_duration), fromCity, toCity));
                                        int day = travelDuration / (24 * 60 * 60);
                                        if (day == 1) {
                                            builder.append(getString(R.string.luis_assistant_place_to_read_day));
                                        } else if (day > 1) {
                                            builder.append(String.format(getString(R.string.luis_assistant_place_to_read_days), day));
                                        }
                                        travelDuration = travelDuration % (24 * 60 * 60);
                                        int hour = travelDuration / (60 * 60);
                                        if (hour == 1) {
                                            builder.append(getString(R.string.luis_assistant_place_to_read_hour));
                                        } else if (hour > 1) {
                                            builder.append(String.format(getString(R.string.luis_assistant_place_to_read_hours), hour));
                                        }
                                        travelDuration = travelDuration % (60 * 60);
                                        int minute = travelDuration / 60;
                                        if (minute == 1) {
                                            builder.append(getString(R.string.luis_assistant_place_to_read_minute));
                                        } else if (minute > 1) {
                                            builder.append(String.format(getString(R.string.luis_assistant_place_to_read_minutes), minute));
                                        }
                                    }
                                    toSpeakThenDone(builder.toString());
                                    return null;
                                }
                            }
                        } else {
                            JSONArray errorDetails = toJason.getJSONArray(DRIVING_ROUTE_TAG_ERROR_DETAILS);
                            StringBuilder builder = new StringBuilder();
                            builder.append(getString(R.string.luis_assistant_places_error_title) + errorDetails.getString(0));
                            toSpeakThenDone(builder.toString());
                            return null;
                        }
                    }
                }
                //}
                if (intent.equals(LuisHelper.ENTITIES_TYPE_PLACES_GET_DISTANCE)) {
                    toSpeakThenDone(getString(R.string.luis_assistant_places_can_not_get_travel_distance));
                } else {
                    toSpeakThenDone(getString(R.string.luis_assistant_places_can_not_get_travel_duration));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "PlaceQueryTask error result = " + e.getMessage());
                toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
            }
            return null;
        }
    }
}
