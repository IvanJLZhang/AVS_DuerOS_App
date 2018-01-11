package com.wistron.demo.tool.teddybear.scene;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;

/**
 * 时间：16-4-15 15:38
 * 作者：bob
 */
public class AudioFileFunc {
    //音频输入-麦克风
    public final int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;

    //采用频率
    //44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    public final int AUDIO_SAMPLE_RATE = 16000;  //44.1KHz,普遍使用的频率

    public final int AUDIO_SAMPLE_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    public final String AUDIO_AMR_FILENAME = "FinalAudio.amr";
    //录音输出文件
    private final String AUDIO_RAW_FILENAME = "RawAudio.raw";
    private final String AUDIO_WAV_FILENAME = "FinalAudio.wav";
    private Context context;

    public AudioFileFunc(Context context) {
        this.context = context;
    }

    /**
     * 判断是否有外部存储设备sdcard
     *
     * @return true | false
     */
    public boolean isSdcardExit() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }

    /**
     * 获取麦克风输入的原始音频流文件路径
     *
     * @return
     */
    public String getRawFilePath() {
        String mAudioRawPath = "";
        if (isSdcardExit()) {
            mAudioRawPath = getExternalFileDir() + File.separator + AUDIO_RAW_FILENAME;
        }

        return mAudioRawPath;
    }

    /**
     * 获取编码后的WAV格式音频文件路径
     *
     * @return
     */
    public String getWavFilePath() {
        String mAudioWavPath = "";
        if (isSdcardExit()) {
            mAudioWavPath = getExternalFileDir() + File.separator + AUDIO_WAV_FILENAME;
        }
        return mAudioWavPath;
    }


    /**
     * 获取编码后的AMR格式音频文件路径
     *
     * @return
     */
    public String getAMRFilePath() {
        String mAudioAMRPath = "";
        if (isSdcardExit()) {
            mAudioAMRPath = getExternalFileDir() + File.separator + AUDIO_AMR_FILENAME;
        }
        return mAudioAMRPath;
    }

    private String getExternalFileDir() {
        String fileBasePath = "/mnt/sdcard/";
        File file = context.getExternalFilesDir(null);
        if (null != file) {
            fileBasePath = file.getAbsolutePath();
        }
        return fileBasePath;
    }

    /**
     * 获取文件大小
     *
     * @param path,文件的绝对路径
     * @return
     */
    public long getFileSize(String path) {
        File mFile = new File(path);
        if (!mFile.exists())
            return -1;
        return mFile.length();
    }

}
