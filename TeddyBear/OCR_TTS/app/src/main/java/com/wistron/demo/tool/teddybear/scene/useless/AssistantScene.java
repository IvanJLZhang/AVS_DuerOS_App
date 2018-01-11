package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
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
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by king on 16-5-24.
 */
public class AssistantScene extends SceneBase {
    public static final String ASSISTANT_KEY_START = "hi assistant";
    private String mScene;

    private SimpleDateFormat yyyyMMddDateFormat;

    private SendToLUISTask mSendToLUISTask;
    private WeatherQueryTask mWeatherQueryTask;
    private PlaceQueryTask mPlaceQueryTask;

    private boolean isMusicScene = false;

    // Music assistant
    private MediaPlayer mPlayer;
    private static int mMusicIndex = 0;

    public AssistantScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    public AssistantScene(Context context, Handler mMainHandler, String scene) {
        super(context, mMainHandler);
        mScene = scene.trim();
        yyyyMMddDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Log.i("King", "AssistantScene mScene = " + mScene);
    }

    public void resetScene(String scene) {
        mScene = scene.trim();
    }

    @Override
    public void stop() {
        super.stop();
        if (mSendToLUISTask != null) {
            mSendToLUISTask.cancel(true);
        }
        stopMusic();
        if (mWeatherQueryTask != null) {
            mWeatherQueryTask.cancel(true);
        }
        if (mPlaceQueryTask != null) {
            mPlaceQueryTask.cancel(true);
        }
    }

    @Override
    public void simulate() {
        super.simulate();
        sendToLUIS(mScene);
    }

    private void sendToLUIS(String displayText) {
        Log.i("King", "question = " + displayText);
        SceneCommonHelper.openLED();

        mSendToLUISTask = new SendToLUISTask();
        mSendToLUISTask.execute(displayText);
    }

    // Send to LUIS
    private class SendToLUISTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String appId = context.getString(R.string.luis_main_app_id);
            String subscriptionKey = context.getString(R.string.luis_subscription_key);
            String message = params[0];
            BufferedReader reader = null;
            StringBuilder outputBuilder = new StringBuilder();

            HttpClient httpclient = new DefaultHttpClient();
            try {
                String getParams = appId +
                        "?subscription-key=" + subscriptionKey +
                        "&q=" + URLEncoder.encode(message, "UTF-8").replaceAll("\\+", "%20") +
                        "&timezoneOffset=0.0&verbose=true";
                HttpGet request = new HttpGet("https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + getParams);

                HttpResponse response = httpclient.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();
                Log.i("King", "AssistantActivity responseCode = " + responseCode);
                if (responseCode == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        /*Log.i("King", EntityUtils.toString(entity));*/
                        reader = new BufferedReader(new InputStreamReader(entity.getContent(), "UTF-8"));
                        String line;
                        while (null != (line = reader.readLine())) {
                            outputBuilder.append(line);
                        }
                        String result = outputBuilder.toString();
                        Log.i("King", "AssistantActivity result = " + result);
                        return result;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "AssistantActivity result = " + e.getMessage());
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            if (!TextUtils.isEmpty(response)) {
                /*analyzeIntent(response);*/
            } else {
                toSpeakThenDone(getString(R.string.luis_assistant_cmd_empty));
                resumeMusic();
            }
        }
    }

