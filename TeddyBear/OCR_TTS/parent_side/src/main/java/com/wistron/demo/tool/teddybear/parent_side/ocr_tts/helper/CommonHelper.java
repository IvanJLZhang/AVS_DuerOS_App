package com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper;

import android.content.Context;

import com.microsoft.speech.tts.Voice;
import com.wistron.demo.tool.teddybear.parent_side.Child;
import com.wistron.demo.tool.teddybear.parent_side.ChildrenManagementActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by king on 16-3-22.
 */
public class CommonHelper {

    public static class LanguageRegion {
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

    public static ArrayList<Child> getChildrenList(Context context) {
        ArrayList<Child> children = new ArrayList<>();
        File mFile = new File(context.getFilesDir(), ChildrenManagementActivity.CHILDREN_LIST_FILE_NAME);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(mFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Object key : properties.keySet()) {
            children.add(new Child((String) properties.get((String) key), (String) key));
        }

        Collections.sort(children, comparator);

        return children;
    }

    private static Comparator comparator = new Comparator() {
        @Override
        public int compare(Object lhs, Object rhs) {
            return ((Child) lhs).getName().compareTo(((Child) rhs).getName());
        }
    };

    /**
     * 检测邮箱地址是否合法
     *
     * @param email
     * @return true合法 false不合法
     */
    public static boolean isValidEmail(String email) {
        if (null == email || "".equals(email)) return false;
//        Pattern p = Pattern.compile("\\w+@(\\w+.)+[a-z]{2,3}"); //简单匹配
        Pattern p = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");//复杂匹配
        Matcher m = p.matcher(email);
        return m.matches();
    }

    public static final int STORAGE_AZURE = 0;
    public static final int STORAGE_QINIU = 1;
    public static final int DEFAULT_STORAGE = STORAGE_QINIU;

    public static final String REMOTE_FOLDER_COMMON = "common";
    public static final String REMOTE_SVA_BT_FILE = "svaBtMap.txt";

    public static final String RESET_BT_NAME = "Teddy";
}
