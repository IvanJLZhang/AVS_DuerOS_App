package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by king on 16-3-22.
 */
public class LocalDecode {
    public static class LocaleInfo implements Comparable<LocaleInfo> {
        static final Collator sCollator = Collator.getInstance();

        String label;
        Locale locale;
        String localeTag;

        public LocaleInfo(String label, Locale locale, String localeTag) {
            this.label = label;
            this.locale = locale;
            this.localeTag = localeTag;
        }

        public String getLabel() {
            return label;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return this.label + ", locale = " + localeTag;
        }

        @Override
        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }
    }

    public void getLocales(Context context) {
        List<LocaleInfo> results = getAllAssetLocales(context, true);
        for (LocaleInfo locale : results) {
            Log.i("King", "locale = " + locale);
        }
    }


    public static List<LocaleInfo> getAllAssetLocales(Context context, boolean isInDeveloperMode) {
        final Resources resources = context.getResources();

        final String[] locales = Resources.getSystem().getAssets().getLocales();
        List<String> localeList = new ArrayList<String>(locales.length);
        Collections.addAll(localeList, locales);

        // Don't show the pseudolocales unless we're in developer mode. http://b/17190407.
        if (!isInDeveloperMode) {
            localeList.remove("ar-XB");
            localeList.remove("en-XA");
        }
        localeList.add("es-ES");
        localeList.add("es-MX");

        Collections.sort(localeList);
        final String[] specialLocaleCodes = new String[]{"ar_EG", "zh_CN", "zh_TW"};
        final String[] specialLocaleNames = new String[]{"العربية", "中文 (简体)", "中文 (繁體)"};

        final ArrayList<LocaleInfo> localeInfos = new ArrayList<LocaleInfo>(localeList.size());
        for (String locale : localeList) {
            final Locale l = Locale.forLanguageTag(locale.replace('_', '-'));
            if (l == null || "und".equals(l.getLanguage())
                    || l.getLanguage().isEmpty() || l.getCountry().isEmpty()) {
                continue;
            }

            if (localeInfos.isEmpty()) {
                /*if (DEBUG) {
                    Log.v(TAG, "adding initial " + toTitleCase(l.getDisplayLanguage(l)));
                }*/
                localeInfos.add(new LocaleInfo(toTitleCase(l.getDisplayLanguage(l)), l, locale));
            } else {
                // check previous entry:
                //  same lang and a country -> upgrade to full name and
                //    insert ours with full name
                //  diff lang -> insert ours with lang-only name
                final LocaleInfo previous = localeInfos.get(localeInfos.size() - 1);
                if (previous.locale.getLanguage().equals(l.getLanguage()) &&
                        !previous.locale.getLanguage().equals("zz")) {
                    /*if (DEBUG) {
                        Log.v(TAG, "backing up and fixing " + previous.label + " to " +
                                getDisplayName(previous.locale, specialLocaleCodes, specialLocaleNames));
                    }*/
                    previous.label = toTitleCase(getDisplayName(
                            previous.locale, specialLocaleCodes, specialLocaleNames));
                    /*if (DEBUG) {
                        Log.v(TAG, "  and adding "+ toTitleCase(
                                getDisplayName(l, specialLocaleCodes, specialLocaleNames)));
                    }*/
                    localeInfos.add(new LocaleInfo(toTitleCase(
                            getDisplayName(l, specialLocaleCodes, specialLocaleNames)), l, locale));
                } else {
                    String displayName = toTitleCase(l.getDisplayLanguage(l));
                    /*if (DEBUG) {
                        Log.v(TAG, "adding "+displayName);
                    }*/
                    localeInfos.add(new LocaleInfo(displayName, l, locale));
                }
            }
        }

        Collections.sort(localeInfos);
        return localeInfos;
    }

    private static String toTitleCase(String s) {
        if (s.length() == 0) {
            return s;
        }

        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String getDisplayName(
            Locale l, String[] specialLocaleCodes, String[] specialLocaleNames) {
        String code = l.toString();

        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }

        return l.getDisplayName(l);
    }
}
