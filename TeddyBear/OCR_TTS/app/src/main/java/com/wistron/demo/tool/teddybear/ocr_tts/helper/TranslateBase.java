package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;

/**
 * Created by king on 16-3-10.
 */
public abstract class TranslateBase {
    public static final String TRANSLATOR_BAIDU = String.valueOf(0);
    public static final String TRANSLATOR_GOOGLE = String.valueOf(1);
    public static final String TRANSLATOR_MICROSOFT = String.valueOf(2);

    protected Context context;
    protected String fromLanguage;
    protected String toLanguage;

    public TranslateBase(Context context, String fromLanguage, String toLanguage) {
        this.fromLanguage = fromLanguage;
        this.toLanguage = toLanguage;
        this.context = context;
    }

    public abstract String translate(String q);
}
