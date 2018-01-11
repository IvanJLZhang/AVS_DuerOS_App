package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.speech.tts.Voice;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsSettingsFragment;

import java.util.HashMap;

/**
 * Created by king on 16-3-22.
 */
public class CommonHelper {
    public static final String EXTRA_AUDIO_LANGUAGE = "audio_language";
    public static final String PUNCTUATION_CONTAINS = "!,;:?.！，；：？。";

    public static class LanguageRegion {  // Baidu || Microsoft
        public static final String REGION_ENGLISH_US = "en-US";
        public static final String REGION_ENGLISH_AU = "en-AU";
        public static final String REGION_ENGLISH_CA = "en-CA";
        public static final String REGION_ENGLISH_IN = "en-IN";
        public static final String REGION_ENGLISH_GB = "en-GB";
        public static final String REGION_CHINESE_CN = "zh-CN";
        public static final String REGION_CHINESE_TW = "zh-TW";
        public static final String REGION_CHINESE_HK = "zh-HK";
        public static final String REGION_JAPAN = "ja-JP";
        public static final String REGION_GERMANY = "de-DE";
        public static final String REGION_FRANCE_FR = "fr-FR";
        public static final String REGION_FRANCE_CA = "fr-CA";
        public static final String REGION_PORTUGUESE = "pt-BR";
        public static final String REGION_ITALIA = "it-IT";
        public static final String REGION_ESPANOL_ES = "es-ES";
        public static final String REGION_ESPANOL_MX = "es-MX";
        public static final String REGION_RUSSIAN = "ru-RU";
    }

    public static class VoiceGender {
        public static final String GENDER_MALE = "0";
        public static final String GENDER_FEMALE = "1";
        public static final String GENDER_BOTH = GENDER_MALE + GENDER_FEMALE;
    }

    public static class GoogleLanguageRegion { // Google
        public static final String REGION_Bengali = "bn_BD";
        public static final String REGION_Chinese_China = "zh_CN";
        public static final String REGION_Chinese_Taiwan = "zh_TW";
        public static final String REGION_Danish = "da_DK";
        public static final String REGION_Dutch = "nl_NL";
        public static final String REGION_English_Australia = "en_AU";
        public static final String REGION_English_India = "en_IN";
        public static final String REGION_English_UK = "en_GB";
        public static final String REGION_English_US = "en_US";
        public static final String REGION_Finnish = "fi_FI";
        public static final String REGION_French = "fr_FR";
        public static final String REGION_German = "de_DE";
        public static final String REGION_Hindi = "hi_IN";
        public static final String REGION_Hungarian = "hu_HU";
        public static final String REGION_Indonesian = "in_ID";
        public static final String REGION_Italian = "it_IT";
        public static final String REGION_Japanese = "ja_JP";
        public static final String REGION_Korean = "ko_KR";
        public static final String REGION_Norwegian = "nb_NO";
        public static final String REGION_Polish = "pl_PL";
        public static final String REGION_Portuguese = "pt_BR";
        public static final String REGION_Russian = "ru_RU";
        public static final String REGION_Spanish_Spain = "es_ES";
        public static final String REGION_Spanish_US = "es_US";
        public static final String REGION_Swedish = "sv_SE";
        public static final String REGION_Thai = "th_TH";
        public static final String REGION_Turkish = "tr_TR";
        public static final String REGION_Vietnamese = "vi_VN";
    }

    // 語種 Region 匹配對
    public static final HashMap<String, String> mLanguageRegionPair = new HashMap<String, String>() {
        {
            put(LanguageCodes.ChineseSimplified, LanguageRegion.REGION_CHINESE_CN);
            put(LanguageCodes.ChineseTraditional, LanguageRegion.REGION_CHINESE_TW);
            put(LanguageCodes.English, LanguageRegion.REGION_ENGLISH_US);
            put(LanguageCodes.French, LanguageRegion.REGION_FRANCE_FR);
            put(LanguageCodes.German, LanguageRegion.REGION_GERMANY);
            put(LanguageCodes.Italian, LanguageRegion.REGION_ITALIA);
            put(LanguageCodes.Japanese, LanguageRegion.REGION_JAPAN);
            put(LanguageCodes.Portuguese, LanguageRegion.REGION_PORTUGUESE);
            put(LanguageCodes.Russian, LanguageRegion.REGION_RUSSIAN);
            put(LanguageCodes.Spanish, LanguageRegion.REGION_ESPANOL_ES);
        }
    };

