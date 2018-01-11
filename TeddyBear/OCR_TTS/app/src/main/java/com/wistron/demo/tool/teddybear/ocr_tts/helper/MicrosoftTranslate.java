package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.helper.NetworkAccessHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SubscriptionKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by king on 16-3-28.
 */
public class MicrosoftTranslate extends TranslateBase {
    private final String TAG = "MicrosoftTranslate";

    public static final String MICROSOFT_LANGUAGE_AUTO = "";
    // speech support language
    public static final String MICROSOFT_LANGUAGE_ENGLISH = "en";  //英語
    public static final String MICROSOFT_LANGUAGE_CHINESE_SIMPLIFIED = "zh-CHS";  //簡體中文
    public static final String MICROSOFT_LANGUAGE_CHINESE_TRADITIONAL = "zh-CHT"; //繁體中文
    public static final String MICROSOFT_LANGUAGE_JAPANESE = "ja";  // 日語
    public static final String MICROSOFT_LANGUAGE_GERMANY = "de";  //德語
    public static final String MICROSOFT_LANGUAGE_FRANCE = "fr";  // 法語
    public static final String MICROSOFT_LANGUAGE_PORTUGUESE = "pt";  // 葡萄牙語
    public static final String MICROSOFT_LANGUAGE_ITALIANO = "it";  // 意大利語
    public static final String MICROSOFT_LANGUAGE_ESPANOL = "es";  // 西班牙語
    public static final String MICROSOFT_LANGUAGE_RUSSIAN = "ru";  // 俄語
    // OTHE LANGUAGE
    public static final String MICROSOFT_LANGUAGE_CZECH = "cs";  // 捷克語
    public static final String MICROSOFT_LANGUAGE_DANISH = "da";  // 丹麥語
    public static final String MICROSOFT_LANGUAGE_DUTCH = "nl";  // 荷蘭語
    public static final String MICROSOFT_LANGUAGE_FINNISH = "fi"; // 芬蘭語
    public static final String MICROSOFT_LANGUAGE_GREEK = "el"; // 希臘語
    public static final String MICROSOFT_LANGUAGE_HUNGARIAN = "hu"; // 匈牙利語
    public static final String MICROSOFT_LANGUAGE_KOREAN = "ko";  // 韓語
    public static final String MICROSOFT_LANGUAGE_POLISH = "pl"; // 波蘭語
    public static final String MICROSOFT_LANGUAGE_SWEDISH = "sv"; //瑞典語
    public static final String MICROSOFT_LANGUAGE_NORWEGIAN = "no"; //挪威語
    public static final String MICROSOFT_LANGUAGE_TURKISH = "tr"; //土耳其語

    // Microsoft words limitation
    private static final int WORDS_LIMITATION = 4500;

    private String getAccessTokenUrl = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken";
    private String translateUrl = "https://api.microsofttranslator.com/v2/http.svc/Translate?appid=%1$s&text=%2$s&to=%3$s";
    private String issueToken = null;

    public MicrosoftTranslate(Context context, String fromLanguage, String toLanguage) {
        super(context, fromLanguage, toLanguage);
    }

    @Override
    public String translate(String q) {
        int totalLength = q.length();
        StringBuilder mBuilder = new StringBuilder();
        int startPosition = 0;
        while (startPosition + WORDS_LIMITATION < totalLength) {
            int offset;
            for (offset = startPosition + WORDS_LIMITATION; offset < totalLength; offset++) {
                String temp = q.substring(offset, offset + 1);
                if (CommonHelper.PUNCTUATION_CONTAINS.contains(temp)) {
                    offset++;
                    break;
                }
            }
            if (offset >= totalLength) {
                String partialResult = startTranslate(q.substring(startPosition));
                if (partialResult == null) {
                    return null;
                }
                mBuilder.append(partialResult);
                startPosition = totalLength;
            } else {
                String partialResult = startTranslate(q.substring(startPosition, offset));
                if (partialResult == null) {
                    return null;
                }
                mBuilder.append(partialResult);
                startPosition = offset;
            }
        }

        if (startPosition < totalLength) {
            String partialResult = startTranslate(q.substring(startPosition));
            if (partialResult == null) {
                return null;
            }
            mBuilder.append(partialResult);
        }

        return mBuilder.toString();
    }

    private String startTranslate(String q) {
        // Old method to translate
        /*Translate.setClientId("wistron_engineer_king"); // Enter your Windows Azure Client Id here
        Translate.setClientSecret("PDHH2HX/6hskHPw8Ama5LopHPAQARhv9uzTAtAYMDd0="); // Enter your Windows Azure Client Secret here

        Log.i("King", "Microsoft translate from: " + Language.fromString(fromLanguage)+", to: "+Language.fromString(toLanguage));
        String translatedText = null;
        try {
            translatedText = Translate.execute(q, Language.fromString(fromLanguage), Language.fromString(toLanguage));
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("King", "Microsoft translate result = "+translatedText);
        return translatedText;*/

        // New method to translate
        String translatedText = null;
        if (issueToken == null) {
            String translatorKey = context.getString(SubscriptionKey.getAzureTextTranslateKey());
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Accept", "application/jwt");
            headers.put("Ocp-Apim-Subscription-Key", translatorKey);
            issueToken = NetworkAccessHelper.invokeNetworkPost(getAccessTokenUrl, headers, null);
            Log.i(TAG, "issueToken result = " + issueToken);
        }

        String translateRequest = String.format(translateUrl,
                Uri.encode("Bearer " + issueToken),
                Uri.encode(q),
                toLanguage);
        Log.i(TAG, "get url = " + translateRequest);
        translatedText = NetworkAccessHelper.invokeNetworkGet(translateRequest);
        if (!TextUtils.isEmpty(translatedText)) {
            translatedText = translatedText.substring(translatedText.indexOf(">") + 1);
            translatedText = translatedText.substring(0, translatedText.lastIndexOf("<"));
        }
        Log.i(TAG, "get result = " + translatedText);

        return translatedText;
    }
}
