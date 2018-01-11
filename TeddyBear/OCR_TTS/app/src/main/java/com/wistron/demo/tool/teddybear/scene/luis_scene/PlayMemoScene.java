package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.DataRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.led_control.LedForRecording;
import com.wistron.demo.tool.teddybear.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.scene.MonitorModeService;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.play_memo.RecordWav;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.wistron.demo.tool.teddybear.scene.SceneActivity.mCurSttEngine;

public class PlayMemoScene extends SceneBase {
    private DataRecognitionClient dataRecognitionClient;
    private ArrayList<String> mMessageList = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private BaseTaskManager mStorageTaskManager;
    private RecordWav mRecordWav;
    private MicrophoneRecognitionClient micClient = null;

    private File mLocalMessageFile;
    private File mNewAudioRecordFile;

    private int mPardonTimes;

    private int Berlin_FTP_ACTION;
    private int Berlin_DownloadAll = 0;
    //    private int Berlin_DownloadOne = 1;
    private int Berlin_UploadWav = 2;
    private int Berlin_UploadTxt = 3;
    private int Berlin_DeleteOne = 4;
    //    private int Berlin_CreateFolder = 5;
    private int Berlin_backup = 9;

    private boolean isRecording = false;
    private boolean isUpdating = true;
    private boolean micro_need = false;
    private boolean isUpdating2 = true;
    private boolean downloadAllFailed = false;
    private boolean anotherChance = true;

    private String mRemoteMemoDirectory;
    private String path_add_sn;
    private String mLocalMemoDirectory;
    private String wav_suffix = ".wav";
    private String s_time_no_s;
    private String s_time;
    private String file_name_has_read = "(played)";
    private String allMessage_name;
    private String micro_content;

    private static final int MSG_SEND_SOUNDS = 0;
    private static final int MSG_WAIT_VOICE_CMD = 1;
    private static final int MSG_PARDEN = 3;
    private static final int MSG_MEMO_OVER = 4;
    private static final int MSG_PLAY_MEMO = 5;
    private static final int MSG_SYNC_FAILED = 6;
    private static final int MSG_SPECIAL = 7;

    private MemoHandler mMemoHandler;

    public PlayMemoScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    private class MemoHandler extends Handler {
        WeakReference<PlayMemoScene> mSceneMemo;

        MemoHandler(PlayMemoScene memo) {
            mSceneMemo = new WeakReference<>(memo);
        }

        @Override
        public void handleMessage(Message msg) {
            PlayMemoScene theMemo = mSceneMemo.get();
            switch (msg.what) {
                case MSG_SEND_SOUNDS:
                    theMemo.updateLog("sending");
                    theMemo.getCreatedTime();
                    theMemo.record();
                    break;
                case MSG_WAIT_VOICE_CMD:
                    theMemo.waitCommend(msg.obj);
                    break;
                case MSG_MEMO_OVER:
                    theMemo.stop();
                    theMemo.updateLog("Memo stop!");
                    break;
                case MSG_PARDEN:
                    String textToSpeak = getString(R.string.luis_assistant_memo_pardon);
                    theMemo.toSpeak(textToSpeak, false);
                    theMemo.waitCommend(textToSpeak);
                    break;
                case MSG_PLAY_MEMO:
                    theMemo.getLastParentSounds();
                    break;
                case MSG_SYNC_FAILED:
                    theMemo.toSpeak(getString(R.string.luis_assistant_memo_last_action_fail), false);
                    break;
                case MSG_SPECIAL:
                    theMemo.special();
                    break;
            }
        }
    }

