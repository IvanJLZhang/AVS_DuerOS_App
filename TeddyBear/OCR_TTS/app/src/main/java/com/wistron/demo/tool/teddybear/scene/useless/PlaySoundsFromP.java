package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.microsoft.bing.speech.SpeechClientStatus;
import com.microsoft.cognitiveservices.speechrecognition.ISpeechRecognitionServerEvents;
import com.microsoft.cognitiveservices.speechrecognition.MicrophoneRecognitionClient;
import com.microsoft.cognitiveservices.speechrecognition.RecognitionResult;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionMode;
import com.microsoft.cognitiveservices.speechrecognition.SpeechRecognitionServiceFactory;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.scene.MonitorModeService;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;


public class PlaySoundsFromP extends SceneBase {
    private MicrophoneRecognitionClient micClient = null;
    private ArrayList<String> s_Message_List = new ArrayList<>();
    private MediaPlayer mediaPlayer;
    private BaseTaskManager mStorageTaskManager;
    private Thread lthread;

    private File messageTXT;

    private boolean status_memo = false;
    private boolean isUpdating = true;
    private boolean isUpdating2 = true;
    private boolean isPlayingMedia = false;
    private boolean ACTION_DownloadAll = false;
    private boolean failToDownall = false;

    //    private String ipAddress, domain, userName, password;
//    private String user_password_ted;
    private String allMessage_name;
    private String ftp_mTxt_path;
    private String ftp_file_path;
    private String path_directory;
    private String path_messageTxt;
    private String file_name_has_read = "(played)";
    private String wav_suffix = ".wav";
    private String path_add_sn;


    private static final int handle_sync_failed = 0;
    private static final int handle_send_record_over = 1;
    private static final int handle_init = 2;
    private static final int handle_parden = 3;
    private static final int handle_memo_over = 4;
    private static final int handle_play_memo = 5;


    class MemoHandler extends Handler {
        WeakReference<PlaySoundsFromP> mSceneMemo;

        MemoHandler(PlaySoundsFromP memo) {
            mSceneMemo = new WeakReference<PlaySoundsFromP>(memo);
        }

        @Override
        public void handleMessage(Message msg) {
            PlaySoundsFromP theMemo = mSceneMemo.get();
            switch (msg.what) {
                case handle_sync_failed:
                    theMemo.toSpeak(getString(R.string.luis_assistant_sync_new_message_failed), false);
                    break;
                case handle_send_record_over:
                    theMemo.waitForUserAnswer();
                    break;
                case handle_init:
                    theMemo.initial();
                    break;
                case handle_memo_over:
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            waiting();
                            stop();
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            updateLog("Memo stop!");
                        }
                    }.execute();
                    break;
                case handle_parden:
                    break;
                case handle_play_memo:
                    break;

            }

        }
    }

    private MemoHandler memoHandler = new MemoHandler(this);

    public PlaySoundsFromP(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    private void waiting() {
        while (isUpdating2) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        super.stop();
        sendToService(false);
        if (null != micClient) {
            micClient.endMicAndRecognition();
            try {
                micClient.finalize();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        micClient = null;
        status_memo = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mStorageTaskManager.removeAzureStorageChangedListener(mResultChangedlistener);
                Log.i("berlin_PP", "PP ends");
            }
        });
        if (null != lthread) lthread.interrupt();
        SceneCommonHelper.closeLED();

    }

    public boolean getStatus() {
        return status_memo;
    }

    @Override
    public void simulate() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                setFTPConfig();
                path_add_sn = getSN();
                Log.i("berlin", "got sn");
                memoHandler.sendMessage(memoHandler.obtainMessage(handle_init));
            }
        });