    // Region Gender 匹配對
    public static final HashMap<String, String> mRegionGenderPairs = new HashMap<String, String>() {
        {
            put(LanguageRegion.REGION_ENGLISH_US, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_ENGLISH_AU, VoiceGender.GENDER_FEMALE);
            put(LanguageRegion.REGION_ENGLISH_CA, VoiceGender.GENDER_FEMALE);
            put(LanguageRegion.REGION_ENGLISH_IN, VoiceGender.GENDER_MALE);
            put(LanguageRegion.REGION_ENGLISH_GB, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_CHINESE_CN, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_CHINESE_TW, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_CHINESE_HK, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_JAPAN, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_GERMANY, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_FRANCE_FR, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_FRANCE_CA, VoiceGender.GENDER_FEMALE);
            put(LanguageRegion.REGION_PORTUGUESE, VoiceGender.GENDER_MALE);
            put(LanguageRegion.REGION_ITALIA, VoiceGender.GENDER_MALE);
            put(LanguageRegion.REGION_ESPANOL_ES, VoiceGender.GENDER_BOTH);
            put(LanguageRegion.REGION_ESPANOL_MX, VoiceGender.GENDER_MALE);
            put(LanguageRegion.REGION_RUSSIAN, VoiceGender.GENDER_BOTH);
        }
    };

    // OCR語種  百度翻譯 匹配對
    public static final HashMap<String, String> mLanguageBaiduTranslatePairs = new HashMap<String, String>() {
        {
            put(LanguageCodes.ChineseSimplified, BaiduTranslate.BAIDU_LANGUAGE_CHINESE_SIMPLIFIED);
            put(LanguageCodes.ChineseTraditional, BaiduTranslate.BAIDU_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageCodes.Czech, BaiduTranslate.BAIDU_LANGUAGE_CZECH);
            put(LanguageCodes.Danish, BaiduTranslate.BAIDU_LANGUAGE_DANISH);
            put(LanguageCodes.Dutch, BaiduTranslate.BAIDU_LANGUAGE_DUTCH);
            put(LanguageCodes.English, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageCodes.Finnish, BaiduTranslate.BAIDU_LANGUAGE_FINNISH);
            put(LanguageCodes.French, BaiduTranslate.BAIDU_LANGUAGE_FRANCE);
            put(LanguageCodes.German, BaiduTranslate.BAIDU_LANGUAGE_GERMANY);
            put(LanguageCodes.Greek, BaiduTranslate.BAIDU_LANGUAGE_GREEK);
            put(LanguageCodes.Hungarian, BaiduTranslate.BAIDU_LANGUAGE_HUNGARIAN);
            put(LanguageCodes.Italian, BaiduTranslate.BAIDU_LANGUAGE_ITALIANO);
            put(LanguageCodes.Japanese, BaiduTranslate.BAIDU_LANGUAGE_JAPANESE);
            put(LanguageCodes.Korean, BaiduTranslate.BAIDU_LANGUAGE_KOREAN);
            //put(LanguageCodes.Norwegian, BaiduTranslate.BAIDU_LANGUAGE_NORWEGIAN); //百度翻譯暫不支持挪威語
            put(LanguageCodes.Polish, BaiduTranslate.BAIDU_LANGUAGE_POLISH);
            put(LanguageCodes.Portuguese, BaiduTranslate.BAIDU_LANGUAGE_PORTUGUESE);
            put(LanguageCodes.Russian, BaiduTranslate.BAIDU_LANGUAGE_RUSSIAN);
            put(LanguageCodes.Spanish, BaiduTranslate.BAIDU_LANGUAGE_ESPANOL);
            put(LanguageCodes.Swedish, BaiduTranslate.BAIDU_LANGUAGE_SWEDISH);
            //put(LanguageCodes.Turkish, BaiduTranslate.BAIDU_LANGUAGE_TURKISH);  //百度翻譯暫不支持土耳其語
        }
    };
    // Region 百度翻譯 匹配對
    public static final HashMap<String, String> mRegionBaiduTranslatePairs = new HashMap<String, String>() {
        {
            put(LanguageRegion.REGION_ENGLISH_US, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_AU, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_CA, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_IN, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_GB, BaiduTranslate.BAIDU_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_CHINESE_CN, BaiduTranslate.BAIDU_LANGUAGE_CHINESE_SIMPLIFIED);
            put(LanguageRegion.REGION_CHINESE_TW, BaiduTranslate.BAIDU_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageRegion.REGION_CHINESE_HK, BaiduTranslate.BAIDU_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageRegion.REGION_JAPAN, BaiduTranslate.BAIDU_LANGUAGE_JAPANESE);
            put(LanguageRegion.REGION_GERMANY, BaiduTranslate.BAIDU_LANGUAGE_GERMANY);
            put(LanguageRegion.REGION_FRANCE_FR, BaiduTranslate.BAIDU_LANGUAGE_FRANCE);
            put(LanguageRegion.REGION_FRANCE_CA, BaiduTranslate.BAIDU_LANGUAGE_FRANCE);
            put(LanguageRegion.REGION_PORTUGUESE, BaiduTranslate.BAIDU_LANGUAGE_PORTUGUESE);
            put(LanguageRegion.REGION_ITALIA, BaiduTranslate.BAIDU_LANGUAGE_ITALIANO);
            put(LanguageRegion.REGION_ESPANOL_ES, BaiduTranslate.BAIDU_LANGUAGE_ESPANOL);
            put(LanguageRegion.REGION_ESPANOL_MX, BaiduTranslate.BAIDU_LANGUAGE_ESPANOL);
            put(LanguageRegion.REGION_RUSSIAN, BaiduTranslate.BAIDU_LANGUAGE_RUSSIAN);
        }
    };

