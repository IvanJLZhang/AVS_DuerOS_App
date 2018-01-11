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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Created by king on 16-6-7.
 */
public class LuisWeatherScene extends SceneBase {
    private WeatherQueryTask mWeatherQueryTask;

    public LuisWeatherScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        SceneCommonHelper.openLED();

        String weatherCity = null;
        String weatherRange = null;
        try {
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_PLACES_FROM_CITY.equals(entitiesType)) {
                    weatherCity = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                } else if (LuisHelper.ENTITIES_TYPE_DATETIME_DATE.equals(entitiesType)) {
                    JSONObject resolution = sceneParams.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                    weatherRange = resolution.getString(LuisHelper.TAG_DATE);
                }
            }
            mWeatherQueryTask = new WeatherQueryTask();
            mWeatherQueryTask.execute(weatherCity, weatherRange);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "PlaceQueryTask error result = " + e.getMessage());
            toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mWeatherQueryTask != null) {
            mWeatherQueryTask.cancel(true);
            mWeatherQueryTask = null;
        }
    }

    // Weather
    //private static final String QUERY_CITIES = "http://autocomplete.wunderground.com/aq?query=%1$s";
    private static final String QUERY_WEATHER = "http://api.wunderground.com/api/%1$s/geolookup/forecast10day%3$s%2$s.json"; // /q/autoip for auto
    private static final String WUNDERGROUND_DECODE_CITY_TAG = "RESULTS";

    private String[] getWeatherRange(String date) {
        if (date.endsWith(LuisHelper.SPLIT_TAG_WE)) {
            /* "date": "2015-W42-WE" */
            String weekNumber = date.substring(date.indexOf("W") + 1, date.lastIndexOf("-"));
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.WEEK_OF_YEAR, Integer.parseInt(weekNumber));
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
            String saturday = yyyyMMddDateFormat.format(calendar.getTime());
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            String sunday = yyyyMMddDateFormat.format(calendar.getTime());
            Log.i("King", "saturday = " + saturday + ", sunday = " + sunday);
            return new String[]{saturday, sunday};
        } else {
            /* "date": "2016-06-09" */
            /* "date": "XXXX-06-10" */
            return new String[]{date};
        }
    }

    private int getDayDiff(String date) {
        int dayDiff = -1;
        try {
            Date currentDate = yyyyMMddDateFormat.parse(yyyyMMddDateFormat.format(new Date(System.currentTimeMillis())));
            Date newDate = yyyyMMddDateFormat.parse(date);
            Long diff = ((newDate.getTime() - currentDate.getTime()) / (1 * 24 * 60 * 60 * 1000));
            dayDiff = diff.intValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return dayDiff;
    }

    private class WeatherQueryTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String cityName = params[0];
            double latitude = 0, longitude = 0;
            String[] weatherRange;
            if (TextUtils.isEmpty(params[1])) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                weatherRange = new String[]{
                        dateFormat.format(new Date(System.currentTimeMillis()))
                };
            } else {
                weatherRange = getWeatherRange(params[1]);
            }
            Log.i("King", "WeatherQueryTask weatherRange = " + (weatherRange.length == 1 ? weatherRange[0] : weatherRange[0] + " ~ " + weatherRange[1]));
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

            String lang = "";
            switch (SceneCommonHelper.getSpeakingLanguageSetting(context)) {
                case CommonHelper.LanguageRegion.REGION_CHINESE_CN:
                    lang = "/lang:CN";
                    break;
                default:
                    break;
            }

            String decodeCityQuery = "/q/" + latitude + "," + longitude;
            Log.i("King", "WeatherQueryTask decodeCityQuery = " + decodeCityQuery);
            if (!TextUtils.isEmpty(decodeCityQuery)) {
                try {
                    String url = String.format(QUERY_WEATHER, context.getString(R.string.wunderground_subscription_key), decodeCityQuery, lang);
                    String getWeatherData = NetworkAccessHelper.invokeNetworkGet(url);
                    JSONObject toJason = new JSONObject(getWeatherData);

                    String weatherCountryName = null, weatherCityName = null;
                    String highTemperature = null, lowTemperature = null;
                    String weatherConditions = null;
                    StringBuilder builder = new StringBuilder();
                    if (toJason.has(LuisHelper.WU_WEATHER_TAG_LOCATION)) {
                        /*location object*/
                        JSONObject locationObject = toJason.getJSONObject(LuisHelper.WU_WEATHER_TAG_LOCATION);
                        weatherCountryName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_COUNTRY_NAME);
                        // weatherCityName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_CITY);
                        weatherCityName = cityName;
                        if (weatherRange.length == 2) {
                            builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_weekend_first), weatherCityName));
                        }
                        Log.i("King", "decode result is: countryName = " + weatherCountryName + ", cityName = " + weatherCityName);
                                /*forecast object*/
                        JSONObject forecastObject = toJason.getJSONObject(LuisHelper.WU_WEATHER_TAG_FORECAST);
                        JSONObject simpleForecastObject = forecastObject.getJSONObject(LuisHelper.WU_WEATHER_TAG_SIMPLEFORECAST);
                        JSONArray forecastday = simpleForecastObject.getJSONArray(LuisHelper.WU_WEATHER_TAG_FORECASTDAY);
                        for (int i = 0; i < forecastday.length(); i++) {
                            JSONObject dayObject = forecastday.getJSONObject(i);
                            JSONObject dateObject = dayObject.getJSONObject(LuisHelper.WU_WEATHER_TAG_DATE);
                            int year = dateObject.getInt(LuisHelper.WU_WEATHER_TAG_YEAR);
                            int month = dateObject.getInt(LuisHelper.WU_WEATHER_TAG_MONTH);
                            int day = dateObject.getInt(LuisHelper.WU_WEATHER_TAG_DAY);
                            JSONObject highObject = dayObject.getJSONObject(LuisHelper.WU_WEATHER_TAG_HIGH);
                            highTemperature = highObject.getString(LuisHelper.WU_WEATHER_TAG_CELSIUS);
                            JSONObject lowObject = dayObject.getJSONObject(LuisHelper.WU_WEATHER_TAG_LOW);
                            lowTemperature = lowObject.getString(LuisHelper.WU_WEATHER_TAG_CELSIUS);
                            weatherConditions = dayObject.getString(LuisHelper.WU_WEATHER_TAG_CONDITIONS);
                            Log.i("King", "highTemerature = " + highTemperature + ", lowTemperature = " + lowTemperature +
                                    ", conditions = " + weatherConditions);

                            String tempDate = String.format("%1$d-%2$02d-%3$02d", year, month, day);
                            if (weatherRange.length == 1) {
                                if (tempDate.substring(tempDate.indexOf("-"))
                                        .equals(weatherRange[0].substring(weatherRange[0].indexOf("-")))) {
                                    int dayDiff = getDayDiff(tempDate);
                                    switch (dayDiff) {
                                        case 0: // Today
                                            builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_future_three_days),
                                                    getString(R.string.luis_assistant_weather_tag_today), weatherCityName, weatherConditions, highTemperature, lowTemperature));
                                            break;
                                        case 1: // Tomorrow
                                            builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_future_three_days),
                                                    getString(R.string.luis_assistant_weather_tag_tomorrow), weatherCityName, weatherConditions, highTemperature, lowTemperature));
                                            break;
                                        case 2: // the day after tomorrow
                                            builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_future_three_days),
                                                    getString(R.string.luis_assistant_weather_tag_after_tomorrow), weatherCityName, weatherConditions, highTemperature, lowTemperature));
                                            break;
                                        default: // specified day, like May 26
                                            SimpleDateFormat readDayFormat = new SimpleDateFormat("MMMM-d.", Locale.ENGLISH);
                                            String readDay = readDayFormat.format(yyyyMMddDateFormat.parse(tempDate));
                                            builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_fixed_day),
                                                    weatherCityName, readDay, weatherConditions, highTemperature, lowTemperature));
                                            break;
                                    }
                                    break;
                                }
                            } else if (weatherRange.length == 2) {
                                if (tempDate.equals(weatherRange[0])) { // Saturday
                                    builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_weekend_last),
                                            getString(R.string.luis_assistant_weather_tag_saturday), weatherConditions, highTemperature, lowTemperature));
                                } else if (tempDate.equals(weatherRange[1])) { // Sunday
                                    builder.append(String.format(getString(R.string.luis_assistant_weather_to_read_weekend_last),
                                            getString(R.string.luis_assistant_weather_tag_sunday), weatherConditions, highTemperature, lowTemperature));
                                    break;
                                }
                            }
                        }
                    }
                    Log.i("King", "To read weather result = " + builder.toString());
                    if (builder.length() > 0) {
                        toSpeakThenDone(builder.toString());
                    } else {
                        toSpeakThenDone(getString(R.string.luis_assistant_weather_can_not_get_conditions));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("King", "WeatherQueryTask error result = " + e.getMessage());
                    toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
                }
            }
            return null;
        }
    }
}
