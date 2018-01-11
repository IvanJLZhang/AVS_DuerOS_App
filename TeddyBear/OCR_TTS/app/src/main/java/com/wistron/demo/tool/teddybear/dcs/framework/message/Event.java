package com.wistron.demo.tool.teddybear.dcs.framework.message;

/**
 * 事件：设备端上发生任何事情，需要通过事件来通知服务端
 * Created by ivanjlzhang on 17-9-21.
 */

public class Event extends BaseMessage {
    public Event(Header header, Payload payload) {
        super(header, payload);
    }
}
