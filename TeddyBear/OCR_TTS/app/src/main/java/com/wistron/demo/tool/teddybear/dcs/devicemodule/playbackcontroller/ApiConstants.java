package com.wistron.demo.tool.teddybear.dcs.devicemodule.playbackcontroller;

/**
 * 定义了表示PlaybackController模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.playback_controller";
    public static final String NAME = "PlaybackControllerInterface";

    public static final class Events {
        public static final class NextCommandIssued {
            public static final String NAME = NextCommandIssued.class.getSimpleName();
        }

        public static final class PreviousCommandIssued {
            public static final String NAME = PreviousCommandIssued.class.getSimpleName();
        }

        public static final class PlayCommandIssued {
            public static final String NAME = PlayCommandIssued.class.getSimpleName();
        }

        public static final class PauseCommandIssued {
            public static final String NAME = PauseCommandIssued.class.getSimpleName();
        }
    }
}
