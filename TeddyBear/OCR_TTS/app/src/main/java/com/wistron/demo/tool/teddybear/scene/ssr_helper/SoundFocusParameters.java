package com.wistron.demo.tool.teddybear.scene.ssr_helper;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * SSR function
 */
public class SoundFocusParameters {
    public static final String ALL;
    public static final String ALL_CONFIG;
    public static final String ALL_DATA;
    public static final String BEAM = "SourceTrack.zoom_beampattern";
    public static final String DELIMITER = ";";
    public static final String DOA_NOISE = "SourceTrack.doa_noise";
    public static final String DOA_SPEECH = "SourceTrack.doa_speech";
    public static final String ENABLE_SECTORS = "SoundFocus.enable_sectors";
    public static final String EQUAL = "=";
    public static final String GAIN_STEP = "SoundFocus.gain_step";
    public static final String HOLDING_POSITION = "SoundFocus.holding_position";
    public static final String MUTE = "SoundFocus.mute";
    public static final String POLAR_ACTIVITY = "SourceTrack.polar_activity";
    public static final String SSR_NOISE_LEVEL = "ssr.noise_level";
    public static final String SSR_NOISE_LEVEL_AFTER_NS = "ssr.noise_level_after_ns";
    public static final String SSR_NS_LEVEL = "ssr.ns_level";
    public static final String START_ANGLE = "SoundFocus.start_angles";
    private static final String TAG = "SourceTracking";
    public static final String VAD = "SourceTrack.vad";
    public static final String WNR_ENABLED = "SourceTrack.enable_wnr";

    static {
        ALL_DATA = build(VAD, DOA_SPEECH, DOA_NOISE, POLAR_ACTIVITY, SSR_NOISE_LEVEL, SSR_NOISE_LEVEL_AFTER_NS);
        ALL_CONFIG = build(START_ANGLE, ENABLE_SECTORS, GAIN_STEP, WNR_ENABLED, BEAM, HOLDING_POSITION, MUTE, SSR_NS_LEVEL);
        ALL = build(START_ANGLE, ENABLE_SECTORS, GAIN_STEP, WNR_ENABLED, BEAM, HOLDING_POSITION, MUTE, VAD, DOA_SPEECH, DOA_NOISE, POLAR_ACTIVITY, SSR_NS_LEVEL, SSR_NOISE_LEVEL, SSR_NOISE_LEVEL_AFTER_NS);
    }

    public static String build(String... args) {
        StringBuilder builder = new StringBuilder();
        for (String arg : args) {
            builder.append(arg);
            builder.append(DELIMITER);
        }
        String str = builder.toString();
        if (str == null || str.length() <= 0 || str.charAt(str.length() - 1) != ';') {
            return str;
        }
        return str.substring(0, str.length() - 1);
    }

    public static String get(String string, String key) {
        try {
            int index = string.indexOf(key);
            if (index != -1) {
                String substring = string.substring(index);
                String[] kvp = substring.split(DELIMITER);
                if (kvp != null && kvp.length > 0) {
                    substring = kvp[0];
                }
                kvp = substring.split(EQUAL);
                if (kvp != null && kvp.length > 1) {
                    return kvp[1];
                }
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "NullPointerException. Invalid arguments. " + e.getMessage());
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> parse(String string) {
        Map<String, String> values = new HashMap();
        try {
            String[] tokens = string.split(DELIMITER);
            if (tokens != null && tokens.length > 0) {
                for (String token : tokens) {
                    String[] kvp = token.split(EQUAL);
                    if (kvp != null && kvp.length > 1) {
                        values.put(kvp[0], kvp[1]);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return values;
    }
}
