package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.util.Log;

import java.net.URLEncoder;

/**
 * Created by king on 16-5-23.
 */
public class LuisHelper {
    public static final String SPLIT_TAG_T = "T";
    public static final String SPLIT_TAG_COLON = ":";
    public static final String SPLIT_TAG_PT = "PT";
    public static final String SPLIT_TAG_H = "H";
    public static final String SPLIT_TAG_M = "M";
    public static final String SPLIT_TAG_WE = "WE";

    public static final String TAG_QUERY = "query";
    public static final String TAG_INTENTS = "intents";
    public static final String TAG_TOP_SCORING_INTENT = "topScoringIntent";
    public static final String TAG_INTENT = "intent";
    public static final String TAG_ENTITIES = "entities";
    public static final String TAG_ENTITY = "entity";
    public static final String TAG_TYPE = "type";
    public static final String TAG_RESOLUTION = "resolution";
    public static final String TAG_RESOLUTION_TYPE = "resolution_type";
    public static final String TAG_TIME = "time";
    public static final String TAG_DATE = "date";
    public static final String TAG_DURATION = "duration";
    public static final String TAG_VALUE = "value";

    /* Main */
    public static final String INTENT_NONE = "None";
    public static final String INTENT_ALARM_SET_ALARM = "teddybear.intent.alarm.set_alarm";
    public static final String INTENT_MUSIC = "teddybear.intent.music";
    public static final String INTENT_LANGUAGE = "teddybear.intent.language";
    public static final String INTENT_WEATHER_CHECK = "teddybear.intent.weather.check_weather";
    public static final String INTENT_PLACES = "teddybear.intent.places";
    public static final String INTENT_EMOTION_FACE_DETECT = "teddybear.intent.emotion.face_detect";
    public static final String INTENT_VOICE_SPEAKER_RECOGNITION = "teddybear.intent.voice.speaker_recognition";
    public static final String INTENT_GAME_PLAY = "teddybear.intent.play_game";
    public static final String INTENT_MESSAGE_PLAY_MEMO = "teddybear.intent.message.play_memo";
    public static final String INTENT_MONITOR_MODE = "teddybear.intent.monitor_mode";
    public static final String INTENT_STORY = "teddybear.intent.story";
    public static final String INTENT_LIGHTS_CONTROL = "teddybear.intent.lights_control";
    public static final String INTENT_EMAIL_NOTIFICATION = "teddybear.intent.email";
    /* Game memo */
    public static final String INTENT_MEMO_SEND_MSG = "teddybear.memo.send_message";
    public static final String INTENT_MEMO_OVER = "teddybear.memo.over";
    public static final String INTENT_MEMO_PLAY = "teddybear.memo.play";
    public static final String INTENT_GAME_READ_ANSWER = "teddybear.game.straight_answer";
    public static final String INTENT_GAME_OVER = "teddybear.game.over";
    public static final String INTENT_GAME_REPEAT = "teddybear.game.repeat";
    /* SVA Scene */
    public static final String INTENT_SVA_SCENE = "teddybear.sva.scene";
    public static final String INTENT_DATETIME = "teddybear.intent.datetime";
    public static final String INTENT_NEWS = "teddybear.intent.news";
    public static final String INTENT_YOUTUBE_VIDEO = "teddybear.intent.youtube.video";
    public static final String INTENT_ADJUST_VOLUME = "teddybear.intent.adjust_volume";
    public static final String INTENT_SCENARIO_ACTIONS = "teddybear.intent.scenario.action";

