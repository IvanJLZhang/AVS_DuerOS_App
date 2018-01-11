package com.wistron.demo.tool.teddybear.dcs.devicemodule.screen;

/**
 * 定义了表示Screen模块的namespace、name，以及其事件、指令的name
 * Created by ivanjlzhang on 17-9-22.
 */

public class ApiConstants {
    public static final String NAMESPACE = "ai.dueros.device_interface.screen";
    public static final String NAME = "ScreenInterface";

    public static final class Events {
        public static final class LinkClicked {
            public static final String NAME = LinkClicked.class.getSimpleName();
        }
    }

    public static final class Directives {
        public static final class HtmlView {
            public static final String NAME = HtmlView.class.getSimpleName();
        }
    }
}

