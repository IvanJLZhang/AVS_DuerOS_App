package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.led_control.LightLed;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.WisShellCommandHelper;
import com.wistron.demo.tool.teddybear.scene.SceneSettingsFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by king on 16-4-13.
 */
public class SceneCommonHelper {
    public static ExecutorService mCachedThreadPool;

    /* Recorder*/
    public static final int RECORDER_BPP = 16;
    public static int RECORDER_SAMPLERATE = 16000;
    public static int RECORDER_CHANNELS = 2;
    public static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    /* Preference pando */
    public static final String PREFERENCE_PANDO_NAME = "pando";
    public static final String PREFERENCE_PANDO_KEY_MUTE = "is_mute";

    /* constant */
    public static final String STORAGE_ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String STORAGE_CONFIG_FOLDER = STORAGE_ROOT_PATH + "/teddy_bear/config/";
    public static final String STORAGE_BAIDU_CONFIG_FOLDER = STORAGE_ROOT_PATH + "/teddy_bear/baidu/";
    public static final String STORAGE_CONFIG_FILE_PATH = STORAGE_CONFIG_FOLDER + "config.txt";
    public static final String STORAGE_SVA_BT_CONFIG_FILE_PATH = STORAGE_CONFIG_FOLDER + "sva_speaker_recognition_bt.txt";
    /* wifi settings config */
    public static final int WIFI_SETTINGS_MODE_BT = 0;
    public static final int WIFI_SETTINGS_MODE_WIFI_P2P = 1;
    public static final int DEFAULT_WIFI_SETTINGS_MODE = WIFI_SETTINGS_MODE_WIFI_P2P;

    private static final String CONFIG_KEY_WIFI_SETTINGS_MODE = "WifiSettingsMode";
    private static final String CONFIG_KEY_WIFI_SETTINGS_TRIGGER_KEY_CODE = "WifiSettingTriggerKeyCode";
    public static final String CONFIG_KEY_KEYWORD_THRESHOLD = "KeywordThreshold";
    private static final String CONFIG_KEY_STT_ENGINE = "SttEngine";
    private static final String CONFIG_KEY_TTS_ENGINE = "TtsEngine";
    private static final String CONFIG_KEY_SVA_LEVEL = "SVALevel";
    public static final String CONFIG_KEY_YEE_LIGHT_GROUP = "YeeLightGroup";
    private static final String CONFIG_KEY_SVA_SPEAKER_RECOGNITION_STYLE = "SVASpeakerRecognitionStyle";
    private static final String CONFIG_KEY_ENABLE_SYNC_NOTIFICATION = "enableSyncNotification";
    private static final String CONFIG_KEY_BUFFERING_MODE = "BufferingMode";
    private static final String CONFIG_KEY_UIM_KEYWORD_TO_TRIGGER = "UimKeywordToTrigger";
    private static final String CONFIG_KEY_WAKEUP_ENGINE = "wakeupEngine";

    public static final int SVA_SPEAKER_RECOGNITION_STYLE_UDM = 0;
    public static final int SVA_SPEAKER_RECOGNITION_STYLE_BT = 1;
    public static final int DEFAULT_SVA_SPEAKER_RECOGNITION_STYLE = SVA_SPEAKER_RECOGNITION_STYLE_UDM;
    public static final boolean DEFAULT_ENABLE_SYNC_NOTIFICATION_STATUS = false;
    private static final int DEFAULT_SVA_LEVEL = 69;

    public static final String REMOTE_FOLDER_COMMON = "common";
    public static final String REMOTE_SVA_BT_FILE = "svaBtMap.txt";

    public static final int STORAGE_AZURE = 0;
    public static final int STORAGE_QINIU = 1;
    public static final int DEFAULT_STORAGE = STORAGE_QINIU;

    public static final int STT_ENGINE_MICROSOFT = 0;
    public static final int STT_ENGINE_GOOGLE = 1;
    public static final int STT_ENGINE_GOOGLE_CLOUD = 2;
    public static final int STT_ENGINE_BAIDU_ONLINE = 3;