    public static final String ENTITIES_TYPE_BUILTIN_NUMBER = "builtin.number";
    public static final String ENTITIES_TYPE_STORY_PREFIX = "teddybear.story::";
    public static final String ENTITIES_TYPE_MUSIC_PREFIX = "teddybear.action.music::";
    public static final String ENTITIES_TYPE_TITLE_NAME_PREFIX = "teddybear.item.title_name::";
    public static final String ENTITIES_TYPE_PLACES_PREFIX = "teddybear.places::";
    public static final String ENTITIES_TYPE_LANGUAGE_PREFIX = "teddybear.language::";
    public static final String ENTITIES_TYPE_ALARM_START_TIME = "teddybear.alarm.start_time";
    public static final String ENTITIES_TYPE_ALARM_START_DATE = "teddybear.alarm.start_date";
    public static final String ENTITIES_TYPE_ALARM_TITLE = "teddybear.item.title_name::alarm";
    public static final String ENTITIES_TYPE_WEATHER_DATE_RANGE = "teddybear.weather.date_range";
    public static final String ENTITIES_TYPE_DATETIME_TIME = "builtin.datetime.time";
    public static final String ENTITIES_TYPE_DATETIME_DATE = "builtin.datetime.date";
    public static final String ENTITIES_TYPE_GEOGRAPHY_CITY = "builtin.geography.city";
    public static final String ENTITIES_TYPE_BOOK_STORY_TITLE = "teddybear.item.title_name::story";
    /*public static final String ENTITIES_TYPE_MUSIC_START = "teddybear.action.music::start";
    public static final String ENTITIES_TYPE_MUSIC_STOP = "teddybear.action.music::stop";
    public static final String ENTITIES_TYPE_MUSIC_PAUSE = "teddybear.action.music::pause";
    public static final String ENTITIES_TYPE_MUSIC_RESUME = "teddybear.action.music::resume";
    public static final String ENTITIES_TYPE_MUSIC_REPEAT = "teddybear.action.music::repeat";
    public static final String ENTITIES_TYPE_MUSIC_RREVIOUS = "teddybear.action.music::previous";
    public static final String ENTITIES_TYPE_MUSIC_NEXT = "teddybear.action.music::next";*/
    public static final String ENTITIES_TYPE_MUSIC_NAME = "teddybear.item.title_name::music";
    public static final String ENTITIES_TYPE_VIDEO_NAME = "teddybear.item.title_name::video";
    public static final String ENTITIES_TYPE_PLACES_CURRENT_LOCATION = "teddybear.places::cur_location";
    public static final String ENTITIES_TYPE_PLACES_GET_DISTANCE = "teddybear.places::get_distance";
    public static final String ENTITIES_TYPE_PLACES_GET_TRAVEL_TIME = "teddybear.places::get_travel_time";
    public static final String ENTITIES_TYPE_PLACES_FROM_CITY = "teddybear.places::from_city";
    public static final String ENTITIES_TYPE_PLACES_TO_CITY = "teddybear.places::to_city";
    public static final String ENTITIES_TYPE_ON_OFF_START = "teddybear.action.on_off::start";
    public static final String ENTITIES_TYPE_ON_OFF_STOP = "teddybear.action.on_off::stop";
    public static final String ENTITIES_TYPE_STORY_LOCAL = "teddybear.story::local";
    public static final String ENTITIES_TYPE_STORY_NEW = "teddybear.story::new";
    public static final String ENTITIES_TYPE_LANGUAGE_SPEAKING = "teddybear.language::speak";
    public static final String ENTITIES_TYPE_LANGUAGE_RECOGNITION = "teddybear.language::recognition";
    public static final String ENTITIES_TYPE_LANGUAGE_CURRENT = "teddybear.language::ask";
    public static final String ENTITIES_TYPE_LANGUAGE_LANGUAGE = "teddybear.language::language";
    public static final String ENTITIES_TYPE_RESERVED_PARAM_1 = "teddybear.entity.type.reserved::param_1";
    public static final String ENTITIES_TYPE_RESERVED_PARAM_2 = "teddybear.entity.type.reserved::param_2";
    public static final String ENTITIES_TYPE_RESERVED_PARAM_3 = "teddybear.entity.type.reserved::param_3";
    public static final String ENTITIES_TYPE_COMMON_RESERVED_1 = "teddybear.entity.common::reserved1";
    public static final String ENTITIES_TYPE_COMMON_RESERVED_2 = "teddybear.entity.common::reserved2";
    public static final String ENTITIES_TYPE_COMMON_RESERVED_3 = "teddybear.entity.common::reserved3";
    public static final String ENTITIES_TYPE_COMMON_RESERVED_4 = "teddybear.entity.common::reserved4";
    public static final String ENTITIES_TYPE_EMAIL_RECIPIENTS_FILTER = "teddybear.entity.common::reserved1";
    public static final String ENTITIES_TYPE_EMAIL_SUBJECT_FILTER = "teddybear.entity.common::reserved2";
    public static final String ENTITIES_TYPE_SVA_CREATE = "teddybear.entity.common::reserved3";
    public static final String ENTITIES_TYPE_SVA_DELETE = "teddybear.entity.common::reserved4";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_PREFIX = "teddybear.scenario.action::";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_PAUSE = "teddybear.scenario.action::pause";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS = "teddybear.scenario.action::previous";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_NEXT = "teddybear.scenario.action::next";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_STOP = "teddybear.scenario.action::stop";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_RESUME = "teddybear.scenario.action::resume";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_START = "teddybear.scenario.action::start";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_REPEAT = "teddybear.scenario.action::repeat";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_VOLUP = "teddybear.scenario.action::volup";
    public static final String ENTITIES_TYPE_SCENARIO_ACTION_VOLDOWN = "teddybear.scenario.action::voldown";

