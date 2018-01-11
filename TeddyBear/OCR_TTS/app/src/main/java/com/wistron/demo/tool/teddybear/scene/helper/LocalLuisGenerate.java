package com.wistron.demo.tool.teddybear.scene.helper;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by king on 17-3-16.
 */

public class LocalLuisGenerate {
    // Music
    private static final String PLAY = "play";
    private static final String LOOP = "loop";
    private static final String MUSIC = "music";
    private static final String PLAY_MUSIC = "play music";
    private static final HashMap<String, String> scenarioActions = new HashMap<String, String>() {
        {
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PREVIOUS, "previous, back up");
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_STOP, "halt,finish,stop");
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_PAUSE, "suspend,pause");
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_NEXT, "skip ahead,next");
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_RESUME, "resume,start");

        }
    };
    // Adjust volume
    private static final String TIMES = "times";
    private static final HashMap<String, String> adjustVolume = new HashMap<String, String>() {
        {
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLDOWN, "decrease volume, volume down");
            put(LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_VOLUP, "increase volume,volume up");

        }
    };
    // DateTime
    private static final String KEYWORD_WHAT = "what";
    private static final String KEYWORD_TIME = "time";
    private static final String KEYWORD_DATE = "date";
    private static final String KEYWORD_DAY = "day";


    public static String generateLocalLuisResult(String query) {
        JSONObject rootObject = new JSONObject();
        try {
            // query
            rootObject.put(LuisHelper.TAG_QUERY, query);
            // topScoringIntent
            JSONObject topScoringIntentObject = new JSONObject();
            topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_NONE);
            JSONArray entitiesArray = new JSONArray();
            /*if ((query.startsWith(PLAY) || query.startsWith(LOOP)) && query.contains(MUSIC)) {
                String musicName = query.substring(query.indexOf(MUSIC) + MUSIC.length()).trim();
                topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_MUSIC);
                if (query.startsWith(PLAY)) {
                    entitiesArray.put(new JSONObject()
                            .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_START)
                            .put(LuisHelper.TAG_ENTITY, PLAY));
                } else {
                    entitiesArray.put(new JSONObject()
                            .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_REPEAT)
                            .put(LuisHelper.TAG_ENTITY, LOOP));
                }
                if (!TextUtils.isEmpty(musicName)) {
                    entitiesArray.put(new JSONObject()
                            .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_MUSIC_NAME)
                            .put(LuisHelper.TAG_ENTITY, musicName));
                }
            }*/
            if (query.contains(PLAY_MUSIC)) {
                String musicName = query.substring(query.indexOf(PLAY_MUSIC) + PLAY_MUSIC.length()).trim();
                topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_MUSIC);
                entitiesArray.put(new JSONObject()
                        .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_SCENARIO_ACTION_START)
                        .put(LuisHelper.TAG_ENTITY, PLAY_MUSIC));
                if (!TextUtils.isEmpty(musicName)) {
                    entitiesArray.put(new JSONObject()
                            .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_MUSIC_NAME)
                            .put(LuisHelper.TAG_ENTITY, musicName));
                }
            } else if (query.contains(KEYWORD_WHAT) &&
                    (query.contains(KEYWORD_DATE) || query.contains(KEYWORD_TIME) || query.contains(KEYWORD_DAY))) {
                topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_DATETIME);
                entitiesArray.put(new JSONObject()
                        .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_COMMON_RESERVED_1)
                        .put(LuisHelper.TAG_ENTITY, query.contains(KEYWORD_TIME) ? KEYWORD_TIME : (query.contains(KEYWORD_DAY) ? KEYWORD_DAY : KEYWORD_DATE)));
            } else {
                boolean isMatched = false;
                // Scenario actions
                for (String key : scenarioActions.keySet()) {
                    if (scenarioActions.get(key).contains(query)) {
                        topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_SCENARIO_ACTIONS);
                        entitiesArray.put(new JSONObject()
                                .put(LuisHelper.TAG_TYPE, key)
                                .put(LuisHelper.TAG_ENTITY, query));
                        isMatched = true;
                        break;
                    }
                }
                // Adjust volume
                if (!isMatched) {
                    for (String key : adjustVolume.keySet()) {
                        String[] values = adjustVolume.get(key).split(",");
                        for (String value : values) {
                            if (query.contains(value)) {
                                int adjustTimes;
                                // 4 times volume up
                                if (query.contains(TIMES)) {
                                    String tempString = query.substring(0, query.indexOf(TIMES)).trim();
                                    if (tempString.contains(" ")) {
                                        tempString = tempString.substring(tempString.lastIndexOf(" ")).trim();
                                    }
                                    try {
                                        adjustTimes = Integer.parseInt(tempString);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        adjustTimes = 1;
                                    }
                                } else {
                                    adjustTimes = 1;
                                }

                                topScoringIntentObject.put(LuisHelper.TAG_INTENT, LuisHelper.INTENT_ADJUST_VOLUME);
                                entitiesArray.put(new JSONObject()
                                        .put(LuisHelper.TAG_TYPE, LuisHelper.ENTITIES_TYPE_BUILTIN_NUMBER)
                                        .put(LuisHelper.TAG_ENTITY, String.valueOf(adjustTimes)));
                                entitiesArray.put(new JSONObject()
                                        .put(LuisHelper.TAG_TYPE, key)
                                        .put(LuisHelper.TAG_ENTITY, value));
                                isMatched = true;
                                break;
                            }
                        }
                        if (isMatched) {
                            break;
                        }
                    }
                }
            }
            rootObject.put(LuisHelper.TAG_TOP_SCORING_INTENT, topScoringIntentObject);
            if (entitiesArray.length() > 0) {
                rootObject.put(LuisHelper.TAG_ENTITIES, entitiesArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return rootObject.toString();
    }
}
