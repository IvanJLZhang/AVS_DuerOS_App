package com.wistron.demo.tool.teddybear.dcs.framework.message;

/**
 * 端状态：服务端在处理某些事件时，需要了解在请求当时设备端各模块所处的状态。比如端上是否正在播放音乐，
 * 是否有闹钟在响，是否正在播报等等
 * Created by ivanjlzhang on 17-9-21.
 */

public class ClientContext extends BaseMessage {
    public ClientContext(Header header, Payload payload) {
        super(header, payload);
    }
}
