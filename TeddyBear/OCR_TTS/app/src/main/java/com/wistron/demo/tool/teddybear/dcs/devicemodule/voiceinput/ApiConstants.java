package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.voice_input";
    public static final String NAME = "VoiceInputInterface";

    public static final class Events {
        public static final class ListenStarted {
            public static final String NAME = ListenStarted.class.getSimpleName();
        }

        public static final class ListenTimedOut {
            public static final String NAME = ListenTimedOut.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class Listen {
            public static final String NAME = Listen.class.getSimpleName();
        }

        public static final class StopListen {
            public static final String NAME = StopListen.class.getSimpleName();
        }
    }
}
