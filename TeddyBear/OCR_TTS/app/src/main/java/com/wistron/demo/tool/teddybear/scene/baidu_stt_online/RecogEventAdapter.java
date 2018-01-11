package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;

/**
 * Created by ivanjlzhang on 17-9-7.
 */

public class RecogEventAdapter implements EventListener {

    private static final String TAG = "RecogEventAdapter";

    private IRecogListener listener;

    public RecogEventAdapter(IRecogListener listener){this.listener = listener;}

    protected String currentJson;

    @Override
    public void onEvent(String name, String params, byte[] data, int i, int i1) {
        currentJson = params;
        String logMessage =  "name:" + name + "; params:" + params;
        Log.i(TAG, logMessage);

        if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)){
            listener.onAsrReady();
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)){
            listener.onAsrBegin();
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)){
            listener.onAsrEnd();
        }
        else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)){
            RecogResult recogResult = RecogResult.parseJson(params);
            // 取出临时识别结果
            String[] results = recogResult.getResultRecognition();
            if(recogResult.isFinalResult()){
                listener.onAsrFinalResult(results, recogResult);
            }
            else if(recogResult.isPartialResult()){
                listener.onAsrPartialResult(results, recogResult);
            }
        }
        else  if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)){
            // 识别结束
            RecogResult recogResult = RecogResult.parseJson(params);
            if(recogResult.hasError()){
                int errorCode = recogResult.getError();
                Log.e(TAG, "asr error " + params);
                listener.onAsrFinishError(errorCode, BD_stt_error.recogError(errorCode), recogResult.getDesc());
            }else{
                listener.onAsrFinish(recogResult);
            }
        }else if(name.equals(SpeechConstant.CALLBACK_EVENT_ASR_EXIT)){
            listener.onAsrExit();
        }
    }
}