    @Override
    public void stop() {
        Log.v("berlin", "stop play memo scene");
        mStorageTaskManager.removeAzureStorageChangedListener(onRequestResultChangedListener);
        sendToService(false);
        mPardonTimes = 0;
        isUpdating = false;
        isUpdating2 = false;
        isRecording = false;
        if (null != mRecordWav) {
            mRecordWav.stopRecord();
        }

        if (null != dataRecognitionClient) {
            dataRecognitionClient.endAudio();
            try {
                dataRecognitionClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        dataRecognitionClient = null;

        if (micClient != null) {
            micClient.endMicAndRecognition();
            try {
                micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            micClient = null;
        }

        super.stop();
        SceneCommonHelper.closeLED();

        toSpeak(getString(R.string.luis_assistant_memo_over));
        Log.v("berlin", "END:stop play memo scene");
    }

    private void special() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                while (isUpdating2 && !isSceneStopped) {
                    try {
                        Log.v("berlin", "special....");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if (isSceneStopped) return;

                String textToSpeak = getString(R.string.luis_assistant_memo_other_help);
                toSpeak(textToSpeak, false);
                waitCommend(textToSpeak);
            }
        }.execute();
    }

    @Override
    public void simulate() {
        super.simulate();
        if (mMemoHandler == null) {
            mMemoHandler = new MemoHandler(this);
        }

        setFTPConfig();
        initial();

        SceneCommonHelper.openLED();
    }

    private BaseTaskManager.OnRequestResultChangedListener onRequestResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BERLIN)) {

                switch (responseCode) {
                    case BaseTaskManager.RESPONSE_CODE_PASS:
                        Log.v("berlin", "sync succeed");
                        isUpdating = false;
                        isUpdating2 = false;
                        break;
                    case BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT:
                        toSpeak(getString(R.string.luis_assistant_memo_connect_fail), false);
                        Log.v("berlin", "sync failed");
                        updateLog("Sorry, sync failed. End the scene.");
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_MEMO_OVER));