    // OCR語種  微软翻譯 匹配對
    public static final HashMap<String, String> mLanguageMicrosoftTranslatePairs = new HashMap<String, String>() {
        {
            put(LanguageCodes.ChineseSimplified, MicrosoftTranslate.MICROSOFT_LANGUAGE_CHINESE_SIMPLIFIED);
            put(LanguageCodes.ChineseTraditional, MicrosoftTranslate.MICROSOFT_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageCodes.Czech, MicrosoftTranslate.MICROSOFT_LANGUAGE_CZECH);
            put(LanguageCodes.Danish, MicrosoftTranslate.MICROSOFT_LANGUAGE_DANISH);
            put(LanguageCodes.Dutch, MicrosoftTranslate.MICROSOFT_LANGUAGE_DUTCH);
            put(LanguageCodes.English, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageCodes.Finnish, MicrosoftTranslate.MICROSOFT_LANGUAGE_FINNISH);
            put(LanguageCodes.French, MicrosoftTranslate.MICROSOFT_LANGUAGE_FRANCE);
            put(LanguageCodes.German, MicrosoftTranslate.MICROSOFT_LANGUAGE_GERMANY);
            put(LanguageCodes.Greek, MicrosoftTranslate.MICROSOFT_LANGUAGE_GREEK);
            put(LanguageCodes.Hungarian, MicrosoftTranslate.MICROSOFT_LANGUAGE_HUNGARIAN);
            put(LanguageCodes.Italian, MicrosoftTranslate.MICROSOFT_LANGUAGE_ITALIANO);
            put(LanguageCodes.Japanese, MicrosoftTranslate.MICROSOFT_LANGUAGE_JAPANESE);
            put(LanguageCodes.Korean, MicrosoftTranslate.MICROSOFT_LANGUAGE_KOREAN);
            put(LanguageCodes.Norwegian, MicrosoftTranslate.MICROSOFT_LANGUAGE_NORWEGIAN);
            put(LanguageCodes.Polish, MicrosoftTranslate.MICROSOFT_LANGUAGE_POLISH);
            put(LanguageCodes.Portuguese, MicrosoftTranslate.MICROSOFT_LANGUAGE_PORTUGUESE);
            put(LanguageCodes.Russian, MicrosoftTranslate.MICROSOFT_LANGUAGE_RUSSIAN);
            put(LanguageCodes.Spanish, MicrosoftTranslate.MICROSOFT_LANGUAGE_ESPANOL);
            put(LanguageCodes.Swedish, MicrosoftTranslate.MICROSOFT_LANGUAGE_SWEDISH);
            put(LanguageCodes.Turkish, MicrosoftTranslate.MICROSOFT_LANGUAGE_TURKISH);
        }
    };
    // Region 微软翻譯 匹配對
    public static final HashMap<String, String> mRegionMicrosoftTranslatePairs = new HashMap<String, String>() {
        {
            put(LanguageRegion.REGION_ENGLISH_US, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_AU, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_CA, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_IN, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_ENGLISH_GB, MicrosoftTranslate.MICROSOFT_LANGUAGE_ENGLISH);
            put(LanguageRegion.REGION_CHINESE_CN, MicrosoftTranslate.MICROSOFT_LANGUAGE_CHINESE_SIMPLIFIED);
            put(LanguageRegion.REGION_CHINESE_TW, MicrosoftTranslate.MICROSOFT_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageRegion.REGION_CHINESE_HK, MicrosoftTranslate.MICROSOFT_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageRegion.REGION_JAPAN, MicrosoftTranslate.MICROSOFT_LANGUAGE_JAPANESE);
            put(LanguageRegion.REGION_GERMANY, MicrosoftTranslate.MICROSOFT_LANGUAGE_GERMANY);
            put(LanguageRegion.REGION_FRANCE_FR, MicrosoftTranslate.MICROSOFT_LANGUAGE_FRANCE);
            put(LanguageRegion.REGION_FRANCE_CA, MicrosoftTranslate.MICROSOFT_LANGUAGE_FRANCE);
            put(LanguageRegion.REGION_PORTUGUESE, MicrosoftTranslate.MICROSOFT_LANGUAGE_PORTUGUESE);
            put(LanguageRegion.REGION_ITALIA, MicrosoftTranslate.MICROSOFT_LANGUAGE_ITALIANO);
            put(LanguageRegion.REGION_ESPANOL_ES, MicrosoftTranslate.MICROSOFT_LANGUAGE_ESPANOL);
            put(LanguageRegion.REGION_ESPANOL_MX, MicrosoftTranslate.MICROSOFT_LANGUAGE_ESPANOL);
            put(LanguageRegion.REGION_RUSSIAN, MicrosoftTranslate.MICROSOFT_LANGUAGE_RUSSIAN);
        }
    };

