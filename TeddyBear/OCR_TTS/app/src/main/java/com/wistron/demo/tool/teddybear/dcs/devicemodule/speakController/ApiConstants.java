package com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController;

/**
 * 定义了表示SpeakController模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.speaker_controller";
    public static final String NAME = "SpeakerControllerInterface";

    public static final class Events {
        public static final class VolumeChanged {
            public static final String NAME = VolumeChanged.class.getSimpleName();
        }

        public static final class MuteChanged {
            public static final String NAME = MuteChanged.class.getSimpleName();
        }

        public static final class VolumeState {
            public static final String NAME = VolumeState.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class SetVolume {
            public static final String NAME = SetVolume.class.getSimpleName();
        }

        public static final class AdjustVolume {
            public static final String NAME = AdjustVolume.class.getSimpleName();
        }

        public static final class SetMute {
            public static final String NAME = SetMute.class.getSimpleName();
        }
    }
}
