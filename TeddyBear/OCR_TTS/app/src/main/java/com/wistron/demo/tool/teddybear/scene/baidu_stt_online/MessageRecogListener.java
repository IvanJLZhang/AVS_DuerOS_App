package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by ivanjlzhang on 17-9-8.
 */

public class MessageRecogListener extends StatusRecogListener {
    private  Handler handler;

    private  long speechEndTime;

    private  boolean needTime = true;

    public MessageRecogListener(Handler handler){this.handler = handler;}

    @Override
    public void onAsrReady() {
        super.onAsrReady();
    }

    @Override
    public void onAsrBegin() {
        super.onAsrBegin();
        sendStatusMessage("start speaking");
    }

    @Override
    public void onAsrEnd() {
        super.onAsrEnd();
        speechEndTime = System.currentTimeMillis();
        sendMessage("stop speaking");
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        sendStatusMessage("partial result: " + results[0] + ", origin json: " + recogResult.getOrigalJson());
        super.onAsrPartialResult(results, recogResult);
    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        super.onAsrFinalResult(results, recogResult);
        String message =  "recog. finish, result " + results[0];
        sendStatusMessage( message + "origin json：" + recogResult.getOrigalJson());
        if (speechEndTime > 0){
            long diffTime = System.currentTimeMillis() - speechEndTime;
            message += "。time spend: " + diffTime + " ms】";

        }
        speechEndTime = 0;
        sendMessage(results[0], status, 1);
    }

    @Override
    public void onAsrFinishError(int errorCode, String errorMessage, String descMessage) {
        super.onAsrFinishError(errorCode, errorMessage, descMessage);
        sendStatusMessage("asr error, error code:" + errorCode + "；error message:" + errorMessage + "；desc：" + descMessage);
        long diffTime = System.currentTimeMillis() - speechEndTime;
        speechEndTime = 0;
        sendMessage("asr error：" + errorMessage + "time spend: " + diffTime + "ms",status,2);
    }

    @Override
    public void onAsrExit() {
        super.onAsrExit();
        sendStatusMessage("asr.exit");
    }

    private void sendMessage(String message) {
        sendMessage(message, WHAT_MESSAGE_STATUS);
    }

    private void sendStatusMessage(String message) {
        sendMessage(message, status);
    }

    private void sendMessage(String message, int what) {
        sendMessage(message, what, -1);
    }


    private void sendMessage(String message, int what, int highlight) {
        if (needTime && what != STATUS_FINISHED) {
            message += "  ;timestamp=" + System.currentTimeMillis();
        }
        Message msg = Message.obtain();
        msg.what = what;
        msg.arg1 = status;
        msg.arg2 = highlight;

        msg.obj = message + "\n";
        Log.i("Ivan", message);
        handler.sendMessage(msg);
    }
}