    /*private void analyzeIntent(String response) {
        *//* Music *//*
        String musicName = null;
        String musicPath = null;
        *//* Weather *//*
        String weatherCity = null;
        String weatherRange = null;
        *//* Place *//*
        String fromCity = null;
        String toCity = null;

        boolean isIgnoreAlarm = true;
        try {
            JSONObject toJason = new JSONObject(response);
            if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                JSONArray intents = toJason.getJSONArray(LuisHelper.TAG_INTENTS);
                String intent = intents.getJSONObject(0).getString(LuisHelper.TAG_INTENT);
                JSONArray entities = toJason.getJSONArray(LuisHelper.TAG_ENTITIES);
                Log.i("King", "AssistantActivity:: intent = " + intent);
                switch (intent) {
                    case LuisHelper.INTENT_ALARM_SET_ALARM:
                        isMusicScene = false;
                        String date = null, hour = null, minute = null;
                        String weekDay = null, duration = null;
                        String alarmTitle = null;

                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            Log.i("King", "AssistantActivity:: entitiesType = " + entitiesType);
                            if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_START_TIME)) {
                                JSONObject resolution = entities.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                                String resolutionType = resolution.getString(LuisHelper.TAG_RESOLUTION_TYPE);
                                Log.i("King", "AssistantActivity:: resolutionType = " + resolutionType);
                                if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_TIME)) {
                                    // reserve to time
                                *//* "time": "2015-10-17T04:17" *//*
                                *//* "time": "XXXX-WXX-1T07" *//*
                                *//*String[] dateTime = resolution.getString(LuisHelper.TAG_TIME).split(LuisHelper.SPLIT_TAG_T);
                                Log.i("King", "AssistantActivity:: dateTime = " + dateTime[0] + "T" + dateTime[1]);
                                if (dateTime.length >= 2) {
                                    if (dateTime[0].startsWith("XXXX-WXX")) {
                                        weekDay = dateTime[0];
                                    }else {
                                        date = dateTime[0];
                                    }
                                    String[] hourMinute = dateTime[1].split(LuisHelper.SPLIT_TAG_COLON);
                                    if (hourMinute.length >= 2) {
                                        Log.i("King", "AssistantActivity:: hourMinute = " + hourMinute[0] + ":" + hourMinute[1]);
                                        hour = hourMinute[0];
                                        minute = hourMinute[1];
                                    }else if (hourMinute.length == 1){
                                        Log.i("King", "AssistantActivity:: hourMinute = " + hourMinute[0]);
                                        hour = hourMinute[0];
                                    }
                                }*//*
                                    String time = resolution.getString(LuisHelper.TAG_TIME);
                                    if (!time.contains(LuisHelper.SPLIT_TAG_PT) && time.contains(LuisHelper.SPLIT_TAG_T)) {
                                        boolean isNeedChangeHourByTimeZone = false;
                                        if (!time.startsWith(LuisHelper.SPLIT_TAG_T)) {
                                            isNeedChangeHourByTimeZone = true;
                                        }
                                        time = time.substring(time.indexOf(LuisHelper.SPLIT_TAG_T));
                                        if (time.contains(LuisHelper.SPLIT_TAG_COLON)) {
                                            hour = time.substring(1, time.indexOf(LuisHelper.SPLIT_TAG_COLON)).trim();
                                            minute = time.substring(time.indexOf(LuisHelper.SPLIT_TAG_COLON) + 1).trim();
                                        } else {
                                            hour = time.substring(1).trim();
                                        }
                                        Log.i("King", "isNeedChangeHourByTimeZone = " + isNeedChangeHourByTimeZone + ", hour = " + hour);
                                        if (isNeedChangeHourByTimeZone && !TextUtils.isEmpty(hour)) {
                                            TimeZone timeZone = TimeZone.getDefault();
                                            int hourOffset = timeZone.getRawOffset() / (1 * 60 * 60 * 1000);
                                            hour = String.valueOf(Integer.parseInt(hour) + hourOffset);
                                            Log.i("King", "new hour = " + hour);
                                        }
                                        isIgnoreAlarm = false;
                                    }
                                } else if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_DURATION)) {
                                    // reserve for date
                                *//* "duration": "PT1H30M" *//*
                                *//*duration = resolution.getString(LuisHelper.TAG_DURATION).substring(2);
                                Log.i("King", "AssistantActivity:: hourMinute = " + duration);
                                Calendar calendar = Calendar.getInstance();
                                if (duration.contains(LuisHelper.SPLIT_TAG_H)){
                                    int tempHour = (calendar.get(Calendar.HOUR_OF_DAY) +
                                            Integer.parseInt(duration.substring(0, duration.indexOf(LuisHelper.SPLIT_TAG_H)))) % 24;
                                    hour = String.valueOf(tempHour);
                                    duration = duration.substring(duration.indexOf(LuisHelper.SPLIT_TAG_H)+1);
                                }
                                if (duration.contains(LuisHelper.SPLIT_TAG_M)){
                                    int tempMinute = (calendar.get(Calendar.MINUTE) +
                                            Integer.parseInt(duration.substring(0, duration.indexOf(LuisHelper.SPLIT_TAG_M)))) % 60;
                                    minute = String.valueOf(tempMinute);
                                }*//*
                                }
                            } else if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_START_DATE)) {
                                // reserve for date
                            *//* "time": "XXXX-WXX-1T07" *//*
                            *//* "date": "2015-10-18" *//*
                            *//*JSONObject resolution = entities.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                            String resolutionType = resolution.getString(LuisHelper.TAG_RESOLUTION_TYPE);
                            Log.i("King", "AssistantActivity:: resolutionType = " + resolutionType);
                            if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_DATE)){
                                date = resolution.getString(LuisHelper.TAG_DATE);
                                Log.i("King", "AssistantActivity:: date = " + date);
                            }else if (resolutionType.equals(LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_TIME)){
                                weekDay = resolution.getString(LuisHelper.TAG_TIME);
                                Log.i("King", "AssistantActivity:: weekDay = "+weekDay);
                            }*//*
                            } else if (entitiesType.equals(LuisHelper.ENTITIES_TYPE_ALARM_TITLE)) {
                                alarmTitle = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }

                        if (TextUtils.isEmpty(minute)) {
                            minute = "00";
                        }

                    *//*if (!TextUtils.isEmpty(date)){
                        Calendar calendar = Calendar.getInstance();
                        long startTime = calendar.getTimeInMillis();

                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddHHmm");
                        long endTime = format.parse(date+hour+minute).getTime();

                        Log.i("King", "duration time(minutes) = "+(endTime - startTime)/(1000*60));
                        if ((endTime - startTime)/(1000*60) > 24*60){ // one day
                            isIgnoreAlarm = true;
                        }
                    }*//*

                        if (!isIgnoreAlarm) {
                            Intent setAlarmIntent = null;
                            setAlarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
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
                        *//*if (!TextUtils.isEmpty(weekDay)){
                            Log.i("King", "weekDay = "+weekDay);
                            if (Integer.parseInt(weekDay) == Calendar.SUNDAY){
                                weekDay = String.valueOf(7);
                            }else {
                                weekDay = String.valueOf(Integer.parseInt(weekDay) -1);
                            }
                            setAlarmIntent.putExtra(AlarmClock.EXTRA_DAYS, new ArrayList<>(Integer.parseInt(weekDay)));
                        }*//*
                            if (setAlarmIntent != null && setAlarmIntent.resolveActivity(context.getPackageManager()) != null) {
                                context.startActivity(setAlarmIntent);
                                toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_success), alarmTitle != null ? alarmTitle : ""));
                            } else {
                                toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_fail), alarmTitle != null ? alarmTitle : ""));
                            }
                        } else {
                            toSpeakThenDone(String.format(getString(R.string.luis_assistant_alarm_fail), alarmTitle != null ? alarmTitle : ""));
                        }

                        // For debug
                        *//*openMic();*//*
                        break;
                    case LuisHelper.INTENT_MUSIC_PLAY:
                        isMusicScene = true;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_MUSIC_SONG_NAME.equals(entitiesType)) {
                                musicName = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }

                        if (TextUtils.isEmpty(musicName)) {
                            musicPath = getMusic(mMusicIndex);
                        } else {
                            musicPath = getMusic(musicName);
                        }
                        if (!TextUtils.isEmpty(musicPath)) {
                            playMusic(musicPath, false);
                        } else {
                            toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                        }
                        break;
                    case LuisHelper.INTENT_MUSIC_REPEAT:
                        isMusicScene = true;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_MUSIC_SONG_NAME.equals(entitiesType)) {
                                musicName = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }

                        if (TextUtils.isEmpty(musicName)) {
                            musicPath = getMusic(mMusicIndex);
                        } else {
                            musicPath = getMusic(musicName);
                        }
                        if (!TextUtils.isEmpty(musicPath)) {
                            playMusic(musicPath, true);
                        } else {
                            toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                        }
                        break;
                    case LuisHelper.INTENT_MUSIC_PAUSE:
                        isMusicScene = true;
                        pauseMusic();
                        break;
                    case LuisHelper.INTENT_MUSIC_RESUME:
                        isMusicScene = true;
                        resumeMusic();
                        break;
                    case LuisHelper.INTENT_MUSIC_PREVIOUS:
                    case LuisHelper.INTENT_MUSIC_NEXT:
                        isMusicScene = true;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_MUSIC_SONG_NAME.equals(entitiesType)) {
                                musicName = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }

                        if (TextUtils.isEmpty(musicName)) {
                            if (intent.equals(LuisHelper.INTENT_MUSIC_PREVIOUS)) {
                                mMusicIndex--;
                            } else if (intent.equals(LuisHelper.INTENT_MUSIC_NEXT)) {
                                mMusicIndex++;
                            }
                            musicPath = getMusic(mMusicIndex);
                        } else {
                            musicPath = getMusic(musicName);
                        }
                        if (!TextUtils.isEmpty(musicPath)) {
                            playMusic(musicPath, true);
                        } else {
                            toSpeakThenDone(getString(R.string.luis_assistant_music_can_not_found));
                        }
                        break;
                    case LuisHelper.INTENT_WEATHER_CHECK:
                        isMusicScene = false;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_WEATHER_ABSOLUTE_LOCATION.equals(entitiesType)) {
                                weatherCity = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            } else if (LuisHelper.ENTITIES_TYPE_WEATHER_DATE_RANGE.equals(entitiesType)) {
                                JSONObject resolution = entities.getJSONObject(i).getJSONObject(LuisHelper.TAG_RESOLUTION);
                                String resolutionType = resolution.getString(LuisHelper.TAG_RESOLUTION_TYPE);
                                if (LuisHelper.ENTITIES_RESOLUTION_TYPE_DATETIME_DATE.equals(resolutionType)) {
                                    weatherRange = resolution.getString(LuisHelper.TAG_DATE);
                                }
                            }
                        }
                        mWeatherQueryTask = new WeatherQueryTask();
                        mWeatherQueryTask.execute(weatherCity, weatherRange);
                        break;
                    case LuisHelper.INTENT_PLACES_GET_TRAVEL_TIME:
                    case LuisHelper.INTENT_PLACES_GET_DISTANCE:
                    case LuisHelper.INTENT_PLACES_FIND_PLACE:
                        isMusicScene = false;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_PLACES_FROM_ABSOLUTE_LOCATION.equals(entitiesType)) {
                                fromCity = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            } else if (LuisHelper.ENTITIES_TYPE_PLACES_TO_ABSOLUTE_LOCATION.equals(entitiesType)) {
                                toCity = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }
                        if (!intent.equals(LuisHelper.INTENT_PLACES_FIND_PLACE) && TextUtils.isEmpty(toCity)) {
                            toSpeakThenDone(getString(R.string.luis_assistant_places_to_is_empty));
                        } else {
                            mPlaceQueryTask = new PlaceQueryTask();
                            mPlaceQueryTask.execute(intent, fromCity, toCity);
                        }
                        break;
                    case LuisHelper.INTENT_ALARM_DELETE_ALARM:
                        *//*String alertTitle = null;
                        for (int i = 0; i < entities.length(); i++) {
                            String entitiesType = entities.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                            if (LuisHelper.ENTITIES_TYPE_ALARM_TITLE.equals(entitiesType)) {
                                alertTitle = entities.getJSONObject(i).getString(LuisHelper.TAG_ENTITY);
                            }
                        }
                        if (!TextUtils.isEmpty(alertTitle)) {
                        }*//*
                    case LuisHelper.INTENT_ALARM_FIND_ALARM:
                    case LuisHelper.INTENT_ALARM_SNOOZE:
                    case LuisHelper.INTENT_ALARM_TIME_REMAINING:
                    default:
                        toSpeakThenDone(getString(R.string.luis_assistant_function_not_support));
                        resumeMusic();
                        break;
                }
            } else {
                toSpeakThenDone(getString(R.string.luis_assistant_cmd_empty));
                resumeMusic();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }*/

