package com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * UserInactivityReport事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class UserInactivityReportPayload extends Payload {
    private long inactiveTimeInSeconds;

    public UserInactivityReportPayload(long inactiveTimeInSeconds) {
        this.inactiveTimeInSeconds = inactiveTimeInSeconds;
    }

    void setInactiveTimeInSeconds(long inactiveTimeInSeconds) {
        this.inactiveTimeInSeconds = inactiveTimeInSeconds;
    }

    long getInactiveTimeInSeconds() {
        return inactiveTimeInSeconds;
    }
}