    public static final String ENTITIES_RESOLUTION_TYPE_DATETIME_TIME = "teddybear.datetime.time";
    public static final String ENTITIES_RESOLUTION_TYPE_DATETIME_DATE = "teddybear.datetime.date";
    public static final String ENTITIES_RESOLUTION_TYPE_DATETIME_DURATION = "teddybear.datetime.duration";
    public static final String ENTITIES_RESOLUTION_TYPE_PLACES_TRANSPORTATION_TYPE = "teddybear.places.transportation_type";

    public static final String WU_WEATHER_TAG_RESPONSE = "response";
    public static final String WU_WEATHER_TAG_RESULTS = "results";
    public static final String WU_WEATHER_TAG_LOCATION = "location";
    public static final String WU_WEATHER_TAG_COUNTRY_NAME = "country_name";
    public static final String WU_WEATHER_TAG_CITY = "city";
    public static final String WU_WEATHER_TAG_FORECAST = "forecast";
    public static final String WU_WEATHER_TAG_SIMPLEFORECAST = "simpleforecast";
    public static final String WU_WEATHER_TAG_FORECASTDAY = "forecastday";
    public static final String WU_WEATHER_TAG_DATE = "date";
    public static final String WU_WEATHER_TAG_YEAR = "year";
    public static final String WU_WEATHER_TAG_MONTH = "month";
    public static final String WU_WEATHER_TAG_DAY = "day";
    public static final String WU_WEATHER_TAG_HIGH = "high";
    public static final String WU_WEATHER_TAG_LOW = "low";
    public static final String WU_WEATHER_TAG_CELSIUS = "celsius";
    public static final String WU_WEATHER_TAG_CONDITIONS = "conditions";

    public static final String IPAPI_TAG_CITY = "city";
    public static final String IPAPI_TAG_REGION = "region";
    public static final String IPAPI_TAG_LATITUDE = "latitude";
    public static final String IPAPI_TAG_LONGITUDE = "longitude";

    public static final String GEONAMES_TAG_GEONAMES = "geonames";
    public static final String GEONAMES_TAG_LAT = "lat";
    public static final String GEONAMES_TAG_LNG = "lng";


    public static String getLuisRequestUrl(Context context, String query, boolean isMainApp) {
        String requestUrl = null;
        String subscriptionKey = context.getString(SubscriptionKey.getLuisSubscriptionKey());
        String appId = SceneCommonHelper.getString(context, SubscriptionKey.getLuisMainAppId());
        if (!isMainApp) {
            appId = SceneCommonHelper.getString(context, SubscriptionKey.getLuisMemoGameAppId());
        }

        try {
            String getParams = appId +
                    "?subscription-key=" + subscriptionKey +
                    "&q=" + URLEncoder.encode(query, "UTF-8").replaceAll("\\+", "%20") +
                    "&timezoneOffset=0.0&verbose=true";
            requestUrl = "https://westus.api.cognitive.microsoft.com/luis/v2.0/apps/" + getParams;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "SceneActivity result = " + e.getMessage());
        }
        return requestUrl;
    }
}
