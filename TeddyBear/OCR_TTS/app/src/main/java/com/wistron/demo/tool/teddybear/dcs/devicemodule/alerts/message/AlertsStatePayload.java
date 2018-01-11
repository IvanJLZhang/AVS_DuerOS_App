package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import java.util.List;

/**
 * Alerts模块端状态对应的payload结构
 * Created by ivanjlzhang on 17-9-21.
 */

public class AlertsStatePayload extends Payload {
    public List<Alert> allAlerts;
    public List<Alert> activeAlerts;

    public AlertsStatePayload(List<Alert> all, List<Alert> active) {
        this.allAlerts = all;
        this.activeAlerts = active;
    }
}