                        break;
                    case BaseTaskManager.RESPONSE_CODE_PASS_DELONE:
                        isUpdating = false;
                        isUpdating2 = false;
                        Log.v("berlin", "delete new.txt successfully! then upload message.txt.");
                        Berlin_FTP_ACTION = Berlin_UploadTxt;
                        ftpPut(mLocalMessageFile);
                        mMemoHandler.obtainMessage(MSG_SPECIAL).sendToTarget();
                        break;
                    case BaseTaskManager.RESPONSE_CODE_FAIL_DOWNALL:
                        if (anotherChance) {
                            getAllFTPfile();
                            toSpeak(getString(R.string.luis_assistant_memo_update_fail), false);
                            anotherChance = false;
                            break;
                        }
                        downloadAllFailed = true;
                        anotherChance = true;
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_SYNC_FAILED));
                        Log.v("berlin", "sync failed");
                        break;
                    case BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD:
                        if (!anotherChance) {
                            anotherChance = true;
                            mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_SYNC_FAILED));
                            isUpdating = false;
                            isUpdating2 = false;
                            Log.v("berlin", "sync failed .   status -> RESPONSE_CODE_FAIL_UPLOAD");
                            break;
                        }
                        anotherChance = false;
                        toSpeak(getString(R.string.luis_assistant_memo_update_fail), false);
                        Log.v("berlin", "upload file failed");
                        switch (Berlin_FTP_ACTION) {
                            case 2:
                                ftpMultiPut();
                                break;
                            case 3:
                                ftpPut(mLocalMessageFile);
                                break;
                        }
                        break;
                    case BaseTaskManager.RESPONSE_CODE_FAIL_DELONE:
                        if (!anotherChance) {
                            anotherChance = true;
                            mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_SYNC_FAILED));
                            Log.v("berlin", "sync failed.  status -> RESPONSE_CODE_FAIL_DELONE");
                            isUpdating = false;
                            isUpdating2 = false;
                            Log.v("berlin", "delete the Flag \"new\" failed! ");
                            if (isSceneStopped) break;

                            try {
                                Thread.sleep(200l);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            String textToSpeak = getString(R.string.luis_assistant_memo_other_help);
                            toSpeak(textToSpeak, false);
                            Message msg = mMemoHandler.obtainMessage(MSG_WAIT_VOICE_CMD);
                            msg.obj = textToSpeak;
                            mMemoHandler.sendMessage(msg);
                            break;
                        }
                        anotherChance = false;
                        toSpeak(getString(R.string.luis_assistant_memo_update_fail), false);
                        Log.v("berlin", "sync failed");

                        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                                BaseTaskManager.REQUEST_ACTION_DELONE,
                                mRemoteMemoDirectory,
                                "new.txt");
                        break;
                    default:
                        //do i need to redo the last action again when it was failed.
                        Berlin_FTP_ACTION = Berlin_backup;
                        isUpdating = false;
                        isUpdating2 = false;
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_SYNC_FAILED));
                        Log.v("berlin", "sync failed. status -> default");
                        break;
                }
            }
        }
    };

    private void setFTPConfig() {
        if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_AZURE) {
            mStorageTaskManager = AzureStorageTaskManager.getInstance(context);
        } else if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_QINIU) {
            mStorageTaskManager = QiniuStorageTaskManager.getInstance(context);
        }
        mStorageTaskManager.addAzureStorageChangedListener(onRequestResultChangedListener);
    }

    private void initial() {
         /*
        if no sounds,say "over"
        */
        sendToService(true);
        SceneCommonHelper.closeDotLED();

        path_add_sn = Build.SERIAL;
        mLocalMemoDirectory = context.getFilesDir() + "/" + path_add_sn + "/Memo/";
        mLocalMessageFile = new File(mLocalMemoDirectory + "message.txt");
        mRemoteMemoDirectory = path_add_sn + "/Memo";
        Log.v("berlin", "mLocalMessageFile === " + mLocalMessageFile.getAbsolutePath());
        Log.v("berlin", "mRemoteMemoDirectory===" + mRemoteMemoDirectory);

        //toSpeak(getString(R.string.luis_assistant_memo_initial), false);
        updateLog("if no sounds output,just say 'over' and try the scene again.");
        updateLog("The queue of accessing the ftp is slowly,please wait for a little patience.");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if (dataRecognitionClient == null) {
                    Log.i("King", "PlayMemoScene initial dataRecognitionlclient......>>>>>>>>>>>>>>>>>>>>>   Start");
                    dataRecognitionClient = SpeechRecognitionServiceFactory.createDataClient(SpeechRecognitionMode.ShortPhrase,
                            SceneCommonHelper.getSpeakingLanguageSetting(context),
                            mSpeechRecognitionEvent,
                            context.getString(SubscriptionKey.getSpeechPrimaryKey()));
                    dataRecognitionClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
                    Log.i("King", "PlayMemoScene initial dataRecognitionlclient......>>>>>>>>>>>>>>>>>>>>>   End");
                }
            }
        });

        if (isSceneStopped) return;
        String textToSpeak = getString(R.string.luis_assistant_memo_action_warning);
        toSpeak(textToSpeak, false);

        Message msg = mMemoHandler.obtainMessage(MSG_WAIT_VOICE_CMD);
        msg.obj = textToSpeak;
        mMemoHandler.sendMessage(msg);
    }

    @Override
    public void updateSttResult(String result) {
        super.updateSttResult(result);
        decodeAnswer(result);
    }

    private void sendToService(boolean reading) {
        Intent intent = new Intent();
        intent.setAction(MonitorModeService.ACTION_NEW_MESSAGE);
        intent.putExtra("isreading", reading);
        context.sendBroadcast(intent);
    }

    private void waitCommend(Object text) {
        if (isSceneStopped) return;

        if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_MICROSOFT) {
            //added on 21st July.
            if (micClient == null) {
                micClient = SpeechRecognitionServiceFactory.createMicrophoneClientWithIntent(
                        (Activity) context,
                        SceneCommonHelper.getSpeakingLanguageSetting(context),
                        mSpeechRecognitionEvent,
                        context.getString(SubscriptionKey.getSpeechPrimaryKey()),
                        SceneCommonHelper.getString(context, SubscriptionKey.getLuisMemoGameAppId()),
                        context.getString(SubscriptionKey.getLuisSubscriptionKey()));
                micClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
            }
            Log.v("berlin", "wait for voice commend...");
            if (!isSceneStopped) {
                ((SceneActivity) context).stopWakeupService();
                micClient.startMicAndRecognition();
            }
        } else if (mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE
                || mCurSttEngine == SceneCommonHelper.STT_ENGINE_GOOGLE_CLOUD) {
            ((SceneActivity) context).startToListenCmd(false, text == null ? null : (String) text);
        }
    }

    private void waitForUpload() {
        if (dataRecognitionClient == null) {
            dataRecognitionClient = SpeechRecognitionServiceFactory.createDataClient(SpeechRecognitionMode.ShortPhrase,
                    SceneCommonHelper.getSpeakingLanguageSetting(context),
                    mSpeechRecognitionEvent,
                    context.getString(SubscriptionKey.getSpeechPrimaryKey()));
            dataRecognitionClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.v("berlin", "wait for data...");
                if (null != dataRecognitionClient) {
                    dataSendNow(mNewAudioRecordFile);
                }
            }
        });
    }

    private void dataSendNow(File file) {
        try {
            // Note for wave files, we can just send data from the file right to the server.
            // In the case you are not an audio file in wave format, and instead you have just
            // raw data (for example audio coming over bluetooth), then before sending up any
            // audio data, you must first send up an SpeechAudioFormat descriptor to describe
            // the layout and format of your raw audio data via DataRecognitionClient's sendAudioFormat() method.
            // String filename = recoMode == SpeechRecognitionMode.ShortPhrase ? "whatstheweatherlike.wav" : "batman.wav";
            Log.v("berlin", "recognition task");
            FileInputStream fileStream = new FileInputStream(file);
            int bytesRead;
            byte[] buffer = new byte[1024];

            do {
                // Get  Audio data to send into byte buffer.
                bytesRead = fileStream.read(buffer);

                if (bytesRead > -1) {
                    // Send of audio data to service.
                    dataRecognitionClient.sendAudio(buffer, bytesRead);
                }
            } while (bytesRead > 0);

            fileStream.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            if (null != dataRecognitionClient) {
                dataRecognitionClient.endAudio();
                Log.v("berlin", " a sdsd==end audio");
            }
        }
    }

    private void decodeAnswer(String result) {
        try {
            JSONObject toJason = new JSONObject(result);
            if (toJason.get(LuisHelper.TAG_QUERY) != null) {
                JSONArray intents = toJason.getJSONArray(LuisHelper.TAG_INTENTS);
                String intent = intents.getJSONObject(0).getString(LuisHelper.TAG_INTENT);
                Log.v("berlin", "get the result in the scene. intent = " + intent);
                switch (intent) {
                    case LuisHelper.INTENT_MEMO_SEND_MSG:
                        mPardonTimes = 0;
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_SEND_SOUNDS));
                        break;
                    case LuisHelper.INTENT_MEMO_OVER:
                        mPardonTimes = 0;
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_MEMO_OVER));
                        break;
                    case LuisHelper.INTENT_MEMO_PLAY:
                        mPardonTimes = 0;
                        SceneCommonHelper.closeDotLED();
                        mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_PLAY_MEMO));
                        break;
                    default:
                        mPardonTimes++;
                        if (mPardonTimes < 3) {
                            mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_PARDEN));
                        } else mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_MEMO_OVER));
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("King", "PlayMemoScene onIntentReceived = " + e.getMessage());
        }
    }

    private ISpeechRecognitionServerEvents mSpeechRecognitionEvent = new ISpeechRecognitionServerEvents() {
        @Override
        public void onPartialResponseReceived(String s) {

        }

        @Override
        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
            if (null != dataRecognitionClient) {
                dataRecognitionClient.endAudio();
                Log.v("aaa", "sdsd==end audio");
            }
            if (null != micClient) {
                // we got the final result, so it we can end the mic reco.  No need to do this
                // for dataReco, since we already called endAudio() on it as soon as we were done
                // sending all the data.
                micClient.endMicAndRecognition();
            }

            ((SceneActivity) context).startWakeupService();
            updateLog("********* Final Response Received *********");

            for (int i = 0; i < recognitionResult.Results.length; i++) {
                updateLog("[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
            if (micro_need) {
                if (recognitionResult.Results.length == 0) {
                    micro_need = false;
                    return;
                }

                micro_content = recognitionResult.Results[0].DisplayText;
                Log.v("berlin", "micro content=" + micro_content);
                micro_need = false;
            }
        }

        @Override
        public void onIntentReceived(String s) {
            decodeAnswer(s);
        }

        public void onError(int i, String s) {
            updateLog("Error received by onError()");
            updateLog("Error code: " + SpeechClientStatus.fromInt(i) + " " + s);
            updateLog("Error text: " + s);

            if (null != dataRecognitionClient) {
                dataRecognitionClient.endAudio();
            }
            if (null != micClient) {
                micClient.endMicAndRecognition();
            }
            updateLog("Please retry!");

            LedForRecording.recordingStop(context);
            ((SceneActivity) context).startWakeupService();
        }

        @Override
        public void onAudioEvent(boolean b) {
            if (!b) {
                if (dataRecognitionClient != null) {
                    dataRecognitionClient.endAudio();
                }
                if (null != micClient) {
                    micClient.endMicAndRecognition();
                }

                updateLog("berlin--- Stop listening voice input!  \n  please wait...");
                LedForRecording.recordingStop(context);
            } else {
                SceneCommonHelper.playSpeakingSound(context, SceneCommonHelper.WARN_SOUND_TYPE_START);
                updateLog("Please answer:");
                LedForRecording.recordingStart(context);
            }
        }

    };

    protected void updateLog(String content) {
        Message message = mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG);
        message.obj = content;
        mMainHandler.sendMessage(message);
    }

    private String readFromFile() {
        String message_from_file = "";
        isFileExist();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(mLocalMessageFile.getAbsolutePath());
            int file_message_length = fileInputStream.available();
            byte[] buffer = new byte[file_message_length];
            if (fileInputStream.read(buffer) > -1) {
                message_from_file = new String(buffer, "UTF-8");
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message_from_file;

    }

    private void write_file(String content_to_write) throws FileNotFoundException {
        isFileExist();
        FileOutputStream fileOutputStream = null;
        Log.v("berlin", "mLocalMessagePath==" + mLocalMessageFile);
        OutputStreamWriter oWriter = null;
        try {
            fileOutputStream = new FileOutputStream(mLocalMessageFile, true);
            oWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            assert oWriter != null;
            oWriter.write(content_to_write);
            oWriter.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (oWriter != null) {
                    oWriter.close();
                    oWriter = null;
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void isFileExist() {
        File exist_file = new File(mLocalMessageFile.getAbsolutePath());
        try {
            if (!exist_file.getParentFile().exists()) {
                boolean c = exist_file.getParentFile().mkdirs();
                Log.v("berlin", "exist=" + c);
            }
            if (!exist_file.exists()) {
                boolean c = exist_file.createNewFile();
                Log.v("berlin", "create new file=" + c);
            }
        } catch (Exception ignored) {
        }
    }

    private void getCreatedTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/hh:mm:ss", Locale.US);
        s_time = dateFormat.format(new Date(System.currentTimeMillis()));
        s_time_no_s = s_time.replaceAll("(/|:)", "");
    }

    public void record() {
        SceneCommonHelper.playSpeakingSound(context, SceneCommonHelper.WARN_SOUND_TYPE_START, false, true);
        isFileExist();
        mNewAudioRecordFile = new File(mLocalMemoDirectory + "Childaudio." + s_time_no_s + wav_suffix);
        updateLog("berlin: child recording");
        //LED twinkling when recording
        SceneCommonHelper.blinkLED();
        if (isSceneStopped) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                anotherRecordWay();
            }
        }).start();
    }

    private void anotherRecordWay() {
        mRecordWav = RecordWav.getInstanse(context, false, mMainHandler);
        mRecordWav.recordChat(mLocalMemoDirectory, mNewAudioRecordFile.getName());
        isRecording = true;
        micro_need = true;

        new Thread() {
            @Override
            public void run() {
                boolean record_effective = false;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.v("berlin", "check the break...0");
                Log.v("berlin", "dB is calculating.");
                while (isRecording) {
                    //2016_5_24:  change  : readsize -> bufferSize
                    boolean dbOver32 = false;
                    Log.v("berlin", "check the break...1");
                    for (int i = 0; i < 8; i++) {
                        dbOver32 = (mRecordWav.calculate_dB() > 32);
                        if (dbOver32) {
                            record_effective = true;
                            Log.v("berlin", "check the break...2");
                            break;
                        }
                    }
                    Log.v("berlin", "check the break...3");
                    if (!dbOver32) {
                        Log.v("berlin", "check the break...4");
                        if (record_effective) recordClose();
                        else recordCloseInvalid();
                    }
                }
            }
        }.start();
    }

    private void anotherRecordStop() {
        //is it R ?  r ?  of recordwav...
        mRecordWav.stopRecord();
        mMainHandler.sendEmptyMessage(SceneActivity.MSG_START_SVA_SERVICE);
    }

    private void recordClose() {
        anotherRecordStop();
        if (isSceneStopped) return;

        toSpeak(getString(R.string.luis_assistant_memo_record_stop), false);
        isRecording = false;//停止文件写入

        //open the ring LED.
        SceneCommonHelper.openLED();

        if (mNewAudioRecordFile.exists()) {
            Log.v("berlin", "mNewAudioRecordFile exists!");
        }

        waitForUpload();
        notifyParent();
        if (isSceneStopped) return;

        updateLog("Sending a message to notify parents...");
        while (isUpdating2 || micro_need) {
            if (isSceneStopped) return;
            try {
                Thread.sleep(1000);
                Log.v("berlin", "Uploading  newC flag...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        updateLog("Receiving the results from microsoft...");
        Log.v("berlin", "mNewAudioRecordFile===" + mNewAudioRecordFile.getAbsolutePath());
        addAudio();

        String textToSpeak = getString(R.string.luis_assistant_memo_other_help);
        toSpeak(textToSpeak, false);
        Message msg = mMemoHandler.obtainMessage(MSG_WAIT_VOICE_CMD);
        msg.obj = textToSpeak;
        mMemoHandler.sendMessage(msg);
    }

    private void recordCloseInvalid() {
        anotherRecordStop();
        isRecording = false;//停止文件写入

        SceneCommonHelper.openLED();
        updateLog("No message is sent.");
        if (isSceneStopped) return;

        String textToSpeak = getString(R.string.luis_assistant_memo_send_too_short);
        toSpeak(textToSpeak, false);
        Message msg = mMemoHandler.obtainMessage(MSG_WAIT_VOICE_CMD);
        msg.obj = textToSpeak;
        mMemoHandler.sendMessage(msg);
    }

    private void addAudio() {
        String addToTxt;
        try {
            String index_children_number = "0x567:";
            addToTxt = "\n" + index_children_number + "Childaudio." + s_time + wav_suffix;
            addToTxt += "\n" + micro_content;
            Log.v("berlin", "Childaudio:: audio is writen==" + addToTxt);
            write_file(addToTxt);
            Berlin_FTP_ACTION = Berlin_UploadWav;
            updateLog("Uploading the messages...");
            ftpMultiPut();

            while (isUpdating2) {
                if (isSceneStopped) return;
                Log.v("berlin", "Uploading...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void ftpMultiPut() {
        isUpdating2 = true;
        File[] a = new File[2];
        a[0] = mNewAudioRecordFile;
        a[1] = mLocalMessageFile;
        ftpPut(a);
    }

    private void ftpPut(File[] localFilePath) {
        int length = localFilePath.length;
        String[] remoteFileName = new String[length];
        String[] localPathString = new String[length];
        StringBuilder allpath = new StringBuilder(";");
        StringBuilder allRemoteName = new StringBuilder(";");
        for (int i = 0; i < length; i++) {
            localPathString[i] = localFilePath[i].getAbsolutePath();
            allpath.append(localPathString[i]);
            allpath.append(";");
            remoteFileName[i] = localFilePath[i].getName();
            allRemoteName.append(remoteFileName[i]);
            allRemoteName.append(";");
        }
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_MUTLI,
                allpath.substring(1),
                mRemoteMemoDirectory,
                allRemoteName.substring(1));
    }

    //get message not read from txt
    private void getUnreadMessageFromFile(String a) {
        int index_0x, index_last_parent_m;
        String cach_s1 = a;
        if (cach_s1.contains(file_name_has_read)) {
            int index_last_read = cach_s1.lastIndexOf(file_name_has_read);
            cach_s1 = cach_s1.substring(index_last_read);
        }
        String index_parent_number = "0x233:";
        while (cach_s1.contains(index_parent_number)) {
            index_last_parent_m = cach_s1.indexOf(index_parent_number);
            index_0x = cach_s1.indexOf("0x", index_last_parent_m + 6);
            String cach_message;
            if (index_0x != -1) {
                cach_message = cach_s1.substring(index_last_parent_m + 6, index_0x);
            } else {
                cach_message = cach_s1.substring(index_last_parent_m + 6);
            }
            Log.v("berlin_cach_s1", "" + cach_message);
            if (!cach_message.contains("(played)"))
                mMessageList.add(cach_message);
            cach_s1 = cach_s1.substring(index_last_parent_m + 6);
        }
        readUnreadMessage();
    }

    private void readUnreadMessage() {
        String S_lastFileName, S_lastFileRenamed;
        String file_in_txt_rename;
        for (int i = 0; i < mMessageList.size(); i++) {
            if (isSceneStopped) return;
            String fileNameCach = mMessageList.get(i);
            if (!fileNameCach.contains(file_name_has_read)) {
                if (isSceneStopped) return;

                toSpeak("" + (1 + i), false);
                if (fileNameCach.contains(wav_suffix)) {
                    int a = fileNameCach.indexOf(".wav");
                    fileNameCach = fileNameCach.substring(0, a);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    S_lastFileName = mLocalMemoDirectory + fileNameCach.replaceAll("(/|:|.wav|\\r\\n|\\n)", "") + wav_suffix;
                    Log.v("berlin", "fileNameCach=== " + fileNameCach);
                    Log.v("berlin", "S_lastFileName=== " + S_lastFileName);
                    File lastFileName = new File(S_lastFileName);
                    playAudioMessage(lastFileName);
                    S_lastFileRenamed = S_lastFileName.replaceAll("(.wav|\\r\\n|\\n)", "") + file_name_has_read + wav_suffix;
                    File newFileName = new File(S_lastFileRenamed);
                    Log.v("berlin", "lastFileName=== " + lastFileName.getName());
                    Log.v("berlin", "newFileName=== " + newFileName.getName());
                    isUpdating2 = true;
                    mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                            BaseTaskManager.REQUEST_ACTION_RENAME,
                            mRemoteMemoDirectory,
                            lastFileName.getName(),
                            newFileName.getName()
                    );
                    while (isUpdating2) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.v("berlin", "updating...");
                    }
                    file_in_txt_rename = newFileName.getName() + "\r\n";
                    boolean c = lastFileName.renameTo(newFileName);
                    Log.v("berlin", "newFileName ==" + newFileName + "//succeed=" + c);
                    insteadWithPlayed(fileNameCach, file_in_txt_rename);
                } else {
                    if (isSceneStopped) return;

                    toSpeak(fileNameCach, false);
                    insteadWithPlayed(fileNameCach, fileNameCach.replaceAll("(\\n|\\r\\n)", "") + file_name_has_read + "\r\n");
                    Log.v("berlin_file_name_cach:", "tts---" + fileNameCach);

                }
            }

        }
        if (isSceneStopped) return;

        toSpeak(getString(R.string.luis_assistant_memo_no_message_left), false);

        Berlin_FTP_ACTION = Berlin_DeleteOne;
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DELONE,
                mRemoteMemoDirectory,
                "new.txt");
    }

    private void insteadWithPlayed(String old, String changer) {
        int a = allMessage_name.indexOf(old);
        int b = allMessage_name.indexOf("0x", a + 1);
        Log.v("berlin", "index is a== " + a);
        Log.v("berlin", "index is b== " + b);
        if (a != -1) {
            if (b != -1) {
                allMessage_name = allMessage_name.substring(0, a) + changer + allMessage_name.substring(b);
            } else {
                allMessage_name = allMessage_name.substring(0, a) + changer;
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(mLocalMessageFile.getAbsolutePath(), false));
            bufferedWriter.write(allMessage_name);
            bufferedWriter.flush();
            bufferedWriter.close();
            Log.v("berlin", "all===" + allMessage_name + "\n");
            Log.v("berlin", "bufferedWriter closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void getLastParentSounds() {
        if (isSceneStopped) return;

        toSpeak(getString(R.string.luis_assistant_memo_update_msg), false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getAndPut();
                if (isSceneStopped) {
                    return;
                }
                allMessage_name = readFromFile();
                getUnreadMessageFromFile(allMessage_name);
            }
        }).start();
    }

    private void getAndPut() {
        isUpdating = true;
        new Thread(updateRun).start();
        while (isUpdating) {
            if (isSceneStopped) return;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.v("berlin", "updating...");
        }
        if (downloadAllFailed) {
            toSpeak(getString(R.string.luis_assistant_memo_connect_fail), false);
            isSceneStopped = true;
            mMemoHandler.sendMessage(mMemoHandler.obtainMessage(MSG_MEMO_OVER));
        }
    }

    private void playAudioMessage(final File file) {
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(String.valueOf(file));
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                }
            });
            Log.v("berlin", "aaaa");
            while (null != mMediaPlayer) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            updateLog("berlin: 播放失败");
        }
    }

    private void ftpPut(File localFilePath) {
        isUpdating2 = true;
        String remoteFileName = localFilePath.getName();
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_UPLOAD,
                localFilePath.getAbsolutePath(),
                mRemoteMemoDirectory,
                remoteFileName);
    }

    private void notifyParent() {
        boolean c;
        File flag = new File(mLocalMemoDirectory + "newC.txt");
        isUpdating2 = true;
        String newCcontent = "";
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                SceneCommonHelper.REMOTE_FOLDER_COMMON,
                "newC.txt",
                String.valueOf(flag));
        while (isUpdating2) {
            try {
                Thread.sleep(1000);
                Log.v("berlin", "notifying...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (!flag.exists()) {
            try {
                c = flag.createNewFile();
                Log.v("berlin", "created " + flag + " successfully? == " + c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(flag);
            int file_message_length = fileInputStream.available();
            byte[] buffer = new byte[file_message_length];

            if (fileInputStream.read(buffer) > -1) {
                newCcontent = new String(buffer, "UTF-8");

            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (newCcontent.contains(path_add_sn)) {
            return;
        }
        Log.v("berlin", "have but not return. Sadly.");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(flag, true);
            Log.v("berlin_EBADF", "flag==" + flag);
            OutputStreamWriter oWriter;
            oWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            oWriter.write("0x567:" + path_add_sn + "\n");
            oWriter.flush();
            fileOutputStream.close();
            oWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isUpdating2 = true;
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_UPLOAD,
                flag.getAbsolutePath(),
                SceneCommonHelper.REMOTE_FOLDER_COMMON,
                flag.getName());
    }

    private Runnable updateRun = new Runnable() {
        @Override
        public void run() {
            getAllFTPfile();
            Log.v("berlin ", "initial after downloading all ......");
        }
    };

    private void getAllFTPfile() {
        Berlin_FTP_ACTION = Berlin_DownloadAll;
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DOWNALL,
                mRemoteMemoDirectory,
                mLocalMemoDirectory);
    }
}
