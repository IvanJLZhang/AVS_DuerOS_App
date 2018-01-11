package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts;

/**
 * 定义Alerts模块的基础能力接口：开启与停止闹钟
 * Created by ivanjlzhang on 17-9-21.
 */

public interface AlertHandler {
    void startAlert(String alertToken);

    void stopAlert(String alertToken);
}
