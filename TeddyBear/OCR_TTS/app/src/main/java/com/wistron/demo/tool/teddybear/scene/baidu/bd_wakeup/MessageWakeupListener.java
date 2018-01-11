package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.voiceactivation.VwuService;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public class MessageWakeupListener implements IWakeupListener {

    private Handler handler;

    private static final String TAG = "MessageWakeupListener";

    public MessageWakeupListener(Handler handler){
        this.handler = handler;
    }

    @Override
    public void onSuccess(String word, WakeupResult result) {
        Log.i(TAG, "wakeup sucess: " + word);
        SendWakeupMessage(word);
    }

    @Override
    public void onStop() {
        Log.i(TAG, "wakeup service stopped.");
    }

    @Override
    public void onError(int errorCode, String errorMsg, WakeupResult result) {
        Log.e(TAG, "wakeup error.errorCode: " + errorCode+ "errormsg:" + errorMsg);
    }

    @Override
    public void onAsrAudio(byte[] data, int offset, int length) {

    }

    public void SendWakeupMessage(String word){
        Message message = handler.obtainMessage();
        message.what = SceneActivity.MSG_SPEAKING_BUTTON;
        int model = VwuService.SELECTED_PROVIDER_TEDDY;
        if(word.equals(BDWakeupService.BD_WAKEUP_KEYWORD_ALEXA))
            model = VwuService.SELECTED_PROVIDER_ALEXA;
        else if(word.equals(BDWakeupService.BD_WAKEUP_KEYWORD_BAIDU))
            model = VwuService.SELECTED_PROVIDER_BAIDU;
        else if(word.equals(BDWakeupService.BD_WAKEUP_KEYWORD_TEDDY))
            model = VwuService.SELECTED_PROVIDER_TEDDY;

        message.arg1 = model;

        handler.sendMessage(message);
    }
}