    public static final int STT_STATUS_INITIAL = 0;
    public static final int STT_STATUS_ERROR = 1;
    public static final int STT_STATUS_START_RECORDING = 2;
    public static final int STT_STATUS_STOP_RECORDING = 3;
    public static final int STT_STATUS_DONE = 4;

    static final int TTS_ENGINE_MICROSOFT = 0;
    static final int TTS_ENGINE_GOOGLE = 1;
    static final int TTS_ENGINE_BAIDU = 2;
    static final int DEFAULT_TTS_ENGINE = TTS_ENGINE_BAIDU;

    public static final int BUFFERING_MODE_ENABLE_SVA_BUFFERING = 0;
    public static final int BUFFERING_MODE_DISABLE_AND_WITHOUT_ALERT = 1;
    public static final int BUFFERING_MODE_DISABLE_AND_WITH_ALERT = 2;

    public static final String RESET_BT_NAME = "Teddy";

    public static final int WARN_SOUND_TYPE_START = 0;
    public static final int WARN_SOUND_TYPE_FAIL = 1;
    public static final int WARN_SOUND_TYPE_WARNING = 2;
    private static MediaPlayer mPlayer;

    private static WisShellCommandHelper mShellCommandHelper;
    private static LedBlinkThread ledBlinkThread;
    private static DotLedBlinkThread dotLedBlinkThread;
    private static boolean isLEDLight = false;
    private static boolean isDotLEDLight = false;
    private static String CMD_LED_OPEN = "echo 1 > /sys/class/leds/ring/brightness";
    private static String CMD_LED_CLOSE = "echo 0 > /sys/class/leds/ring/brightness";
    private static String CMD_DOTLED_OPEN = "echo 1 > /sys/class/leds/dot/brightness";
    private static String CMD_DOTLED_CLOSE = "echo 0 > /sys/class/leds/dot/brightness";

    public static final int UIM_KEYWORD_TO_TRIGGER_HELLO_TEDDY = 0;
    public static final int UIM_KEYWORD_TO_TRIGGER_HEY_ALEXA = 1;
    public static final int UIM_KEYWORD_TO_TRIGGER_OKAY_GOOGLE = 2;

    public static final int WAKEUP_ENGINE_SVA = 0;
    public static final int WAKEUP_ENGINE_BAIDU = 1;

    // Settings parameters
    private static int mSttEngine = STT_ENGINE_BAIDU_ONLINE;
    private static int mTtsEngine = DEFAULT_TTS_ENGINE;
    private static int mSVALevel = DEFAULT_SVA_LEVEL;
    private static int mWifiSettingsMode = DEFAULT_WIFI_SETTINGS_MODE;
    private static boolean isEnableSyncNotification = DEFAULT_ENABLE_SYNC_NOTIFICATION_STATUS;
    private static int mSVASpeakerRecognitionStyle = DEFAULT_SVA_SPEAKER_RECOGNITION_STYLE;
    private static int mBufferingMode = BUFFERING_MODE_DISABLE_AND_WITH_ALERT;
    private static int mWifiSettingTriggerKeyCode = KeyEvent.KEYCODE_F12;
    private static int mUimKeywordToTrigger = UIM_KEYWORD_TO_TRIGGER_HELLO_TEDDY;
    private static int mWakeupEngine = WAKEUP_ENGINE_BAIDU;


    static {
        mCachedThreadPool = Executors.newCachedThreadPool();
    }