    // OCR語種  谷歌翻譯 匹配對
    public static final HashMap<String, String> mLanguageGoogleTranslatePairs = new HashMap<String, String>() {
        {
            put(LanguageCodes.ChineseSimplified, GoogleTranslate.GOOGLE_LANGUAGE_CHINESE_SIMPLIFIED);
            put(LanguageCodes.ChineseTraditional, GoogleTranslate.GOOGLE_LANGUAGE_CHINESE_TRADITIONAL);
            put(LanguageCodes.Czech, GoogleTranslate.GOOGLE_LANGUAGE_CZECH);
            put(LanguageCodes.Danish, GoogleTranslate.GOOGLE_LANGUAGE_DANISH);
            put(LanguageCodes.Dutch, GoogleTranslate.GOOGLE_LANGUAGE_DUTCH);
            put(LanguageCodes.English, GoogleTranslate.GOOGLE_LANGUAGE_ENGLISH);
            put(LanguageCodes.Finnish, GoogleTranslate.GOOGLE_LANGUAGE_FINNISH);
            put(LanguageCodes.French, GoogleTranslate.GOOGLE_LANGUAGE_FRANCE);
            put(LanguageCodes.German, GoogleTranslate.GOOGLE_LANGUAGE_GERMANY);
            put(LanguageCodes.Greek, GoogleTranslate.GOOGLE_LANGUAGE_GREEK);
            put(LanguageCodes.Hungarian, GoogleTranslate.GOOGLE_LANGUAGE_HUNGARIAN);
            put(LanguageCodes.Italian, GoogleTranslate.GOOGLE_LANGUAGE_ITALIANO);
            put(LanguageCodes.Japanese, GoogleTranslate.GOOGLE_LANGUAGE_JAPANESE);
            put(LanguageCodes.Korean, GoogleTranslate.GOOGLE_LANGUAGE_KOREAN);
            put(LanguageCodes.Norwegian, GoogleTranslate.GOOGLE_LANGUAGE_NORWEGIAN);
            put(LanguageCodes.Polish, GoogleTranslate.GOOGLE_LANGUAGE_POLISH);
            put(LanguageCodes.Portuguese, GoogleTranslate.GOOGLE_LANGUAGE_PORTUGUESE);
            put(LanguageCodes.Russian, GoogleTranslate.GOOGLE_LANGUAGE_RUSSIAN);
            put(LanguageCodes.Spanish, GoogleTranslate.GOOGLE_LANGUAGE_ESPANOL);
            put(LanguageCodes.Swedish, GoogleTranslate.GOOGLE_LANGUAGE_SWEDISH);
            put(LanguageCodes.Turkish, GoogleTranslate.GOOGLE_LANGUAGE_TURKISH);
        }
    };
    // Region 谷歌翻譯 匹配對
    public static final HashMap<String, String> mRegionGoogleTranslatePairs = new HashMap<String, String>() {
        {
            put(GoogleLanguageRegion.REGION_Bengali, GoogleTranslate.GOOGLE_LANGUAGE_BENGALI);
            put(GoogleLanguageRegion.REGION_Chinese_China, GoogleTranslate.GOOGLE_LANGUAGE_CHINESE_SIMPLIFIED);
            put(GoogleLanguageRegion.REGION_Chinese_Taiwan, GoogleTranslate.GOOGLE_LANGUAGE_CHINESE_TRADITIONAL);
            put(GoogleLanguageRegion.REGION_Danish, GoogleTranslate.GOOGLE_LANGUAGE_DANISH);
            put(GoogleLanguageRegion.REGION_Dutch, GoogleTranslate.GOOGLE_LANGUAGE_DUTCH);
            put(GoogleLanguageRegion.REGION_English_Australia, GoogleTranslate.GOOGLE_LANGUAGE_ENGLISH);
            put(GoogleLanguageRegion.REGION_English_India, GoogleTranslate.GOOGLE_LANGUAGE_ENGLISH);
            put(GoogleLanguageRegion.REGION_English_UK, GoogleTranslate.GOOGLE_LANGUAGE_ENGLISH);
            put(GoogleLanguageRegion.REGION_English_US, GoogleTranslate.GOOGLE_LANGUAGE_ENGLISH);
            put(GoogleLanguageRegion.REGION_Finnish, GoogleTranslate.GOOGLE_LANGUAGE_FINNISH);
            put(GoogleLanguageRegion.REGION_French, GoogleTranslate.GOOGLE_LANGUAGE_FRANCE);
            put(GoogleLanguageRegion.REGION_German, GoogleTranslate.GOOGLE_LANGUAGE_GERMANY);
            put(GoogleLanguageRegion.REGION_Hindi, GoogleTranslate.GOOGLE_LANGUAGE_HINDI);
            put(GoogleLanguageRegion.REGION_Hungarian, GoogleTranslate.GOOGLE_LANGUAGE_HUNGARIAN);
            put(GoogleLanguageRegion.REGION_Indonesian, GoogleTranslate.GOOGLE_LANGUAGE_INDONESIAN);
            put(GoogleLanguageRegion.REGION_Italian, GoogleTranslate.GOOGLE_LANGUAGE_ITALIANO);
            put(GoogleLanguageRegion.REGION_Japanese, GoogleTranslate.GOOGLE_LANGUAGE_JAPANESE);
            put(GoogleLanguageRegion.REGION_Korean, GoogleTranslate.GOOGLE_LANGUAGE_KOREAN);
            put(GoogleLanguageRegion.REGION_Norwegian, GoogleTranslate.GOOGLE_LANGUAGE_NORWEGIAN);
            put(GoogleLanguageRegion.REGION_Polish, GoogleTranslate.GOOGLE_LANGUAGE_POLISH);
            put(GoogleLanguageRegion.REGION_Portuguese, GoogleTranslate.GOOGLE_LANGUAGE_PORTUGUESE);
            put(GoogleLanguageRegion.REGION_Russian, GoogleTranslate.GOOGLE_LANGUAGE_RUSSIAN);
            put(GoogleLanguageRegion.REGION_Spanish_Spain, GoogleTranslate.GOOGLE_LANGUAGE_ESPANOL);
            put(GoogleLanguageRegion.REGION_Spanish_US, GoogleTranslate.GOOGLE_LANGUAGE_ESPANOL);
            put(GoogleLanguageRegion.REGION_Swedish, GoogleTranslate.GOOGLE_LANGUAGE_SWEDISH);
            put(GoogleLanguageRegion.REGION_Thai, GoogleTranslate.GOOGLE_LANGUAGE_THAI);
            put(GoogleLanguageRegion.REGION_Turkish, GoogleTranslate.GOOGLE_LANGUAGE_TURKISH);
            put(GoogleLanguageRegion.REGION_Vietnamese, GoogleTranslate.GOOGLE_LANGUAGE_VIETNAMESE);
        }
    };

