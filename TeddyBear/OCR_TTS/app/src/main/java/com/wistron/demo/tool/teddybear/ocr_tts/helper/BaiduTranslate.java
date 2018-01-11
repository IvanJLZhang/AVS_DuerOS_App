package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Created by king on 16-3-9.
 */
public class BaiduTranslate extends TranslateBase {
    public static final String BAIDU_LANGUAGE_AUTO = "auto";
    // speech support language
    public static final String BAIDU_LANGUAGE_ENGLISH = "en";  //英語
    public static final String BAIDU_LANGUAGE_CHINESE_SIMPLIFIED = "zh";  //簡體中文
    public static final String BAIDU_LANGUAGE_CHINESE_TRADITIONAL = "cht"; //繁體中文
    public static final String BAIDU_LANGUAGE_JAPANESE = "jp";  // 日語
    public static final String BAIDU_LANGUAGE_GERMANY = "de";  //德語
    public static final String BAIDU_LANGUAGE_FRANCE = "fra";  // 法語
    public static final String BAIDU_LANGUAGE_PORTUGUESE = "pt";  // 葡萄牙語
    public static final String BAIDU_LANGUAGE_ITALIANO = "it";  // 意大利語
    public static final String BAIDU_LANGUAGE_ESPANOL = "spa";  // 西班牙語
    public static final String BAIDU_LANGUAGE_RUSSIAN = "ru";  // 俄語
    // OTHE LANGUAGE
    public static final String BAIDU_LANGUAGE_CZECH = "cs";  // 捷克語
    public static final String BAIDU_LANGUAGE_DANISH = "dan";  // 丹麥語
    public static final String BAIDU_LANGUAGE_DUTCH = "nl";  // 荷蘭語
    public static final String BAIDU_LANGUAGE_FINNISH = "fin"; // 芬蘭語
    public static final String BAIDU_LANGUAGE_GREEK = "el"; // 希臘語
    public static final String BAIDU_LANGUAGE_HUNGARIAN = "hu"; // 匈牙利語
    public static final String BAIDU_LANGUAGE_KOREAN = "kor";  // 韓語
    public static final String BAIDU_LANGUAGE_POLISH = "pl"; // 波蘭語
    public static final String BAIDU_LANGUAGE_SWEDISH = "swe"; //瑞典語

    // Baidu words limitation
    private static final int WORDS_LIMITATION = 4500;

    public BaiduTranslate(Context context, String fromLanguage, String toLanguage) {
        super(context, fromLanguage, toLanguage);
    }

    private static String getMD5(String info) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes("UTF-8"));
            byte[] encryption = md5.digest();

            StringBuffer strBuf = new StringBuffer();
            for (int i = 0; i < encryption.length; i++) {
                if (Integer.toHexString(0xff & encryption[i]).length() == 1) {
                    strBuf.append("0").append(Integer.toHexString(0xff & encryption[i]));
                } else {
                    strBuf.append(Integer.toHexString(0xff & encryption[i]));
                }
            }

            return strBuf.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        }
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
        String result = null;

        String UTF8 = "utf-8";
        //申请者开发者id，实际使用时请修改成开发者自己的appid
        String appId = "20160309000015075";
        //申请成功后的证书token，实际使用时请修改成开发者自己的token
        String token = "qyb17fpw4p5_TPrtdUfF";
        //随机数，用于生成md5值
        Random random = new Random(System.currentTimeMillis());
        int salt = random.nextInt(10000);
        StringBuilder md5String = new StringBuilder();
        md5String.append(appId).append(q).append(salt).append(token);
        String md5 = getMD5(md5String.toString());
        Log.i("King", "salt = " + salt + "， md5 = " + md5);

        HttpURLConnection mHttpURLConnection = null;
        try {
            URL url = new URL("http://api.fanyi.baidu.com/api/trans/vip/translate");
            mHttpURLConnection = (HttpURLConnection) url.openConnection();
            mHttpURLConnection.setRequestMethod("POST");
            mHttpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            mHttpURLConnection.setDoOutput(true);
            mHttpURLConnection.setDoInput(true);
            mHttpURLConnection.setUseCaches(false);

            DataOutputStream outputStream = new DataOutputStream(mHttpURLConnection.getOutputStream());
            String parameters = "q=" + URLEncoder.encode(q, "UTF-8") +
                    "&from=" + URLEncoder.encode(fromLanguage, "UTF-8") +
                    "&to=" + URLEncoder.encode(toLanguage, "UTF-8") +
                    "&appid=" + URLEncoder.encode(appId, "UTF-8") +
                    "&salt=" + URLEncoder.encode(Integer.toString(salt), "UTF-8") +
                    "&sign=" + URLEncoder.encode(md5, "UTF-8");
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
            JSONObject resultJson = new JSONObject(response.toString());

            //开发者自行处理错误，本示例失败返回为null
            try {
                String error_code = resultJson.getString("error_code");
                if (error_code != null) {
                    System.out.println("出错代码:" + error_code);
                    System.out.println("出错信息:" + resultJson.getString("error_msg"));
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //获取返回翻译结果
            StringBuilder builder = new StringBuilder();
            JSONArray array = (JSONArray) resultJson.get("trans_result");
            for (int i = 0; i < array.length(); i++) {
                if (i != 0) {
                    builder.append("\n");
                }
                JSONObject dst = (JSONObject) array.get(i);
                String text = dst.getString("dst");
                builder.append(text);
                //builder.append(URLDecoder.decode(text, UTF8));
            }
            result = builder.toString();

            Log.i("King", "Baidu translate result = " + result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mHttpURLConnection != null) {
                mHttpURLConnection.disconnect();
            }
        }

        return result;
    }
}
