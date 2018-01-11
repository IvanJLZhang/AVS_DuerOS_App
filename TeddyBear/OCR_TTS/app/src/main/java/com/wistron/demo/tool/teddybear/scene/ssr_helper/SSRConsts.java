package com.wistron.demo.tool.teddybear.scene.ssr_helper;

public class SSRConsts {
    public static final String ACTION_AUDIO_STATE_CHANGED = "qualcomm.intent.action.ACTION_AUDIO_STATE_CHANGED";
    public static final String AUDIO_SAMPLE_RATE = "AUDIO_SAMPLE_RATE";
    public static final String AUDIO_STATE_CHANGED_DATA = "audio_state_changed_data";
    public static final String CAMERA_ENABLED = "CAMERA_ENABLED";
    public static final short CMD_ACC_SENSOR_DATA = (short) 4;
    public static final short CMD_NS_ENABLE = (short) 3;
    public static final short CMD_SECTOR_DEF = (short) 2;
    public static final String COMMA = ",";
    public static int CONFIG_MAX_SECTOR_ANGLE = 0;
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 48000;
    public static final int DEFAULT_AUDIO_SOURCE = 1;
    public static final int DEFAULT_BEAM = 127;
    public static final boolean DEFAULT_DEMO_MODE = false;
    public static final int[] DEFAULT_ENABLED_SECTORS;
    public static int DEFAULT_MIC_CONFIG = 0;
    public static final Mode DEFAULT_MODE;
    public static final boolean DEFAULT_MUTE = false;
    public static final int DEFAULT_NS_LEVEL = 0;
    public static final int DEFAULT_NUMBER_OF_CHANNELS = 2;
    public static final long DEFAULT_POLL_CONFIG_PERIOD_MS = 2000;
    public static final long DEFAULT_POLL_PERIOD_MSEC = 50;
    public static final int DEFAULT_SMOOTHING_FACTOR = 0;
    public static final int[] DEFAULT_START_ANGLES;
    public static final boolean DEFAULT_WNR_ENABLED = true;
    public static final String DEMO_MODE = "DEMO_MODE";
    public static final String EQUAL = "=";
    public static final int GAIN_INCREMENT = 3;
    public static final String GRAPH_ENABLED = "GRAPH_ENABLED";
    public static final String HEADSET = "HEADSET";
    public static final String LAST_MODE = "LAST_MODE";
    public static final int MAX_GAIN_BOUND = 24;
    public static final int MAX_SECTORS = 4;
    public static final int MAX_SECTOR_ANGLE = 360;
    public static final String MIC_CONFIG = "MIC_CONFIG";
    public static final String MIC_GAIN_ENABLED = "MIC_GAIN_ENABLED";
    public static final int MIN_GAIN_BOUND = 0;
    public static final String NUMBER_OF_CHANNELS_INPUT = "NUMBER_OF_CHANNELS_INPUT";
    public static final String NUMBER_OF_CHANNELS_OUTPUT = "NUMBER_OF_CHANNELS_OUTPUT";
    public static final int OFFSET_ANGLE_DATA = 8;
    public static final int OFFSET_INTERFERER_ANGLE = 2;
    public static final int OFFSET_TARGET_ANGLE = 0;
    public static final int OFFSET_TARGET_SECTOR_ENABLED = 8;
    public static final int OFFSET_VAD = 4;
    public static final String POLL_CONFIG_PERIOD_MS = "POLL_CONFIG_PERIOD_MS";
    public static final String POLL_PERIOD_MSEC = "POLL_PERIOD_MSEC";
    public static final String SECTOR_ANGLE = "ANGLE";
    public static final String SECTOR_ENABLED = "SECTOR_ENABLED";
    public static final String SFAST_PREFS = "SFAST_PREFS";
    public static final String SMOOTHING_FACTOR = "SMOOTHING_FACTOR";
    public static final String SOURCE_TYPE = "SOURCE_TYPE";
    public static final String SPEAKER_PHONE_ON = "SPEAKER_PHONE_ON";
    public static final int THREE_MIC = 3;
    public static final int VIEW_GRAPH = 0;
    public static final int VIEW_LIST = 1;
    public static final int VIEW_PLAYBACK = 2;
    public static final String WNR_ENABLED = "WNR_ENABLED";

    public enum Mode {
        VIDEO_RECORD_MODE,
        LOOPBACK_MODE,
        AUDIO_RECORD_MODE,
        MAX_MODE
    }

    static {
        CONFIG_MAX_SECTOR_ANGLE = MAX_SECTOR_ANGLE;
        DEFAULT_MIC_CONFIG = THREE_MIC;
        DEFAULT_START_ANGLES = new int[]{45, 135, 225, 315};
        DEFAULT_ENABLED_SECTORS = new int[]{VIEW_LIST, VIEW_LIST, VIEW_LIST, VIEW_LIST};
        DEFAULT_MODE = Mode.VIDEO_RECORD_MODE;
    }
}
