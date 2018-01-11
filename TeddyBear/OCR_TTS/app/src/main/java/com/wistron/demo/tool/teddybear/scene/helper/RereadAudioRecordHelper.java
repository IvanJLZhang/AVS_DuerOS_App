package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;

import net.surina.soundtouch.SoundTouch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by king on 16-4-13.
 */
public class RereadAudioRecordHelper {
    private Context context;

    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final String SOUND_TOUCH_FILE_NAME = "sound_touch.wav";
    private String AUDIO_RECORDER_WAV_FILE;

    private BlockingQueue<byte[]> blockingQueue;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    //private Thread detectSpeakingThread = null;
    private boolean isRecording = false;
    //private boolean isSpeaking = false;
    private boolean isExit = false;

    private Handler mainHandler;

    // Play
    private AudioTrack audioTrack;

    // prefix of audio data
    private ArrayList<byte[]> mPreAudioData = new ArrayList<>(5);

    public RereadAudioRecordHelper(Context context, Handler mMainHandler) {
        this.context = context;
        this.mainHandler = mMainHandler;
    }

    public void initial() {
        initial(SceneCommonHelper.RECORDER_SAMPLERATE, SceneCommonHelper.RECORDER_CHANNELS, SceneCommonHelper.RECORDER_AUDIO_ENCODING);
    }

    private void initial(int sampleRate, int channels, int audioEncoding) {
        bufferSize = AudioRecord.getMinBufferSize
                (sampleRate, channels, audioEncoding) * 3;
        //audioData = new short[bufferSize]; //short array that pcm data is put into.
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate,
                channels,
                audioEncoding,
                bufferSize);

        blockingQueue = new LinkedBlockingDeque<>();
        audioTrack = new AudioTrack(3, 16000, 2, 2, AudioTrack.getMinBufferSize(16000, 2, 2), 1);
    }

    private void startRecord() {
        startRecord(AUDIO_RECORDER_WAV_FILE);
    }

    /**
     * @param fileName .wav file
     */
    public void startRecord(String fileName) {
        if (isRecording) {
            return;
        } else if (audioTrack != null) {
            if (AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()
                    || AudioTrack.PLAYSTATE_PAUSED == audioTrack.getPlayState()) {
                audioTrack.stop();
            }
        }
        SceneCommonHelper.blinkLED();
        updateStatus(context.getString(R.string.reread_status_pls_start_speaking));

        AUDIO_RECORDER_WAV_FILE = fileName;

        int i = recorder.getState();
        if (i == AudioRecord.STATE_INITIALIZED) {
            recorder.startRecording();
        }

        isRecording = true;

        /*detectSpeakingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startDetectSpeaking();
            }
        }, "Calculate Thread");
        detectSpeakingThread.start();*/

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void updateStatus(String status) {
        Message msg = mainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG);
        msg.obj = status;
        mainHandler.sendMessage(msg);
    }

    public void stopRecord() {
        Log.i("King", "stop record ");
        if (null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            Log.i("King", "Record state = " + i);
            if (i == AudioRecord.STATE_INITIALIZED
                    || i == AudioRecord.RECORDSTATE_RECORDING)
                recorder.stop();

            //detectSpeakingThread = null;
            if (recordingThread != null) {
                recordingThread.interrupt();
                recordingThread = null;
            }
        }
        if (mPreAudioData != null) {
            mPreAudioData.clear();
        }
    }

    public void exit() {
        isExit = true;

        if (audioTrack != null) {
            if (AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()
                    || AudioTrack.PLAYSTATE_PAUSED == audioTrack.getPlayState()) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }

        if (isRecording) {
            stopRecord();
        }
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
    }

    private String getBaseFolder() {
        return context.getFilesDir().getAbsolutePath();
    }

    private String getFilename() {
        return (getBaseFolder() + "/" + AUDIO_RECORDER_WAV_FILE);
    }

    private String getTempFilename() {
        return (getBaseFolder() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private String getSoundTouchFilename() {
        return (getBaseFolder() + "/" + SOUND_TOUCH_FILE_NAME);
    }

    private void writeAudioDataToFile() {
        String filename = getTempFilename();
        Log.i("King", "fileName = " + filename);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        byte data[] = new byte[bufferSize];

        int read = 0;
        try {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);
                Log.i("King", "read = " + read);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    os.write(data, 0, read);
                    os.flush();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (recorder != null) {
            recorder.stop();
        }
        Log.i("King", "detectSpeaking finished.");

        SceneCommonHelper.openLED();
        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        Log.i("King", "inFileName: " + inFilename + ", outFilename: " + outFilename);
        FileInputStream in = null;
        FileOutputStream out = null;

        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);

            SceneCommonHelper.writeWaveFileHeader(in, out);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();

            if (!isExit) {
                updateStatus(context.getString(R.string.reread_status_playing));
                startPlay(outFilename);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPlay(final String srcFileName) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                SoundTouch st = new SoundTouch();
                st.setTempo(80 * 0.01f);
                st.setPitchSemiTones(-7);
                int res = st.processFile(srcFileName, getSoundTouchFilename());
                if (res != 0) {
                    String err = SoundTouch.getErrorString();
                    Log.i("King", "SoundTouch decode error: " + err);
                    startRecord();
                    return;
                }

                try {
                    byte[] data = new byte[bufferSize];
                    FileInputStream inputStream = new FileInputStream(getSoundTouchFilename());
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while (inputStream.read(data) != -1) {
                        out.write(data);
                    }
                    inputStream.close();
                    out.close();

                    if (audioTrack != null) {
                        byte[] sound = out.toByteArray();
                        if (audioTrack.getState() == 1) {
                            audioTrack.play();
                            audioTrack.write(sound, 0, sound.length);

                            updateStatus(context.getString(R.string.reread_status_tips));
                            SceneCommonHelper.closeLED();

                            if (audioTrack != null && audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                                audioTrack.stop();
                                //audioTrack.release();
                                //startRecord();
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /*private void startDetectSpeaking() {
        String filename = getTempFilename();
        Log.i("King", "fileName = " + filename);
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int retryCount = -1;
        isSpeaking = false;
        while (isRecording) {
            try {
                byte[] data = blockingQueue.take();
                double db = SceneCommonHelper.countDb(SceneCommonHelper.Bytes2Shorts(data));
                Log.i("King", "db = " + db);
                if (db <= 75) {
                    if (!isSpeaking){
                        if (mPreAudioData.size() >= 5){
                            mPreAudioData.remove(0);
                        }
                        mPreAudioData.add(data.clone());
                    }

                    if (retryCount >= 0) {
                        retryCount++;
                        if (retryCount >= 15) {
                            stopRecord();
                            mPreAudioData.clear();
                            break;
                        }
                    }
                } else {
                    retryCount = 0;
                    isSpeaking = true;
                    updateStatus(context.getString(R.string.reread_status_recording));
                }

                if (isSpeaking) {
                    try {
                        if (mPreAudioData.size() > 0) {
                            for (byte[] preData : mPreAudioData) {
                                os.write(preData);
                            }
                            mPreAudioData.clear();
                        }

                        os.write(data);
                        os.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.i("King", "detectSpeaking finished.");
        try {
            os.close();
            Log.i("King", "save finished");
        } catch (IOException e) {
            e.printStackTrace();
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();
    }*/
}
