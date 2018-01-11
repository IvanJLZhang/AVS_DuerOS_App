package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;
import com.microsoft.speech.tts.Synthesizer;
import com.microsoft.speech.tts.Voice;
import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.led_control.LedController;
import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.SceneSettingsFragment;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by king on 16-4-27.
 */
public class ToSpeak {
    private Context context;
    private static ToSpeak instance;
    private int mCurrentTtsEngine = -1;

    private boolean isGoogleTtsInited = false;
    private ArrayList<String> mGoogleTtsNeedToReadMsg = new ArrayList<>();

    // Microsoft TTS service
    private SpeakTask mSpeakTask;
    private static Synthesizer m_syn = null;
    private static Voice voice = null;
    private static AudioTrack audioTrack;

    private boolean isStopped;

    // Google TTS service
    private TextToSpeech mGoogleTtsEngine;

    // Baidu TTS service
    SpeechSynthesizer speechSynthesizer;
    BaiduSpeechListener baiduListener;

    private ToSpeak(Context context) {
        this.context = context;
    }

    public static ToSpeak getInstance(Context context) {
        if (instance == null) {
            instance = new ToSpeak(context);
        }
        return instance;
    }

    public ToSpeak init() {
        mCurrentTtsEngine = SceneCommonHelper.getTtsEngine();
        if (mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_GOOGLE) {
            isGoogleTtsInited = false;
            mGoogleTtsEngine = new TextToSpeech(context, googleTtsInitListener);
        } else if(mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_BAIDU){
            baiduListener = new BaiduSpeechListener();
            this.speechSynthesizer = SpeechSynthesizer.getInstance();
            this.speechSynthesizer.setContext(context);
            this.speechSynthesizer.setSpeechSynthesizerListener(baiduListener);
            this.speechSynthesizer.setApiKey(context.getString(SubscriptionKey.getmBaiduAsrAuthInfo()[1]),
                    context.getString(SubscriptionKey.getmBaiduAsrAuthInfo()[2]));
            this.speechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
            this.speechSynthesizer.initTts(TtsMode.ONLINE);// online mode
            isGoogleTtsInited = true;
        }
        else{
            isGoogleTtsInited = true;
        }
        return instance;
    }

    private TextToSpeech.OnInitListener googleTtsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            isGoogleTtsInited = true;

