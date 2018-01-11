package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput;

/**
 * 定义了表示Voice Output模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.voice_output";
    public static final String NAME = "VoiceOutputInterface";

    public static final class Events {
        public static final class SpeechStarted {
            public static final String NAME = SpeechStarted.class.getSimpleName();
        }

        public static final class SpeechFinished {
            public static final String NAME = SpeechFinished.class.getSimpleName();
        }

        public static final class SpeechState {
            public static final String NAME = SpeechState.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class Speak {
            public static final String NAME = Speak.class.getSimpleName();
        }
    }
}