    // Debug
    /*private AudioRecord recorder = null;
    private void openMic(){
        if (recorder == null){
            int bufferSize = AudioRecord.getMinBufferSize
                    (16000,
                            2,
                            AudioFormat.ENCODING_PCM_16BIT) * 3;
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000,
                    2,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
        }

        int i = recorder.getState();
        if (i == AudioRecord.STATE_INITIALIZED
                || i == AudioRecord.RECORDSTATE_STOPPED) {
            //recorder.startRecording();
            Log.i("King", "-------  Debug startRecording");
        }
    }*/

    // Music
    private String getMusic(String name) {
        String musicPath = null;
        if (!TextUtils.isEmpty(name)) {
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media
                    .EXTERNAL_CONTENT_URI, null, null, null, null);
            if (null != cursor && cursor.getCount() > 0) {
                int maxMatchCount = 0;
                while (cursor.moveToNext()) {
                    String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
                    displayName = displayName.substring(0, displayName.lastIndexOf("."));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String[] sceneSplit = name.split(" ");
                    int matchCount = 0;
                    for (String split : sceneSplit) {
                        String[] fileNameSplit = displayName.split("[\\W_]");
                        for (String temp : fileNameSplit) {
                            if (temp.toLowerCase().equals(split.toLowerCase())) {
                                matchCount++;
                            }
                        }
                    }
                    if (matchCount > maxMatchCount) {
                        maxMatchCount = matchCount;
                        mMusicIndex = cursor.getPosition();
                        musicPath = path;
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicPath;
    }

    private String getMusic(int index) {
        String musicPath = null;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media
                .EXTERNAL_CONTENT_URI, null, null, null, null);
        if (null != cursor && cursor.getCount() > 0) {
            if (index >= cursor.getCount()) {
                index = 0;
            } else if (index <= 0) {
                index = cursor.getCount() - 1;
            }
            cursor.moveToPosition(index);
            musicPath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        }
        if (cursor != null) {
            cursor.close();
        }
        return musicPath;
    }

    private void playMusic(String path, final boolean repeat) {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }

        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }
        try {
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.setLooping(repeat);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (!repeat) {
                        SceneCommonHelper.closeLED();
                    }
                }
            });
            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    SceneCommonHelper.closeLED();
                    return false;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            SceneCommonHelper.closeLED();
        }
    }

    private void stopMusic() {
        if (null != mPlayer) {
            mPlayer.stop();
            mPlayer.reset();
            mPlayer = null;
        } else {
            SceneCommonHelper.closeLED();
        }
    }

    public void pauseMusic() {
        if (null != mPlayer) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        } else {
            SceneCommonHelper.closeLED();
        }
    }

    public void resumeMusic() {
        if (isMusicScene) {
            if (null != mPlayer) {
                if (!mPlayer.isPlaying()) {
                    mPlayer.start();
                    SceneCommonHelper.openLED();
                }
            } else {
                SceneCommonHelper.closeLED();
            }
        }
    }

    // Weather
    private static final String QUERY_CITIES = "http://autocomplete.wunderground.com/aq?query=%1$s";
    private static final String QUERY_WEATHER = "http://api.wunderground.com/api/%1$s/geolookup/forecast10day%2$s.json"; // /q/autoip for auto
    private static final String WEATHER_AUTO_LOCATION = "/q/autoip";
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
            String[] weatherRange;
            if (TextUtils.isEmpty(params[1])) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                weatherRange = new String[]{
                        dateFormat.format(new Date(System.currentTimeMillis()))
                };
            } else {
                weatherRange = getWeatherRange(params[1]);
            }
            Log.i("King", "WeatherQueryTask weatherRange = " + weatherRange);

            String decodeCityQuery = null;
            HttpClient httpclient = new DefaultHttpClient();
            try {
                if (!TextUtils.isEmpty(cityName)) {
                    Log.i("King", "WeatherQueryTask cityName = " + cityName);
                    HttpGet request = new HttpGet(String.format(QUERY_CITIES, cityName.replaceAll("\\s*", "")));
                    HttpResponse response = httpclient.execute(request);
                    int responseCode = response.getStatusLine().getStatusCode();
                    Log.i("King", "WeatherQueryTask responseCode = " + responseCode);
                    if (responseCode == 200) {
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
                            Log.i("King", "WeatherQueryTask result = " + result);

                            JSONObject toJason = new JSONObject(result);
                            if (toJason.get(WUNDERGROUND_DECODE_CITY_TAG) != null) {
                                JSONArray mCities = toJason.getJSONArray(WUNDERGROUND_DECODE_CITY_TAG);
                                for (int i = 0; i < mCities.length(); i++) {
                                    String tz = mCities.getJSONObject(i).getString("tz");
                                    Log.i("King", i + " --- " + "WeatherQueryTask timeZone = " + tz + ", --> query = " + mCities.getJSONObject(i).getString("l"));
                                    if (tz.contains("/")) {
                                        decodeCityQuery = mCities.getJSONObject(i).getString("l");
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    decodeCityQuery = WEATHER_AUTO_LOCATION;
                }

                Log.i("King", "WeatherQueryTask decodeCityQuery = " + decodeCityQuery);
                if (!TextUtils.isEmpty(decodeCityQuery)) {
                    HttpGet weatherRequest = new HttpGet(String.format(QUERY_WEATHER, context.getString(R.string.wunderground_subscription_key), decodeCityQuery));
                    HttpResponse weatherResponse = httpclient.execute(weatherRequest);
                    int weatherResponseCode = weatherResponse.getStatusLine().getStatusCode();
                    Log.i("King", "WeatherQueryTask Weather responseCode = " + weatherResponseCode);
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
                            Log.i("King", "WeatherQueryTask Weather result = " + result);

                            JSONObject toJason = new JSONObject(result);
                            String weatherCountryName = null, weatherCityName = null;
                            String highTemperature = null, lowTemperature = null;
                            String weatherConditions = null;
                            StringBuilder builder = new StringBuilder();
                            if (toJason.has(LuisHelper.WU_WEATHER_TAG_LOCATION)) {
                                /*location object*/
                                JSONObject locationObject = toJason.getJSONObject(LuisHelper.WU_WEATHER_TAG_LOCATION);
                                weatherCountryName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_COUNTRY_NAME);
                                weatherCityName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_CITY);
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
                                        if (tempDate.equals(weatherRange[0])) {
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
                            if (builder.length() > (getString(R.string.luis_assistant_weather_to_read_weekend_first).length() +
                                    getString(R.string.luis_assistant_weather_to_read_weekend_last).length() - 15)) {
                                toSpeakThenDone(builder.toString());
                                return null;
                            }
                        }
                    }
                    toSpeakThenDone(getString(R.string.luis_assistant_weather_can_not_get_conditions));
                } else {
                    toSpeakThenDone(String.format(getString(R.string.luis_assistant_weather_can_not_found_city), cityName));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "WeatherQueryTask error result = " + e.getMessage());
                toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
            }
            return null;
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
            /*String intent = params[0];
            String fromCity = params[1];
            String toCity = params[2];
            String bingMapkey = context.getString(R.string.bing_map_subscription_key);
            Log.i("King", "PlaceQueryTask intent = " + intent + ", fromCity = " + fromCity + ", toCity = " + toCity);

            String decodeCityQuery = null;
            HttpClient httpclient = new DefaultHttpClient();
            try {
                String weatherCityName = null;
                if (intent.equals(LuisHelper.INTENT_PLACES_FIND_PLACE) || TextUtils.isEmpty(fromCity)) {
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
                                *//*location object*//*
                                JSONObject locationObject = toJason.getJSONObject(LuisHelper.WU_WEATHER_TAG_LOCATION);
                                weatherCityName = locationObject.getString(LuisHelper.WU_WEATHER_TAG_CITY);
                            }
                        }
                    }
                }

                if (intent.equals(LuisHelper.INTENT_PLACES_FIND_PLACE)) {
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
                                    if (intent.equals(LuisHelper.INTENT_PLACES_GET_DISTANCE)) {
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
                if (intent.equals(LuisHelper.INTENT_PLACES_GET_DISTANCE)) {
                    toSpeakThenDone(getString(R.string.luis_assistant_places_can_not_get_travel_distance));
                } else {
                    toSpeakThenDone(getString(R.string.luis_assistant_places_can_not_get_travel_duration));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "PlaceQueryTask error result = " + e.getMessage());
                toSpeakThenDone(getString(R.string.luis_assistant_weather_error));
            }*/
            return null;
        }
    }
}
