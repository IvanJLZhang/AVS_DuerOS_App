package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * ClearQueue指令对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class ClearQueuePayload extends Payload {
    public enum ClearBehavior {
        CLEAR_ENQUEUED,
        CLEAR_ALL
    }

    public ClearBehavior clearBehavior;
}
