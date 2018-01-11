package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public class WakeupEventAdapter implements EventListener {

    private static final String TAG = "WakeupEventAdapter";
    private IWakeupListener listener;
    public  WakeupEventAdapter(IWakeupListener listener){this.listener = listener;}

    @Override
    public void onEvent(String name, String params, byte[] data, int i, int i1) {
        Log.i(TAG, "wakeup name: " + name + "params:" + params);
        if(SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS.equals(name)){
            WakeupResult result = WakeupResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()){
                listener.onError(errorCode, BD_wakeup_error.WakeupError(errorCode), result);
            }else{
                String word = result.getWord();
                listener.onSuccess(word, result);
            }
        }else if(SpeechConstant.CALLBACK_EVENT_WAKEUP_ERROR.equals(name)){
            WakeupResult result = WakeupResult.parseJson(name, params);
            int errorCode = result.getErrorCode();
            if (result.hasError()){
                listener.onError(errorCode, BD_wakeup_error.WakeupError(errorCode), result);
            }
        }else if(SpeechConstant.CALLBACK_EVENT_WAKEUP_STOPED.equals(name)){
            listener.onStop();
        }else if(SpeechConstant.CALLBACK_EVENT_WAKEUP_AUDIO.equals(name)){
            listener.onAsrAudio(data, i, i1);
        }
    }
}