    public static void writeWaveFileHeader(FileInputStream in, OutputStream out) {
        try {
            long totalDataLen = in.getChannel().size();
            long totalAudioLen = totalDataLen + (44 - 8);
            Log.i("King", "totalDataLen = " + totalDataLen);
            long byteRate = SceneCommonHelper.RECORDER_BPP * SceneCommonHelper.RECORDER_SAMPLERATE * SceneCommonHelper.RECORDER_CHANNELS / 8;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS, byteRate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param out            wav output stream
     * @param totalAudioLen  total wav file length
     * @param totalDataLen   total data length, lack 44 bytes header
     * @param longSampleRate sample rate 16000 (16K HZ)
     * @param channels       channel 1: Mono 2: stereo
     * @param byteRate       byte rate
     * @throws IOException
     */
    public static void writeWaveFileHeader(
            OutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalAudioLen & 0xff);
        header[5] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[6] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[7] = (byte) ((totalAudioLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalDataLen & 0xff);
        header[41] = (byte) ((totalDataLen >> 8) & 0xff);
        header[42] = (byte) ((totalDataLen >> 16) & 0xff);
        header[43] = (byte) ((totalDataLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public static Map<String, String> getSingleParameters(String mConfigPath) {
        // TODO Auto-generated method stub
        Map<String, String> mParametersList = new LinkedHashMap<String, String>();
        String mReadLine;
        String mKey = "";
        String mValue = "";
        try {
            FileReader mFileReader = new FileReader(mConfigPath);
            BufferedReader mReader = new BufferedReader(mFileReader);
            while ((mReadLine = mReader.readLine()) != null) {
                if (!mReadLine.startsWith("#") && mReadLine.trim().length() > 0) {
                    String[] mKeyValuePair = mReadLine.split("=");
                    mKey = mKeyValuePair[0].trim();
                    mValue = mKeyValuePair[1].trim();
                    mParametersList.put(mKey, mValue);
                }
            }
            mReader.close();
            mFileReader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return mParametersList;
    }

    public static double countDb(short[] data) {
        float BASE = 32768f;
        float maxAmplitude = 0;

        for (int i = 0; i < data.length; i++) {
            maxAmplitude += data[i] * data[i];
        }
        maxAmplitude = (float) Math.sqrt(maxAmplitude / data.length);
        float ratio = maxAmplitude / BASE;
        float db = 0;
        if (ratio > 0) {
            db = (float) (20 * Math.log10(ratio)) + 100;
        }

        return db;
    }

    public static short getShort(byte[] buf, boolean bBigEnding) {
        if (buf == null) {
            throw new IllegalArgumentException("byte array is null!");
        }
        if (buf.length > 2) {
            throw new IllegalArgumentException("byte array size > 2 !");
        }
        short r = 0;
        if (bBigEnding) {
            for (int i = 0; i < buf.length; i++) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        } else {
            for (int i = buf.length - 1; i >= 0; i--) {
                r <<= 8;
                r |= (buf[i] & 0x00ff);
            }
        }
        return r;
    }

    public static short[] Bytes2Shorts(byte[] buf) {
        byte bLength = 2;
        short[] s = new short[buf.length / bLength];
        for (int iLoop = 0; iLoop < s.length; iLoop++) {
            byte[] temp = new byte[bLength];
            for (int jLoop = 0; jLoop < bLength; jLoop++) {
                temp[jLoop] = buf[iLoop * bLength + jLoop];
            }
            s[iLoop] = getShort(temp, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
        }
        return s;
    }

    public static void playSpeakingSound(final Context context, final int soundType) {
        playSpeakingSound(context, soundType, true);
    }

    public static void playSpeakingSound(final Context context, final int soundType, boolean changeLED) {
        playSpeakingSound(context, soundType, true, false);
    }

    public static void playSpeakingSound(final Context context, final int soundType, boolean changeLED, boolean isWaitForFinish) {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setLooping(false);
        }

        if (mPlayer.isPlaying()) {
            mPlayer.stop();
        }

        try {
            AssetFileDescriptor mDescriptor = null;
            if (soundType == WARN_SOUND_TYPE_FAIL) {
                mDescriptor = context.getAssets().openFd("osmium.wav");
                if (changeLED) {
                    closeLED();
                }
            } else if (soundType == WARN_SOUND_TYPE_WARNING) {
                mDescriptor = context.getAssets().openFd("carina.wav");
                if (changeLED) {
                    blinkLED();
                }
            } else {
                mDescriptor = context.getAssets().openFd("succeed.wav");
                if (changeLED) {
                    blinkLED();
                }
            }
            mPlayer.reset();
            mPlayer.setDataSource(mDescriptor.getFileDescriptor(), mDescriptor.getStartOffset(),
                    mDescriptor.getLength());
            mPlayer.prepare();
            mPlayer.start();
            while (isWaitForFinish && mPlayer.isPlaying()) {
                Thread.sleep(500l);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class LedBlinkThread extends Thread {
        public LedBlinkThread() {
            super();
        }

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (isLEDLight) {
                        closeLED(true);
                    } else {
                        openLED(true);
                    }
                    Thread.sleep(1 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                isLEDLight = !isLEDLight;
            }
            Log.i("King", "LED STATUS --> LED blink finish");
        }
    }

    public static void blinkLED() {
        isLEDLight = false;
        if (ledBlinkThread != null) {
            ledBlinkThread.interrupt();
        }
        ledBlinkThread = new LedBlinkThread();
        ledBlinkThread.start();
    }

    public static void openLED() {
        openLED(false);
    }

    /* Reserved */
    private static void openLED(boolean isBlink) {
        /*if (mShellCommandHelper == null){
            mShellCommandHelper = new WisShellCommandHelper();
        }
        if (!isBlink) {
            if (ledBlinkThread != null) {
                ledBlinkThread.interrupt();
            }
        }
        Log.i("King", "LED STATUS --> To open LED...");
        mShellCommandHelper.exec(CMD_LED_OPEN);*/
    }

    /* Reserved*/
    public static void closeLED() {
        /*closeLED(false);*/
    }

    /* Reserved */
    private static void closeLED(boolean isBlink) {
        /*if (mShellCommandHelper == null){
            mShellCommandHelper = new WisShellCommandHelper();
        }
        if (!isBlink) {
            if (ledBlinkThread != null) {
                ledBlinkThread.interrupt();
            }
        }
        Log.i("King", "LED STATUS --> To close LED...");
        mShellCommandHelper.exec(CMD_LED_CLOSE);*/
    }

    //berlin added for circle dot led ...twinkling~~~ blinking~~~
    private static class DotLedBlinkThread extends Thread {
        public DotLedBlinkThread() {
            super();
        }

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    if (isDotLEDLight) {
                        closeDotLED(true);
                    } else {
                        openDotLED(true);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                isDotLEDLight = !isDotLEDLight;
            }
            Log.i("King", "LED STATUS --> Circle Dot LED blink finish");
        }
    }

    /* Reserved */
    public static void twinkleDotLED() {
        /*isDotLEDLight = false;
        if (dotLedBlinkThread != null) {
            dotLedBlinkThread.interrupt();
        }
        dotLedBlinkThread = new DotLedBlinkThread();
        dotLedBlinkThread.start();*/
    }


    public static void openDotLED() {
        openDotLED(false);
    }

    /* Reserved */
    private static void openDotLED(boolean isBlink) {
        /*if (mShellCommandHelper == null){
            mShellCommandHelper = new WisShellCommandHelper();
        }
        if (!isBlink) {
            if (dotLedBlinkThread != null) {
                dotLedBlinkThread.interrupt();
            }
        }
        Log.i("King", "LED STATUS --> To open Circle Dot LED...");
        mShellCommandHelper.exec(CMD_DOTLED_OPEN);*/
    }

    /* Reserved */
    public static void closeDotLED() {
        /*closeDotLED(false);*/
    }

    /* Reserved*/
    private static void closeDotLED(boolean isBlink) {
        /*if (mShellCommandHelper == null){
            mShellCommandHelper = new WisShellCommandHelper();
        }
        if (!isBlink) {
            if (dotLedBlinkThread != null) {
                dotLedBlinkThread.interrupt();
            }
        }
        Log.i("King", "LED STATUS --> To closeCircle Dot LED...");
        mShellCommandHelper.exec(CMD_DOTLED_CLOSE);*/
    }

    public static String getSpeakingLanguageSetting(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String speakingLanguage = sharedPreferences.getString(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE, context.getString(R.string.preference_speaking_language_default_value));
        return speakingLanguage;
    }

    public static int getSttEngine() {
        Log.i("King", "STT engine: " + mSttEngine + ". [0= Microsoft; 1= Google local app; 2= Google cloud speech; 3= Baidu online]");
        return mSttEngine;
    }

    public static int getTtsEngine() {
        return mTtsEngine;
    }

    public static int getWifiSettingsMode() {
        return mWifiSettingsMode;
    }

    public static int getSVALevel() {
        return mSVALevel;
    }

    public static int getSvaSpeakerRecognitionStyle() {
        return mSVASpeakerRecognitionStyle;
    }

    public static boolean isEnableSyncNotification() {
        return isEnableSyncNotification;
    }

    public static int getBufferingMode() {
        return mBufferingMode;
    }

    public static int getWifiSettingTriggerKeyCode() {
        return mWifiSettingTriggerKeyCode;
    }

    public static int getUimKeywordToTriggerAction() {
        return mUimKeywordToTrigger;
    }

    public static int getmWakeupEngine() {
        return mWakeupEngine;
    }

    public static void initSettingsParameters() {
        Map<String, String> mParametersList = getSingleParameters(STORAGE_CONFIG_FILE_PATH);
        if (mParametersList != null && mParametersList.size() > 0) {
            for (String key : mParametersList.keySet()) {
                if (key.equals(CONFIG_KEY_BUFFERING_MODE)) {
                    mBufferingMode = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_STT_ENGINE)) {
                    mSttEngine = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_TTS_ENGINE)) {
                    mTtsEngine = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_WIFI_SETTINGS_MODE)) {
                    mWifiSettingsMode = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_ENABLE_SYNC_NOTIFICATION)) {
                    isEnableSyncNotification = Integer.parseInt(mParametersList.get(key)) == 1;
                } else if (key.equals(CONFIG_KEY_SVA_LEVEL)) {
                    mSVALevel = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_SVA_SPEAKER_RECOGNITION_STYLE)) {
                    mSVASpeakerRecognitionStyle = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_WIFI_SETTINGS_TRIGGER_KEY_CODE)) {
                    mWifiSettingTriggerKeyCode = Integer.parseInt(mParametersList.get(key));
                } else if (key.equals(CONFIG_KEY_UIM_KEYWORD_TO_TRIGGER)){
                    mUimKeywordToTrigger = Integer.parseInt(mParametersList.get(key));
                } else if(key.equals(CONFIG_KEY_WAKEUP_ENGINE)){
                    mWakeupEngine = Integer.parseInt(mParametersList.get(key));
                }
            }
        }
    }

    public static ArrayList<BtConfigDevice> getConfigDevices(Context context) {
        ArrayList<BtConfigDevice> configDevices = new ArrayList<>();
        try {
            String mReadLine;
            // FileReader mFileReader = new FileReader(STORAGE_SVA_BT_CONFIG_FILE_PATH); // localConfig
            FileReader mFileReader = new FileReader(new File(context.getFilesDir(), SceneCommonHelper.REMOTE_SVA_BT_FILE)); // cloudConfig
            BufferedReader mReader = new BufferedReader(mFileReader);
            while ((mReadLine = mReader.readLine()) != null) {
                if (!mReadLine.startsWith("#") && mReadLine.trim().length() > 0) {
                    String[] mUserBTEmailPair = mReadLine.split(",");
                    if (mUserBTEmailPair.length >= 3) {
                        configDevices.add(new BtConfigDevice(mUserBTEmailPair[0], mUserBTEmailPair[1], mUserBTEmailPair[2]));
                    }
                }
            }
            mReader.close();
            mFileReader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return configDevices;
    }

    public static String getString(Context context, int stringId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String recognitionLanguage = sharedPreferences.getString(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE,
                context.getString(R.string.preference_recognition_language_default_value));
        if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_CHINESE_CN)) {
            stringId++;
        }
        /* reserved */
        /*else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_FRANCE_FR)){
            stringId += 2;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_GERMANY)){
            stringId += 3;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_ITALIA)){
            stringId += 4;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_ESPANOL_ES)){
            stringId += 5;
        }*/
        return context.getString(stringId);
    }

