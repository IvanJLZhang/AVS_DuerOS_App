package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import java.io.Serializable;

/**
 * Created by ivanjlzhang on 17-9-21.
 */

public abstract class SetAlertPayload extends Payload implements Serializable {
    public enum AlertType{
        ALARM,
        TIMER
    }

    private String token;
    private AlertType type;
    //format 2017-09-21T15:42:00:00+0000
    private String scheduledTime;
    private String content;

    @Override
    public String toString() {
        return "SetAlertPayload{"
                + "token='"
                + token
                + '\''
                + ", type="
                + type
                + ", scheduledTime='"
                + scheduledTime
                + '\''
                + '}';
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AlertType getType() {
        return type;
    }

    public void setType(AlertType type) {
        this.type = type;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }




}
