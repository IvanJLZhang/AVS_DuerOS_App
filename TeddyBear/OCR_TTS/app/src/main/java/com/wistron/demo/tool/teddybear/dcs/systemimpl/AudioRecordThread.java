package com.wistron.demo.tool.teddybear.dcs.systemimpl;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioRecord;
import com.wistron.demo.tool.teddybear.dcs.util.LogUtil;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * 录音采集线程-音频数据生产者
 * Created by ivanjlzhang on 17-9-23.
 */

public class AudioRecordThread extends Thread implements IAudioRecord {
    private static final String TAG = AudioRecordThread.class.getSimpleName();
    // 采样率
    private static final int SAMPLE_RATE_HZ = 16000;
    private int bufferSize;
    private AudioRecord audioRecord;
    private volatile boolean isStartRecord = false;
    private LinkedBlockingDeque<byte[]> linkedBlockingDeque;

    public AudioRecordThread(LinkedBlockingDeque<byte[]> linkedBlockingDeque) {
        this.linkedBlockingDeque = linkedBlockingDeque;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    }

    @Override
    public void startRecord() {
        if (isStartRecord) {
            return;
        }
        isStartRecord = true;
        this.start();
    }

    @Override
    public void stopRecord() {
        isStartRecord = false;
    }

    @Override
    public void run() {
        super.run();
        LogUtil.i(TAG, "audioRecord startRecording ");
        audioRecord.startRecording();
        byte[] buffer = new byte[bufferSize];
        while (isStartRecord) {
            int readBytes = audioRecord.read(buffer, 0, bufferSize);
            if (readBytes > 0) {
                linkedBlockingDeque.add(buffer);
            }
        }
        // 清空数据
        linkedBlockingDeque.clear();
        // 释放资源
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        LogUtil.i(TAG, "audioRecord release ");
    }
}
