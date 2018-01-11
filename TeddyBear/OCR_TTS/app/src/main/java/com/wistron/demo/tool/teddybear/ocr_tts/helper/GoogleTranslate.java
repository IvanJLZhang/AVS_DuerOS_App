package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.NetworkAccessHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

/**
 * Created by king on 16-3-10.
 */
public class GoogleTranslate extends TranslateBase {
    /*private static final String URL_TEMPLATE = "http://translate.google.cn/translate_a/single?client=t&sl=%1$s&tl=%2$s&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&ie=UTF-8&oe=UTF-8&source=btn&srcrom=0&ssel=3&tsel=0&kc=0&tk=507718.103120&q=%3$s";
    private static final String ENCODING = "UTF-8";*/

    private static final String TAG = "GoogleTranslate";
    public static final String GOOGLE_LANGUAGE_AUTO = "auto";
    // speech support language
    public static final String GOOGLE_LANGUAGE_BENGALI = "bn";  // 孟加拉语
    public static final String GOOGLE_LANGUAGE_ENGLISH = "en";  //英語
    public static final String GOOGLE_LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN";  //簡體中文
    public static final String GOOGLE_LANGUAGE_CHINESE_TRADITIONAL = "zh-TW"; //繁體中文
    public static final String GOOGLE_LANGUAGE_DANISH = "da";  // 丹麥語
    public static final String GOOGLE_LANGUAGE_DUTCH = "nl";  // 荷蘭語
    public static final String GOOGLE_LANGUAGE_FINNISH = "fi"; // 芬蘭語
    public static final String GOOGLE_LANGUAGE_JAPANESE = "ja";  // 日語
    public static final String GOOGLE_LANGUAGE_GERMANY = "de";  //德語
    public static final String GOOGLE_LANGUAGE_FRANCE = "fr";  // 法語
    public static final String GOOGLE_LANGUAGE_HINDI = "hi";  // 印地语
    public static final String GOOGLE_LANGUAGE_INDONESIAN = "id"; // 印尼语
    public static final String GOOGLE_LANGUAGE_NORWEGIAN = "no"; // 挪威语
    public static final String GOOGLE_LANGUAGE_PORTUGUESE = "pt";  // 葡萄牙語
    public static final String GOOGLE_LANGUAGE_ITALIANO = "it";  // 意大利語
    public static final String GOOGLE_LANGUAGE_ESPANOL = "es";  // 西班牙語
    public static final String GOOGLE_LANGUAGE_RUSSIAN = "ru";  // 俄語
    public static final String GOOGLE_LANGUAGE_THAI = "th";  // 泰语
    public static final String GOOGLE_LANGUAGE_TURKISH = "tr"; // 土耳其语
    public static final String GOOGLE_LANGUAGE_VIETNAMESE = "vi";  // 越南语
    // OTHE LANGUAGE
    public static final String GOOGLE_LANGUAGE_CZECH = "cs";  // 捷克語
    public static final String GOOGLE_LANGUAGE_GREEK = "el"; // 希臘語
    public static final String GOOGLE_LANGUAGE_HUNGARIAN = "hu"; // 匈牙利語
    public static final String GOOGLE_LANGUAGE_KOREAN = "ko";  // 韓語
    public static final String GOOGLE_LANGUAGE_POLISH = "pl"; // 波蘭語
    public static final String GOOGLE_LANGUAGE_SWEDISH = "sv"; //瑞典語

    // Google words limitation
    private static final int WORDS_LIMITATION_CHINESE = 200;
    private static final int WORDS_LIMITATION_ENGLISH = 1000;
    private static int WORDS_LIMITATION = WORDS_LIMITATION_ENGLISH;  // Max 2K characters

    private static final String GOOGLE_TRANSLATION_URL = "https://translation.googleapis.com/language/translate/v2?key=%1$s&target=%2$s&q=%3$s";

    public GoogleTranslate(Context context, String fromLanguage, String toLanguage) {
        super(context, fromLanguage, toLanguage);
        if (fromLanguage.equals(GOOGLE_LANGUAGE_CHINESE_SIMPLIFIED) ||
                fromLanguage.equals(GOOGLE_LANGUAGE_CHINESE_TRADITIONAL) ||
                fromLanguage.equals(GOOGLE_LANGUAGE_KOREAN) ||
                fromLanguage.equals(GOOGLE_LANGUAGE_JAPANESE)) {
            WORDS_LIMITATION = WORDS_LIMITATION_CHINESE;
        }
    }