            Log.i("King", "Google TTS init status  = " + status + " [0 is SUCCESS; -1 is ERROR; -2 is STOPPED]");
            mGoogleTtsEngine.setPitch(0.8f);
            if (mGoogleTtsNeedToReadMsg.size() > 0) {
                String recognitionLanguage = getRecognitionLanguage();
                if (setGoogleTtsLanguage(recognitionLanguage)) {
                    LedController.lightFlashLed(LedController.COLORS.BLUE, LedController.COLORS.RED);

                    for (String msg : mGoogleTtsNeedToReadMsg) {
                        startGoogleSpeak(msg, false);
                    }

                    LedController.closeLed(context);
                }
            }
            mGoogleTtsNeedToReadMsg.clear();
        }
    };

    public void toSpeak(String content, boolean isAsyncToSpeak) {
        if (!isGoogleTtsInited) {
            mGoogleTtsNeedToReadMsg.add(content);
            return;
        }
        isStopped = false;
        String recognitionLanguage = getRecognitionLanguage();
        Log.i("King", "current tts engine is " + mCurrentTtsEngine + " [0:microsoft 1:google 2:baidu]");
        if (mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_GOOGLE) { // Google TTS engine
            if (setGoogleTtsLanguage(recognitionLanguage)) {
                LedController.lightFlashLed(LedController.COLORS.BLUE, LedController.COLORS.RED);

                startGoogleSpeak(content, isAsyncToSpeak);

                LedController.closeLed(context);
            }
        }else if(mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_BAIDU){
            int result = this.speechSynthesizer.speak(content);
            if(result < 0){
                Log.e("Ivan", "error,please look up error code in doc or URL:http://yuyin.baidu.com/docs/tts/122");
            }
        }
        else {   // Microsoft TTS engine
            String text = CommonHelper.getSpeakContent(content);
            if (isAsyncToSpeak) {
                mSpeakTask = new SpeakTask();
                mSpeakTask.execute(text, recognitionLanguage);
            } else {
                startSpeak(text, recognitionLanguage);
            }
        }
    }

    private String getRecognitionLanguage() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(SceneSettingsFragment.KEY_RECOGNITION_LANGUAGE,
                context.getString(R.string.preference_recognition_language_default_value));
    }

    private boolean setGoogleTtsLanguage(String recognitionLanguage) {
        int setTtsLanguageResult = -1;
        if (recognitionLanguage.equals(CommonHelper.LanguageRegion.REGION_CHINESE_CN)) {
            Log.i("King", "Trying to set the TTS language to Chinese!");
            setTtsLanguageResult = mGoogleTtsEngine.setLanguage(Locale.CHINA);
        } else {
            Log.i("King", "Trying to set the TTS language to english!");
            setTtsLanguageResult = mGoogleTtsEngine.setLanguage(Locale.US);
        }
        if (setTtsLanguageResult == TextToSpeech.LANG_MISSING_DATA || setTtsLanguageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.i("King", "The language is not supported!");
            return false;
        } else {
            Log.i("King", "TTS language set success!");
            return true;
        }
    }

    private void startGoogleSpeak(String content, boolean isAsyncToSpeak) {
        isStopped = false;
        Log.i("King", "ToSpeak:: start to speak..... " + content);

        if (isAsyncToSpeak) {
            mGoogleTtsEngine.speak(content, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            mGoogleTtsEngine.speak(content, TextToSpeech.QUEUE_ADD, null, null);
            boolean isCheckSpeakingAgain = false;
            do {
                try {
                    Thread.sleep(500);
                    if (!mGoogleTtsEngine.isSpeaking()) {
                        if (isCheckSpeakingAgain) {
                            break;
                        } else {
                            isCheckSpeakingAgain = true;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (mGoogleTtsEngine.isSpeaking() || isCheckSpeakingAgain);
        }

        isStopped = true;
        Log.i("King", "ToSpeak:: end to speak.....");

    }

    private void startSpeak(String content, String language) {
        LedController.lightFlashLed(LedController.COLORS.BLUE, LedController.COLORS.RED);
        Log.i("King", "ToSpeak:: init for speak.....");
        if (m_syn == null) {
            String clientId = context.getString(SubscriptionKey.getSpeechPrimaryKey());
            m_syn = new Synthesizer(clientId);
            m_syn.SetServiceStrategy(Synthesizer.ServiceStrategy.AlwaysService);
            Log.i("King", "m_syn initial... with clientId = " + clientId);
        }

        if (voice != null) {
            if (!voice.lang.equals(language)) {
                voice = null;
            }
        }
        if (voice == null) {
            String voiceGender = CommonHelper.mRegionGenderPairs.get(language);
            if (voiceGender.equals(CommonHelper.VoiceGender.GENDER_BOTH)) {
                voiceGender = CommonHelper.VoiceGender.GENDER_FEMALE;
            }
            voice = CommonHelper.getTtsVoice(language, voiceGender);
        }

        m_syn.SetVoice(voice, null);
        if (audioTrack == null) {
            audioTrack = new AudioTrack(3, 16000, 2, 2, AudioTrack.getMinBufferSize(16000, 2, 2), 1);
        }
        if (!isStopped) {
            Log.i("King", "ToSpeak:: get sound data start .....");
            Log.i("King", "ToSpeak:: content = " + content);
            byte[] sound = m_syn.SpeakSSML(CommonHelper.formatMicrosoftTTSToSSML(voice, content));
            Log.i("King", "ToSpeak:: get sound data end ....." + (sound == null ? "No data!!!" : " size = " + sound.length));
            Log.i("King", "ToSpeak:: start to speak.....");
            if (sound != null && sound.length != 0) {
                if (audioTrack.getState() == 1) {
                    audioTrack.play();
                    audioTrack.write(sound, 0, sound.length);
                    if (audioTrack != null && audioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                        audioTrack.stop();
                    }
                }
            }
        }
        isStopped = true;
        Log.i("King", "ToSpeak:: end to speak.....");
        LedController.closeLed(context);
    }


    private  class BaiduSpeechListener implements SpeechSynthesizerListener{
        @Override
        public void onSynthesizeStart(String s) {
            Log.i("Ivan", "start to synthesize.");
        }

        @Override
        public void onSynthesizeDataArrived(String s, byte[] bytes, int i) {
            //Log.i("Ivan", "synt. data arrived: " + s);
        }

        @Override
        public void onSynthesizeFinish(String s) {
            Log.i("Ivan", "synt. finished " + s);
        }

        @Override
        public void onSpeechStart(String s) {
            Log.i("Ivan", "speech start.");
        }

        @Override
        public void onSpeechProgressChanged(String s, int i) {
            //Log.i("Ivan", "speech process:" + i);
        }

        @Override
        public void onSpeechFinish(String s) {
            Log.i("Ivan", "speech finished.");
        }

        @Override
        public void onError(String s, SpeechError speechError) {
            Log.i("Ivan", "speech error " + s + speechError.toString());
        }
    }



    private class SpeakTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            startSpeak(params[0], params[1]);
            return null;
        }
    }

    public void stop() {
        isStopped = true;
        Log.i("King", "stop tts engine start... mCurrentTtsEngine = " + mCurrentTtsEngine);
        if (mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_GOOGLE) {
            if (mGoogleTtsEngine != null) {
                mGoogleTtsEngine.stop();
                //mGoogleTtsEngine.shutdown();
            }
        }
        else if(mCurrentTtsEngine == SceneCommonHelper.TTS_ENGINE_BAIDU){
            if(speechSynthesizer != null){
                speechSynthesizer.stop();
            }
        }
        else {
            if (mSpeakTask != null) {
                mSpeakTask.cancel(true);
            }
            if (audioTrack != null) {
                if (AudioTrack.PLAYSTATE_PLAYING == audioTrack.getPlayState()
                        || AudioTrack.PLAYSTATE_PAUSED == audioTrack.getPlayState()) {
                    audioTrack.stop();
                }
            /*audioTrack.release();
            audioTrack = null;*/
            }
        }
        Log.i("King", "stop tts engine end...");
    }
}
