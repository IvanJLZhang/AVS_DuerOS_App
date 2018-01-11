package com.wistron.demo.tool.teddybear.dcs.systemimpl.audioinput;

import android.os.Handler;

import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioInput;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class AudioVoiceInputImpl implements IAudioInput {
    // 消费线程
    private AudioVoiceInputThread audioVoiceInputThread;
    // 音频数据
    private LinkedBlockingDeque<byte[]> linkedBlockingDeque;
    private IAudioInputListener audioInputListener;
    private Handler handler = new Handler();

    public AudioVoiceInputImpl(LinkedBlockingDeque<byte[]> linkedBlockingDeque) {
        this.linkedBlockingDeque = linkedBlockingDeque;
    }

    @Override
    public void startRecord() {
        DcsStreamRequestBody dcsStreamRequestBody = new DcsStreamRequestBody();
        audioInputListener.onStartRecord(dcsStreamRequestBody);
        audioVoiceInputThread = new AudioVoiceInputThread(
                linkedBlockingDeque,
                dcsStreamRequestBody.sink(),
                handler);
        audioVoiceInputThread.setAudioInputListener(new AudioVoiceInputThread.IAudioInputListener() {
            @Override
            public void onWriteFinished() {
                if (audioInputListener != null) {
                    audioInputListener.onStopRecord();
                }
            }
        });
        audioVoiceInputThread.startWriteStream();
    }

    @Override
    public void stopRecord() {
        audioVoiceInputThread.stopWriteStream();
    }

    @Override
    public void registerAudioInputListener(IAudioInputListener audioInputListener) {
        this.audioInputListener = audioInputListener;
    }
}
