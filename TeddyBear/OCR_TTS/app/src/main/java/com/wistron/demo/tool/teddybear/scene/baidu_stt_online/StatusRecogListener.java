package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

/**
 * Created by ivanjlzhang on 17-9-8.
 */

public class StatusRecogListener implements IRecogListener, IStatus {

    private static final String TAG = "StatusRecogListener";
    /**
     * current recog. status
     */
    protected int status = STATUS_NONE;
    @Override
    public void onAsrReady() {
        status = STATUS_READY;
    }

    @Override
    public void onAsrBegin() {
        status = STATUS_SPEAKING;
    }

    @Override
    public void onAsrEnd() {
        status = STATUS_STOPPED;
    }

    @Override
    public void onAsrPartialResult(String[] results, RecogResult recogResult) {
        status = STATUS_RECOGNITION;
    }

    @Override
    public void onAsrFinalResult(String[] results, RecogResult recogResult) {
        status = STATUS_FINISHED;
    }

    @Override
    public void onAsrFinish(RecogResult recogResult) {

    }

    @Override
    public void onAsrFinishError(int errorCode, String errorMessage, String descMessage) {
        status = STATUS_FINISHED;
    }

    @Override
    public void onAsrExit() {
        status = STATUS_NONE;
    }
}