    public static Voice getTtsVoice(Context context, String audioLanguage) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean translateEnable = sharedPreferences.getBoolean(OcrTtsSettingsFragment.KEY_TRANSLATE_ENABLER, true);
        String languageRegion;
        if (translateEnable) {
            languageRegion = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_VOICE_LANGUAGE, LanguageRegion.REGION_ENGLISH_US);
        } else {
            languageRegion = mLanguageRegionPair.get(audioLanguage);
        }
        String voiceGender = mRegionGenderPairs.get(languageRegion);
        if (voiceGender.equals(VoiceGender.GENDER_BOTH)) {
            voiceGender = sharedPreferences.getString(OcrTtsSettingsFragment.KEY_VOICE_GENDER, VoiceGender.GENDER_FEMALE);
        }

        return getTtsVoice(languageRegion, voiceGender);
    }

    public static Voice getTtsVoice(String languageRegion, String voiceGender) {
        Voice voice = null;
        // voice = new Voice("zh-CN", "Microsoft Server Speech Text to Speech Voice (zh-CN, Yaoyao, Apollo)", Voice.Gender.Female, true);
        String voiceName = null;
        switch (languageRegion) {
            case LanguageRegion.REGION_ENGLISH_US:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "ZiraRUS";
                } else {
                    voiceName = "BenjaminRUS";
                }
                break;
            case LanguageRegion.REGION_ENGLISH_AU:
                voiceName = "Catherine";
                break;
            case LanguageRegion.REGION_ENGLISH_CA:
                voiceName = "Linda";
                break;
            case LanguageRegion.REGION_ENGLISH_IN:
                voiceName = "Ravi, Apollo";
                break;
            case LanguageRegion.REGION_ENGLISH_GB:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Susan, Apollo";
                } else {
                    voiceName = "George, Apollo";
                }
                break;
            case LanguageRegion.REGION_CHINESE_CN:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Yaoyao, Apollo";
                } else {
                    voiceName = "Kangkang, Apollo";
                }
                break;
            case LanguageRegion.REGION_CHINESE_TW:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Yating, Apollo";
                } else {
                    voiceName = "Zhiwei, Apollo";
                }
                break;
            case LanguageRegion.REGION_CHINESE_HK:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Tracy, Apollo";
                } else {
                    voiceName = "Danny, Apollo";
                }
                break;
            case LanguageRegion.REGION_JAPAN:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Ayumi, Apollo";
                } else {
                    voiceName = "Ichiro, Apollo";
                }
                break;
            case LanguageRegion.REGION_GERMANY:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Hedda";
                } else {
                    voiceName = "Stefan, Apollo";
                }
                break;
            case LanguageRegion.REGION_FRANCE_FR:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Julie, Apollo";
                } else {
                    voiceName = "Paul, Apollo";
                }
                break;
            case LanguageRegion.REGION_FRANCE_CA:
                voiceName = "Caroline";
                break;
            case LanguageRegion.REGION_PORTUGUESE:
                voiceName = "Daniel, Apollo";
                break;
            case LanguageRegion.REGION_ITALIA:
                voiceName = "Cosimo, Apollo";
                break;
            case LanguageRegion.REGION_ESPANOL_ES:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Laura, Apollo";
                } else {
                    voiceName = "Pablo, Apollo";
                }
                break;
            case LanguageRegion.REGION_ESPANOL_MX:
                voiceName = "Raul, Apollo";
                break;
            case LanguageRegion.REGION_RUSSIAN:
                if (voiceGender.equals(VoiceGender.GENDER_FEMALE)) {
                    voiceName = "Irina, Apollo";
                } else {
                    voiceName = "Pavel, Apollo";
                }
                break;
        }
        voice = new Voice(languageRegion,
                String.format("Microsoft Server Speech Text to Speech Voice (%1$s, %2$s)", languageRegion, voiceName),
                voiceGender.equals(VoiceGender.GENDER_FEMALE) ? Voice.Gender.Female : Voice.Gender.Male,
                true);
        return voice;
    }

    public static String formatMicrosoftTTSToSSML(Voice voice, String content) {
        String ssml = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"http://www.w3.org/2001/mstts\" xml:lang=\"" + voice.lang + "\"><voice xml:lang=\"" + voice.lang + "\"";
        //if(this.m_eServiceStrategy == Synthesizer.ServiceStrategy.AlwaysService) {
        if (voice.voiceName.length() > 0) {
            ssml = ssml + " name=\"" + voice.voiceName + "\">";
        } else {
            ssml = ssml + ">";
        }

        ssml = ssml + content + "</voice></speak>";
        //}
        return ssml;
    }

    public static String getSpeakContent(String content) {
        String result = content;
        if (result.contains("&")) {
            result = result.replace("&", "&amp;");
        }
        if (result.contains("<")) {
            result = result.replace("<", "&lt;");
        }
        if (result.contains(">")) {
            result = result.replace(">", "&gt;");
        }
        if (result.contains("\'")) {
            result = result.replace("\'", "&apos;");
        }
        if (result.contains("\"")) {
            result = result.replace("\"", "&quot;");
        }
        return result;
    }

    // MAX=800 --> English, German,
    public static final int LENGTH_TO_READ_MAX_800 = 600;
    // MAX=200 --> Chinese, Japan
    private static final int LENGTH_TO_READ_MAX_200 = 150;
    // MAX=250 --> French, Portuguese, Italian, Spanish, Russian
    private static final int LENGTH_TO_READ_MAX_250 = 180;
    public static final HashMap<String, Integer> mLanguageLimitationPair = new HashMap<String, Integer>() {
        {
            put(LanguageCodes.ChineseSimplified, LENGTH_TO_READ_MAX_200);
            put(LanguageCodes.ChineseTraditional, LENGTH_TO_READ_MAX_200);
            put(LanguageCodes.English, LENGTH_TO_READ_MAX_800);
            put(LanguageCodes.French, LENGTH_TO_READ_MAX_250);
            put(LanguageCodes.German, LENGTH_TO_READ_MAX_800);
            put(LanguageCodes.Italian, LENGTH_TO_READ_MAX_250);
            put(LanguageCodes.Japanese, LENGTH_TO_READ_MAX_200);
            put(LanguageCodes.Korean, LENGTH_TO_READ_MAX_200);
            put(LanguageCodes.Portuguese, LENGTH_TO_READ_MAX_250);
            put(LanguageCodes.Russian, LENGTH_TO_READ_MAX_250);
            put(LanguageCodes.Spanish, LENGTH_TO_READ_MAX_250);
        }
    };

    // OCR_TTS
    public static final int OCR_REQUEST_CODE = 10;
    public static final int OCR_RESULT_CODE = 20;
    public static final String EXTRA_KEY_CODE = "keyCode";
    public static final String EXTRA_LAUNCH_MODE = "launch_mode";
    public static final String EXTRA_STORIES = "stories";

    public static final int LAUNCH_MODE_DEFAULT = 0;
    public static final int LAUNCH_MODE_TAKE_PHOTO = 1;
    public static final int LAUNCH_MODE_FROM_GALLERY = 2;

    public static final String ACTION_KEYWORD_DETECTED = "com.wistron.teddybear.KEYWORD_DETECTED";
    public static final String ACTION_SCENARIO_ACTION = "com.wistron.teddybear.SCENARIO_ACTION";
    public static final String EXTRA_SCENARIO_ACTION = "scenario_action";
    public static final int SCENARIO_ACTION_RESUME = 0;
    public static final int SCENARIO_ACTION_PAUSE = 1;
    public static final int SCENARIO_ACTION_PREVIOUS = 2;
    public static final int SCENARIO_ACTION_NEXT = 3;
    public static final int SCENARIO_ACTION_STOP = 4;

    public static boolean isNetworkAvailable(Context context) {
        boolean status = false;
        final ConnectivityManager connManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != connManager) {
            NetworkInfo activeNetworkInfo = connManager.getActiveNetworkInfo();
            if (null != activeNetworkInfo) {
                status = activeNetworkInfo.isConnected();
            }
        }
        Log.i("King", "Detect isNetworkAvailable = " + status);
        return status;
    }


}
