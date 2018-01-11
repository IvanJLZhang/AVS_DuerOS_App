package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer;

/**
 * 定义了表示Audio Player模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.audio_player";
    public static final String NAME = "AudioPlayerInterface";

    public static final class Events {
        public static final class PlaybackStarted {
            public static final String NAME = PlaybackStarted.class.getSimpleName();
        }

        public static final class PlaybackNearlyFinished {
            public static final String NAME = PlaybackNearlyFinished.class.getSimpleName();
        }

        public static final class PlaybackStutterStarted {
            public static final String NAME = PlaybackStutterStarted.class.getSimpleName();
        }

        public static final class PlaybackStutterFinished {
            public static final String NAME = PlaybackStutterFinished.class.getSimpleName();
        }

        public static final class PlaybackFinished {
            public static final String NAME = PlaybackFinished.class.getSimpleName();
        }

        public static final class PlaybackFailed {
            public static final String NAME = PlaybackFailed.class.getSimpleName();
        }

        public static final class PlaybackStopped {
            public static final String NAME = PlaybackStopped.class.getSimpleName();
        }

        public static final class PlaybackPaused {
            public static final String NAME = PlaybackPaused.class.getSimpleName();
        }

        public static final class PlaybackResumed {
            public static final String NAME = PlaybackResumed.class.getSimpleName();
        }

        public static final class PlaybackQueueCleared {
            public static final String NAME = PlaybackQueueCleared.class.getSimpleName();
        }

        public static final class ProgressReportDelayElapsed {
            public static final String NAME = ProgressReportDelayElapsed.class.getSimpleName();
        }

        public static final class ProgressReportIntervalElapsed {
            public static final String NAME = ProgressReportIntervalElapsed.class
                    .getSimpleName();
        }

        public static final class PlaybackState {
            public static final String NAME = PlaybackState.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class Play {
            public static final String NAME = Play.class.getSimpleName();
        }

        public static final class Stop {
            public static final String NAME = Stop.class.getSimpleName();
        }

        public static final class ClearQueue {
            public static final String NAME = ClearQueue.class.getSimpleName();
        }
    }
}
