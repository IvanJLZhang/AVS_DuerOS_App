package com.wistron.demo.tool.teddybear.scene.baidu_stt_online;

/**
 * Created by ivanjlzhang on 17-9-7.
 */

public interface IRecogListener {
    /**
     * ASR_START engine is ready
     */
    void onAsrReady();

    /**
     * start speaking
     */
    void onAsrBegin();

    /**
     * stop speaking or ASR_STOP send
     */
    void onAsrEnd();

    /**
     * partial result
     * @param results
     * @param recogResult
     */
    void onAsrPartialResult(String[] results, RecogResult recogResult);

    /**
     * final recog. result
     * @param results
     * @param recogResult
     */
    void onAsrFinalResult(String[] results, RecogResult recogResult);

    void onAsrFinish(RecogResult recogResult);

    void onAsrFinishError(int errorCode, String errorMessage, String descMessage);

    void onAsrExit();
}