    {
        // GET
    /*@Override
    public String translate(String from, String to, String q) {
        String result = null;
        from = "en";
        to = "zh-CN";
        q = "This is an apple";
        try {
            DefaultHttpClient client = new DefaultHttpClient();
            String url = String.format(URL_TEMPLATE, from, to, URLEncoder.encode(q, "UTF-8"));

            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            String allInfo = EntityUtils.toString(response.getEntity());
            String resultArray[] = allInfo.split("]]")[0].split("]");
            StringBuilder resultBuffer = new StringBuilder();
            for (int i = 0; i < resultArray.length - 1; i++) {
                resultBuffer.append(resultArray[i].split("\"")[1]);
            }
            result = resultBuffer.toString();
            result = result.replace("\\n", "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }*/

        // POST
    /*@Override
    public String translate(String q) {
        String UTF8 = "utf-8";
        String parametersTemplate = "client=t&sl=%1$s&tl=%2$s&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&ie=UTF-8&oe=UTF-8&source=btn&srcrom=0&ssel=3&tsel=0&kc=0&tk=507718.103120&q=%3$s";
        fromLanguage = "en";
        toLanguage = "zh-CN";
        q = "This is an apple and orange";

        String result = "";
        HttpURLConnection mHttpURLConnection = null;
        try {
            //URL url = new URL("http://translate.google.com/translate_t#");
            URL url = new URL("http://translate.google.cn/translate_a/single");
            mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setRequestMethod("POST");
            mHttpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            mHttpURLConnection.setRequestProperty("Proxy-Connection", "keep-alive");
            mHttpURLConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36");
            //mHttpURLConnection.setRequestProperty("Data","q="+URLEncoder.encode(q, "UTF-8"));
            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.setUseCaches(false);

            DataOutputStream outputStream = new DataOutputStream(mHttpURLConnection.getOutputStream());
            String parameters = String.format(parametersTemplate, fromLanguage, toLanguage, URLEncoder.encode(q, "UTF-8"));
            outputStream.writeBytes(parameters);
            outputStream.flush();
            outputStream.close();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(mHttpURLConnection.getInputStream(), UTF8));
            StringBuilder response = new StringBuilder();
            String str = null;
            while ((str = reader.readLine()) != null) {
                response.append(str).append("\n");
            }
            reader.close();

            //转化为json对象，注：Json解析的jar包可选其它
            Log.i("King", "google response = " + response.toString());
            String resultArray[] = response.toString().split("]]")[0].split("]");
            StringBuilder resultBuffer = new StringBuilder();
            for (int i = 0; i < resultArray.length - 1; i++) {
                resultBuffer.append(resultArray[i].split("\"")[1]);
            }
            result = resultBuffer.toString();
            result = result.replace("\\n", "\r\n");

            Log.i("King", "translate result = " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mHttpURLConnection != null) {
                mHttpURLConnection.disconnect();
            }
        }

        return result;
    }*/
    }

    // API error
    {
    /*@Override
    public String translate(String q) {
        *//*RetryParams retryParams = RetryParams.newBuilder()
                .setRetryMaxAttempts(3)
                .setMaxRetryDelayMillis(30000)
                .setTotalRetryPeriodMillis(120000)
                .setInitialRetryDelayMillis(250)
                .build();
        TranslateOptions translateOption = TranslateOptions.newBuilder()
                .setRetryParams(retryParams)
                .setConnectTimeout(60000)
                .setReadTimeout(60000)
                .setApiKey("AIzaSyAj0VvnkpHcK5cYV_9LhaSGXXdX8fd5CaQ")
                .build();
        Translate translate = translateOption.getService();
        TranslateOption srcLang = TranslateOption.sourceLanguage(fromLanguage);
        TranslateOption tgtLang = TranslateOption.targetLanguage(toLanguage);

        Translation translation = translate.translate(q, srcLang, tgtLang);
        Log.i(TAG, String.format("Source Text:\n\tLang: %1$s, Text: %2$s\n", fromLanguage, q));
        Log.i(TAG, String.format("TranslatedText:\n\tLang: %1$s, Text: %2$s\n", toLanguage,
                translation.getTranslatedText()));*//*

        return null;
    }*/
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
        String translatedText = null;

        try {
            String apiKey = context.getString(R.string.google_cloud_translation_api_key);
            q = URLEncoder.encode(q, "UTF-8");
            Log.i(TAG, "translate query = " + q);
            String requestUrl = String.format(GOOGLE_TRANSLATION_URL, apiKey, toLanguage, q);
            Log.i(TAG, "requestUrl = " + requestUrl);
            translatedText = NetworkAccessHelper.invokeNetworkGet(requestUrl);

            if (!TextUtils.isEmpty(translatedText)) {
                JSONObject resultJson = new JSONObject(translatedText);
                if (resultJson.has("data")) {
                    JSONObject dataJson = resultJson.getJSONObject("data");
                    if (dataJson.has("translations")) {
                        JSONArray translationsJsonArray = dataJson.getJSONArray("translations");
                        translatedText = translationsJsonArray.getJSONObject(0).getString("translatedText");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Google translatedText = " + translatedText);

        return translatedText;
    }
}
