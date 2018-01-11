package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public interface IWakeupListener {
    void onSuccess(String word, WakeupResult result);

    void onStop();

    void onError(int errorCode, String errorMsg, WakeupResult result);

    void onAsrAudio(byte[] data, int offset, int length);

}