//        initial();
        SceneCommonHelper.openLED();
        super.simulate();
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedlistener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BERLIN)) {
                switch (responseCode) {
                    case BaseTaskManager.RESPONSE_CODE_PASS:
                        Log.v("berlin", "sync succeed");
                        break;
                    case BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT:
                        memoHandler.sendMessage(memoHandler.obtainMessage(handle_sync_failed));
                        Log.v("berlin", "sync failed");
                        if (ACTION_DownloadAll) {
                            failToDownall = true;
                        }
                        break;
                    case BaseTaskManager.RESPONSE_CODE_PASS_DELONE:
                        Log.v("berlin", "delete new.txt successfully! then upload message.txt.");
                        break;
                }
                Log.v("berlin", "break out...");
                isUpdating = false;
                isUpdating2 = false;
            }
        }
    };

    private void setFTPConfig() {
        if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_AZURE) {
            mStorageTaskManager = AzureStorageTaskManager.getInstance(context);
        } else if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_QINIU) {
            mStorageTaskManager = QiniuStorageTaskManager.getInstance(context);
        }
        mStorageTaskManager.addAzureStorageChangedListener(mResultChangedlistener);
    }

    private static String getASCII(String a) {
        String ns = "";
        char[] c1 = a.toCharArray();
        for (char c : c1) {
            ns = ns + "%" + c;
        }
        return ns;
    }


    private void initial() {
         /*
        if no sounds,say "over"
        */
        //ensure the path
        status_memo = true;

        String pmName = context.getPackageName();
        Log.v("berlin", "pmName=" + pmName);
        Log.v("berlin", "path_add_sn=" + path_add_sn);
        path_directory = context.getFilesDir() + "/" + path_add_sn + "/Memo/";
        path_messageTxt = path_directory + "message.txt";
        messageTXT = new File(path_messageTxt);

        ftp_file_path = path_add_sn + "/Memo";
        ftp_mTxt_path = "/" + ftp_file_path + "/message.txt";
        Log.v("berlin", "ftp_mTxt_path===" + ftp_mTxt_path);
        Log.v("berlin", "ftp_file_path===" + ftp_file_path);
        Log.v("berlin", "path_messageTxt===" + path_messageTxt);
        toSpeak(getString(R.string.luis_assistant_sync_new_message_initial), false);
        sendToService(true);
        updateLog("if no sounds output, try the scene again.");
        lthread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Log.v("berlin", "updating...");
                getLastParentSounds();
            }
        });
        lthread.start();
    }

    private void getLastParentSounds() {
        toSpeak(getString(R.string.luis_assistant_sync_new_message_updating), false);

        getAndPut();
        if (failToDownall) {
            return;
        }
        ACTION_DownloadAll = false;
        allMessage_name = readFromFile();
        get_message_not_read(allMessage_name);

        toSpeak(getString(R.string.luis_assistant_sync_new_message_end), false);
    }

    private void sendToService(boolean reading) {
        Intent intent = new Intent();
        intent.setAction(MonitorModeService.ACTION_NEW_MESSAGE);
        intent.putExtra("isreading", reading);
        context.sendBroadcast(intent);
    }

    private void waitForUserAnswer() {
        updateLog("Please answer:");
        if (micClient == null) {
            micClient = SpeechRecognitionServiceFactory.createMicrophoneClient(
                    SpeechRecognitionMode.ShortPhrase,
                    "en-us",
                    mSpeechRecognitionEvent,
                    context.getString(SubscriptionKey.getSpeechPrimaryKey()));
            micClient.setAuthenticationUri(getString(SubscriptionKey.getSpeechAuthenticationUri()));
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                micClient.startMicAndRecognition();
                updateLog("micclien is on");

            }
        });
    }

    private ISpeechRecognitionServerEvents mSpeechRecognitionEvent = new ISpeechRecognitionServerEvents() {
        @Override
        public void onPartialResponseReceived(String s) {
//            updateLog("Let's play a Quiz game");
        }

        @Override
        public void onFinalResponseReceived(RecognitionResult recognitionResult) {
            if (null != micClient) {
                micClient.endMicAndRecognition();
            }
            updateLog("********* Final Response Received *********");
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                updateLog("[" + i + "]" + " Confidence=" + recognitionResult.Results[i].Confidence +
                        " Text=\"" + recognitionResult.Results[i].DisplayText + "\"");
            }
            for (int i = 0; i < recognitionResult.Results.length; i++) {
                String useranswer = recognitionResult.Results[i].DisplayText;
                Log.v("berlin", "test useranswer=" + useranswer);
            }

        }

        @Override
        public void onIntentReceived(String s) {

        }

        public void onError(int i, String s) {
            updateLog("Error code: " + SpeechClientStatus.fromInt(i) + " " + s);
            updateLog("Error text: " + s);
            micClient.endMicAndRecognition();
            updateLog("Please retry!");
        }

        @Override
        public void onAudioEvent(boolean b) {
            if (!b) {
                micClient.endMicAndRecognition();
                updateLog("berlin--- Stop listening voice input!  \n  please wait...");
            }
        }

    };


    private String getSN() {
        String c = "";
        Class<?> classA;
        try {
            classA = Class.forName("android.os.SystemProperties");

            Method getSN = classA.getMethod("get", String.class);
            c = (String) getSN.invoke(classA, "ro.serialno");
        } catch (ClassNotFoundException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return c;
    }

    private void getAndPut() {
        Log.v("berlin", "get and put...");
        isUpdating = true;
        ACTION_DownloadAll = true;
        getAllFTPfile();
        while (isUpdating) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.v("berlin", "updating...");
        }
    }

    private void getAllFTPfile() {
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DOWNALL,
                ftp_file_path,
                path_directory);
    }

    private void ftpPut(File localFilePath) {
        isUpdating2 = true;
        boolean c;
        String remoteFileName = localFilePath.getName();
        String remoteFolder = ftp_file_path;
        File flag = new File(path_directory + "new.txt");
        if (!flag.exists()) {
            try {
                c = flag.createNewFile();
                Log.v("berlin", "created " + flag + " successfully? == " + c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_UPLOAD,
                localFilePath.getAbsolutePath(),
                remoteFolder,
                remoteFileName);
    }

    private void smb_file_exist_or_not() {
        try {
            SmbFile mSmbtxtFile = new SmbFile(ftp_mTxt_path);
            SmbFile mSmbpath = new SmbFile(ftp_file_path);
            Log.v("berlin", "txt=== " + String.valueOf(mSmbtxtFile));
            Log.v("berlin", "path=== " + String.valueOf(mSmbpath));
            if (!mSmbpath.exists()) {
                mSmbpath.mkdirs();
            }
            if (!mSmbtxtFile.exists()) {
                mSmbtxtFile.createNewFile();
            }
        } catch (MalformedURLException | SmbException e) {
            e.printStackTrace();
        }
    }

    private void file_exits_or_not() {
        File exist_file = new File(path_messageTxt);
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


    private String readFromFile() {
        String message_from_file = "";
        file_exits_or_not();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(path_messageTxt);
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

    private void get_message_not_read(String a) {
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
                s_Message_List.add(cach_message);
            cach_s1 = cach_s1.substring(index_last_parent_m + 6);
        }
        read_message_not_read();
    }

    private void read_message_not_read() {
        String S_lastFileName, S_lastFileRenamed;
        String file_in_txt_rename;
        for (int i = 0; i < s_Message_List.size(); i++) {
            String fileNameCach = s_Message_List.get(i);
            if (!fileNameCach.contains(file_name_has_read)) {
                toSpeak("" + (1 + i), false);
                if (fileNameCach.contains(wav_suffix)) {
                    int a = fileNameCach.indexOf(".wav");
                    fileNameCach = fileNameCach.substring(0, a);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    S_lastFileName = path_directory + fileNameCach.replaceAll("(/|:|.wav|\\r\\n|\\n)", "") + wav_suffix;
                    Log.v("berlin", "fileNameCach=== " + fileNameCach);
                    Log.v("berlin", "S_lastFileName=== " + S_lastFileName);
                    File lastFileName = new File(S_lastFileName);
                    play_media_start(lastFileName);
                    S_lastFileRenamed = S_lastFileName.replaceAll("(.wav|\\r\\n|\\n)", "") + file_name_has_read + wav_suffix;
                    File newFileName = new File(S_lastFileRenamed);
                    Log.v("berlin", "lastFileName=== " + lastFileName.getName());
                    Log.v("berlin", "newFileName=== " + newFileName.getName());
                    isUpdating2 = true;
                    mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                            BaseTaskManager.REQUEST_ACTION_RENAME,
                            ftp_file_path,
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
                    Log.v("berlin", "S_lastFileRenamed--- " + S_lastFileRenamed + " // succeed=" + c);
                    instead_the_txt(fileNameCach, file_in_txt_rename);
                } else {
                    toSpeak(fileNameCach, false);
                    instead_the_txt(fileNameCach, fileNameCach.replaceAll("(\\n|\\r\\n)", "") + file_name_has_read + "\r\n");
                    Log.v("berlin_file_name_cach:", "tts---" + fileNameCach);

                }
            }

        }
        toSpeak(getString(R.string.luis_assistant_sync_new_message_no_message), false);

        isUpdating2 = true;
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DELONE,
                ftp_file_path,
                "new.txt");
        waiting();
        ftpPut(messageTXT);
        memoHandler.obtainMessage(handle_memo_over).sendToTarget();
    }

    private void play_media_start(final File file) {
        mediaPlayer = new MediaPlayer();
        try {
            isPlayingMedia = true;
            mediaPlayer.setDataSource(String.valueOf(file));
            mediaPlayer.prepare();
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlayingMedia = false;
                }
            });
            Log.v("berlin", "aaaa");
            while (isPlayingMedia) {
//                delayHandler.postDelayed(runnable, 500);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            textToSpeech("Do you want repeat?");
//            waitForUserAnswer();
        } catch (IOException e) {
            updateLog("berlin: 播放失败");
        }
    }

    private void instead_the_txt(String old, String changer) {
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
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(messageTXT, false));
            bufferedWriter.write(allMessage_name);
            bufferedWriter.flush();
            bufferedWriter.close();
            Log.v("berlin", "all===" + allMessage_name + "\n");
            Log.v("berlin", "bufferedWriter closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void smbPut(final String localFilePath) {
        try {
            File localFile = new File(localFilePath);
            String fileName = localFile.getName();
            SmbFile remoteFile = new SmbFile(ftp_file_path + fileName);
            Log.v("berlin", "Upload ...");
            if (remoteFile.exists()) {
                remoteFile.delete();
                remoteFile.createNewFile();
            }
            FileInputStream inf = new FileInputStream(localFilePath);
            SmbFileOutputStream outfsmb = new SmbFileOutputStream(remoteFile);
//                    in = new BufferedInputStream(new FileInputStream(localFile));
//                    out = new BufferedOutputStream(new SmbFileOutputStream(remoteFile));
            long t0 = System.currentTimeMillis();
            byte[] buffer;
            buffer = new byte[8196];
            int n, tot = 0;
            while (-1 != (n = inf.read(buffer))) {
                outfsmb.write(buffer, 0, n);
                tot += n;
            }
            long t = System.currentTimeMillis() - t0;
            Log.v("berlin_time", tot + " bytes transfered in " + (t / 1000) + " seconds at "
                    + ((tot / 1000) / Math.max(1, (t / 1000))) + "Kbytes/sec");
            outfsmb.close();
            inf.close();
            Log.v("berlin", "Upload " + remoteFile + " successfully...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