    public static String[] getStringArray(Context context, int stringId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String recognitionLanguage = sharedPreferences.getString(SceneSettingsFragment.KEY_SPEAKING_LANGUAGE,
                context.getString(R.string.preference_recognition_language_default_value));
        if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_CHINESE_CN)) {
            stringId++;
        }
        /* reserved */
        /*else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_FRANCE_FR)){
            stringId += 2;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_GERMANY)){
            stringId += 3;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_ITALIA)){
            stringId += 4;
        }else if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_ESPANOL_ES)){
            stringId += 5;
        }*/
        return context.getResources().getStringArray(stringId);
    }

    public static final HashMap<String, String> mSpeakingLanguageRegionPairs = new HashMap<String, String>() {
        {
            put("chinese", CommonHelper.LanguageRegion.REGION_CHINESE_CN);
            put("中文", CommonHelper.LanguageRegion.REGION_CHINESE_CN);
            put("汉语", CommonHelper.LanguageRegion.REGION_CHINESE_CN);

            put("english", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
            put("英文", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
            put("英语", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
            put("美语", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
        }
    };

    public static final HashMap<String, String> mRecognitionLanguageRegionPairs = (HashMap<String, String>) mSpeakingLanguageRegionPairs.clone();
    /*public static final HashMap<String, String> mRecognitionLanguageRegionPairs= new HashMap<String, String>() {
        {
            put("chinese", CommonHelper.LanguageRegion.REGION_CHINESE_CN);
            put("中文", CommonHelper.LanguageRegion.REGION_CHINESE_CN);
            put("english", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
            put("英文", CommonHelper.LanguageRegion.REGION_ENGLISH_US);
        }
    };*/

    public static String removeBlackChar(String text, String mAudioLanguage) {
        String result = text;
        // 中文去空格, 去換行
        if (mAudioLanguage.equals(LanguageCodes.ChineseSimplified)
                || mAudioLanguage.equals(LanguageCodes.ChineseTraditional)
                || mAudioLanguage.equals(LanguageCodes.Japanese)) {
            result = result.replaceAll("\\s*", "");
        } else if (mAudioLanguage.equals(LanguageCodes.English)) {
            result = result.replaceAll("[\\f\\n\\r\\t]+", " ");
        }
        return result;
    }

    /**
     * Push shell files to device
     */
    public static void pushLedShellFiles(Context context) {
        LightLed.SHELL_PATH = context.getFilesDir().toString();
        try {
            String[] mPathList = context.getAssets().list("Led");
            if (mPathList != null && mPathList.length > 0) {
                for (String path : mPathList) {
                    pushAssetsFileToDevice(context, "Led", path,
                            context.getFilesDir().toString(), true);
                }
            }
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private static void pushAssetsFileToDevice(Context context, String assetsFolder, String assetsFileName,
                                               String desPath, boolean overwriteIfExist) {
        File mDesFile = new File(String.format("%1$s/%2$s", desPath, assetsFileName));
        if (!overwriteIfExist && mDesFile.exists()) {
            return;
        } else {
            try {
                if (overwriteIfExist && mDesFile.exists()) {
                    mDesFile.delete();
                }

                try {
                    if (!mDesFile.getParentFile().exists()) {
                        mDesFile.getParentFile().mkdirs();
                    }
                    mDesFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mDesFile.setExecutable(true, false);
                mDesFile.setWritable(true, false);
                mDesFile.setReadable(true, false);

                InputStream mInputStream = context.getAssets().open(
                        assetsFolder == null ? assetsFileName : assetsFolder
                                + File.separator + assetsFileName);
                OutputStream mOutputStream = new FileOutputStream(mDesFile);
                byte[] result = new byte[1024];
                do {
                    int readSize = mInputStream.read(result);
                    if (readSize == -1) {
                        break;
                    }
                    mOutputStream.write(result, 0, readSize);
                } while (true);
                mInputStream.close();
                mOutputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    // Pando
    public static boolean getPandoMicroMute(Context context) {
        SharedPreferences pandoPreference = context.getSharedPreferences(SceneCommonHelper.PREFERENCE_PANDO_NAME, Context.MODE_PRIVATE);
        return pandoPreference.getBoolean(SceneCommonHelper.PREFERENCE_PANDO_KEY_MUTE, false);
    }

    public static void setPandoMicroMute(Context context, boolean isPandoMicroMute) {
        SharedPreferences.Editor pandoPreference = context.getSharedPreferences(SceneCommonHelper.PREFERENCE_PANDO_NAME, Context.MODE_PRIVATE).edit();
        pandoPreference.putBoolean(SceneCommonHelper.PREFERENCE_PANDO_KEY_MUTE, isPandoMicroMute);
        pandoPreference.commit();
    }
}
