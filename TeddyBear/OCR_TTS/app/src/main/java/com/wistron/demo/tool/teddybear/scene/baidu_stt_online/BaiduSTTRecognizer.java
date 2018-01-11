package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

import android.content.Context;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.Map;

/**
 * Created by ivanjlzhang on 17-9-8.
 */

public class BaiduSTTRecognizer {
    private EventManager asr;

    private EventListener eventListener;

    private static boolean isOfflineEngineLoaded = false;

    private  static boolean isInited = false;

    private static final String TAG = "BaiduSTTRecognizer";


    public BaiduSTTRecognizer(Context context, IRecogListener eventListener) {
        this(context, new RecogEventAdapter(eventListener));
    }

    public BaiduSTTRecognizer(Context context, EventListener eventListener){
        if(isInited){
            Log.e(TAG, "no release error");
            throw new RuntimeException("no release error");
        }
        isInited = true;
        this.eventListener = eventListener;
        asr = EventManagerFactory.create(context, "asr");
        asr.registerListener(eventListener);
    }

    /**
     *
     * @param params
     */
    public void loadOfflineEngine(Map<String, Object> params) {
        String json = new JSONObject(params).toString();
        Log.i(TAG, "loadOfflineEngine params:" + json);
        asr.send(SpeechConstant.ASR_KWS_LOAD_ENGINE, json, null, 0, 0);
        isOfflineEngineLoaded = true;
    }

    public  void start(Map<String, Object> params){
        String json = new JSONObject(params).toString();
        Log.i(TAG, "asr params " + json);
        asr.send(SpeechConstant.ASR_START, json, null, 0, 0);
    }

    public void stop(){
        Log.i(TAG, "stop recording");
        asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
    }

    /**
     * 取消本次识别，取消后将立即停止不会返回识别结果。
     * cancel 与stop的区别是 cancel在stop的基础上，完全停止整个识别流程，
     */
    public void cancel() {
        Log.i(TAG, "asr.cancel");
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }


    public void release() {
        cancel();
        if (isOfflineEngineLoaded) {
            asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0);
            isOfflineEngineLoaded = false;
        }
        asr.unregisterListener(eventListener);
        asr = null;
        isInited = false;
    }
}
