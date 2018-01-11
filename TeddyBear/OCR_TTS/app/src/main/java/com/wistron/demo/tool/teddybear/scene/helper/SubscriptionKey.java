package com.wistron.demo.tool.teddybear.scene.helper;

import com.wistron.demo.tool.teddybear.R;

import java.text.SimpleDateFormat;
import java.util.Random;

/**
 * Created by king on 16-4-27.
 */
public class SubscriptionKey {
    private static SimpleDateFormat yyyyMMddDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static String SPEECH_KEY_OVERTIME_DATE = "2016-07-20";

    private static final int[] mComputerVisionKeys = {
            R.string.azure_computer_vision_subscription_key
    };

    private static final int[] mSpeechPrimaryKeys = {
            R.string.azure_speech_subscription_primary_key
    };
    /*private static final int[] mSpeechSecondaryKeys = {
            R.string.azure_speech_subscription_secondary_key
    };*/

    // it's overtime on 2016/7/28; Notice: For demo tool, pls don't modify this array, it should keep a fixed value.
    private static final int[] mSpeechPrimaryKeysTemp = {
            R.string.speech_subscription_primary_key_temp
    };
    // it's overtime on 2016/7/28; Notice: For demo tool, pls don't modify this array, it should keep a fixed value.
    private static final int[] mSpeechSecondaryKeysTemp = {
            R.string.speech_subscription_secondary_key_temp
    };

    private static final int[] mFaceKeys = {
            R.string.azure_face_subscription_key
    };
    private static final int[] mEmotionKeys = {
            R.string.azure_emotion_subscription_key
    };
    // Notice: Speaker recognition key must be a fixed value.
    private static final int[] mSpeakerRecognitionKeys = {
            R.string.azure_speaker_recognition_subscription_key
    };
    private static final int[] mAzureTextTranslatorKeys = {
            R.string.azure_translator_subscription_key
    };


    public static int[] getmBaiduAsrAuthInfo() {
        return mBaiduAsrAuthInfo;
    }

    private static final int[] mBaiduAsrAuthInfo = {
            R.string.baidu_online_api_appid,
            R.string.baidu_online_api_key,
            R.string.baidu_online_api_secret
    };

    public static int getComputerVisionKey() {
        return getRandomKey(mComputerVisionKeys);
    }

    public static int getSpeechPrimaryKey() {
        /*String curDate = yyyyMMddDateFormat.format(new Date(System.currentTimeMillis()));
        if (curDate.compareTo(SPEECH_KEY_OVERTIME_DATE) < 0) {
            return getRandomKey(mSpeechPrimaryKeysTemp);
        } else {*/
        return getRandomKey(mSpeechPrimaryKeys);
        //}
    }

    public static int getSpeechAuthenticationUri() {
        return R.string.authenticationUri;
    }

    /*public static int getSpeechSecondaryKey() {
        String curDate = yyyyMMddDateFormat.format(new Date(System.currentTimeMillis()));
        if (curDate.compareTo(SPEECH_KEY_OVERTIME_DATE) < 0) {
            return getRandomKey(mSpeechSecondaryKeysTemp);
        } else {
        return getRandomKey(mSpeechSecondaryKeys);
        }
    }*/

    public static int getFaceKey() {
        return getRandomKey(mFaceKeys);
    }

    public static int getEmotionKey() {
        return getRandomKey(mEmotionKeys);
    }

    public static int getSpeakerRecognitionKey() {
        return getRandomKey(mSpeakerRecognitionKeys);
    }

    public static int getAzureTextTranslateKey() {
        return getRandomKey(mAzureTextTranslatorKeys);
    }

    private static int getRandomKey(int[] keys) {
        Random mRandom = new Random(System.currentTimeMillis());
        return keys[mRandom.nextInt(keys.length)];
    }

    public static int getLuisSubscriptionKey() {
        return R.string.luis_subscription_key;
    }

    public static int getLuisMainAppId() {
        return R.string.luis_main_app_id;
    }

    public static int getLuisMemoGameAppId() {
        return R.string.luis_memo_game_app_id;
    }

    public static int getYoutubeDeveloperKey() {
        return R.string.google_youtube_developer_key;
    }
}
