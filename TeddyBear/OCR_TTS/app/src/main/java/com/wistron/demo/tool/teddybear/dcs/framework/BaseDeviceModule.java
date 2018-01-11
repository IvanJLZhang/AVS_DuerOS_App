package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.HandleDirectiveException;
import com.wistron.demo.tool.teddybear.dcs.framework.message.ClientContext;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;


/**
 * Created by ivanjlzhang on 17-9-21.
 */

public abstract class BaseDeviceModule {
    private final String nameSpace;
    protected final IMessageSender messageSender;

    public BaseDeviceModule(String nameSpace) {
        this(nameSpace, null);
    }

    public BaseDeviceModule(String nameSpace, IMessageSender messageSender) {
        this.nameSpace = nameSpace;
        this.messageSender = messageSender;
    }

    /**
     * 端状态：服务端在处理某些事件时，需要了解在请求当时设备端各模块所处的状态。比如端上是否正在播放音乐，
     * 是否有闹钟在响，是否正在播报等等
     *
     * @return 端状态
     */
    public abstract ClientContext clientContext();

    /**
     * 处理服务端下发的指令
     *
     * @param directive
     * @throws HandleDirectiveException
     */
    public abstract void handleDirective(Directive directive) throws HandleDirectiveException;

    public abstract void release();

    public String getNameSpace() {
        return nameSpace;
    }

}
