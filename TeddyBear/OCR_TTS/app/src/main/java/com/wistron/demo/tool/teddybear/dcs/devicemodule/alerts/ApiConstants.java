package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts;

/**
 * 定义了表示Alerts模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-21.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.alerts";
    public static final String NAME = "AlertsInterface";

    public static final class Events {
        public static final class SetAlertSucceeded {
            public static final String NAME = SetAlertSucceeded.class.getSimpleName();
        }

        public static final class SetAlertFailed {
            public static final String NAME = SetAlertFailed.class.getSimpleName();
        }

        public static final class DeleteAlertSucceeded {
            public static final String NAME = DeleteAlertSucceeded.class.getSimpleName();
        }

        public static final class DeleteAlertFailed {
            public static final String NAME = DeleteAlertFailed.class.getSimpleName();
        }

        public static final class AlertStarted {
            public static final String NAME = AlertStarted.class.getSimpleName();
        }

        public static final class AlertStopped {
            public static final String NAME = AlertStopped.class.getSimpleName();
        }

        public static final class AlertsState {
            public static final String NAME = AlertsState.class.getSimpleName();
        }

        public static final class AlertEnteredForeground {
            public static final String NAME = AlertEnteredForeground.class.getSimpleName();
        }

        public static final class AlertEnteredBackground {
            public static final String NAME = AlertEnteredBackground.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class SetAlert {
            public static final String NAME = SetAlert.class.getSimpleName();
        }

        public static final class DeleteAlert {
            public static final String NAME = DeleteAlert.class.getSimpleName();
        }
    }
}
