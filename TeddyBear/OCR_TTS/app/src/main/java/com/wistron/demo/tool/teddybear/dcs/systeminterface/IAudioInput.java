package com.wistron.demo.tool.teddybear.dcs.systeminterface;

import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;

/**
 * 语音输入接口
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IAudioInput {
    /**
     * 处理开始录音的逻辑
     */
    void startRecord();

    /**
     * 处理停止录音的逻辑
     */
    void stopRecord();

    void registerAudioInputListener(IAudioInputListener audioInputListener);

    interface IAudioInputListener{
        void onStartRecord(DcsStreamRequestBody dcsStreamRequestBody);
        void onStopRecord();
    }
}
