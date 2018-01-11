package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

import android.content.Context;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public class BDWakeupService {
    private static boolean isInited = false;


    public static String[] BD_WAKEUP_KEYWORDS = {
      "小度你好","阿莱克丝","泰迪泰迪"
    };
    public static final String CONFIG_KEY_FOR_WAKEUP_WORD_TEDDY = "TeddyBear";
    public static final String CONFIG_KEY_FOR_WAKEUP_WORD_ALEXA = "Alexa";
    public static final String CONFIG_KEY_FOR_WAKEUP_WORD_BAIDU = "Baidu";
    public static final String CONFIG_KEY_FOR_WAKEUP_WORD_GOOGLE = "Google";

    public static String BD_WAKEUP_KEYWORD_BAIDU = BD_WAKEUP_KEYWORDS[0];
    public static String BD_WAKEUP_KEYWORD_ALEXA = BD_WAKEUP_KEYWORDS[1];
    public static String BD_WAKEUP_KEYWORD_TEDDY = BD_WAKEUP_KEYWORDS[2];
    public static String BD_WAKEUP_KEYWORD_GOOGLE = "";

    private EventManager wp;
    private EventListener eventListener;

    private static final String TAG = "BDWakeupService";

    public BDWakeupService(Context context, EventListener eventListener){
        if(isInited){
            Log.e(TAG, "please call release first.");
            throw new RuntimeException("please call release first.");
        }
        isInited = true;
        initWakeupWordsParams();
        this.eventListener = eventListener;
        wp = EventManagerFactory.create(context, "wp");
        wp.registerListener(eventListener);
    }


    public BDWakeupService(Context context, IWakeupListener eventListener){
        this(context, new WakeupEventAdapter(eventListener));
    }
    private void initWakeupWordsParams(){
        Map<String, String> params = SceneCommonHelper.getSingleParameters(SceneCommonHelper.STORAGE_BAIDU_CONFIG_FOLDER + "wakeup_words");
        if(params != null && params.size() > 0){
            for (String key:params.keySet()) {
                if(key.equals(CONFIG_KEY_FOR_WAKEUP_WORD_ALEXA)){
                    BD_WAKEUP_KEYWORD_ALEXA = params.get(key);
                    Log.i(TAG, "wake up word for service: " + CONFIG_KEY_FOR_WAKEUP_WORD_ALEXA + "=" + BD_WAKEUP_KEYWORD_ALEXA);
                }else if(key.equals(CONFIG_KEY_FOR_WAKEUP_WORD_BAIDU)){
                    BD_WAKEUP_KEYWORD_BAIDU = params.get(key);
                    Log.i(TAG, "wake up word for service: " + CONFIG_KEY_FOR_WAKEUP_WORD_BAIDU + "=" + BD_WAKEUP_KEYWORD_BAIDU);
                }else if(key.equals(CONFIG_KEY_FOR_WAKEUP_WORD_TEDDY)){
                    BD_WAKEUP_KEYWORD_TEDDY = params.get(key);
                    Log.i(TAG, "wake up word for service: " + CONFIG_KEY_FOR_WAKEUP_WORD_TEDDY + "=" + BD_WAKEUP_KEYWORD_TEDDY);
                }else if(key.equals(CONFIG_KEY_FOR_WAKEUP_WORD_GOOGLE)){
                    BD_WAKEUP_KEYWORD_GOOGLE = params.get(key);
                    Log.i(TAG, "wake up word for service: " + CONFIG_KEY_FOR_WAKEUP_WORD_GOOGLE + "=" + BD_WAKEUP_KEYWORD_GOOGLE);
                }
            }
        }
    }

    public void start(Map<String, Object> params){
        String json = new JSONObject(params).toString();
        Log.i(TAG, "start wakeup, wakeup params: " + json);

        wp.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);
    }

    public void stop(){
        Log.i(TAG, "stop wakeup");
        wp.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
    }

    public void release(){
        stop();
        wp.unregisterListener(eventListener);
        wp = null;
        isInited = false;
    }
}
