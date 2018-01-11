package com.wistron.demo.tool.teddybear.dcs.devicemodule.system;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.system";
    public static final String NAME = "SystemInterface";

    public static final class Events {
        public static final class SynchronizeState {
            public static final String NAME = SynchronizeState.class.getSimpleName();
        }

        public static final class ExceptionEncountered {
            public static final String NAME = ExceptionEncountered.class.getSimpleName();
        }

        public static final class UserInactivityReport {
            public static final String NAME = UserInactivityReport.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class ResetUserInactivity {
            public static final String NAME = ResetUserInactivity.class.getSimpleName();
        }

        public static final class SetEndpoint {
            public static final String NAME = SetEndpoint.class.getSimpleName();
        }

        public static final class ThrowException {
            public static final String NAME = ThrowException.class.getSimpleName();
        }
    }

    public static final class Exception {
        public static final String NAME = Exception.class.getSimpleName();
    }
}
