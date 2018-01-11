package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

import android.content.Context;

import com.baidu.speech.asr.SpeechConstant;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public class WakeupParams {


    private static final String TAG = "WakeupParams";

    private Context context;
    public  WakeupParams(final Context context){
        this.context = context;
    }

    public Map<String, Object> fetch(){
        Map<String, Object> params = new HashMap<>();

        params.put(SpeechConstant.WP_WORDS_FILE, SceneCommonHelper.STORAGE_BAIDU_CONFIG_FOLDER + "wakeup.bin");
        params.put(SpeechConstant.APP_ID, context.getString(SubscriptionKey.getmBaiduAsrAuthInfo()[0]));
        //params.put(SpeechConstant.ACCEPT_AUDIO_DATA,true);
        //params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME,true);
        // params.put(SpeechConstant.IN_FILE,"res:///com/baidu/android/voicedemo/wakeup.pcm");
        return params;
    }
}
