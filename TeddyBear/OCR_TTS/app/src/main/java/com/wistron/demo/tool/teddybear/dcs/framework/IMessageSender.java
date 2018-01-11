package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Event;

/**
 * 发送event接口
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IMessageSender {
    /**
     * 发送event
     *
     * @param event
     */
    void sendEvent(Event event);

    /**
     * 发送event
     *
     * @param event
     * @param responseListener 回调
     */
    void sendEvent(Event event, IResponseListener responseListener);

    /**
     * 发送event且带有stream
     *
     * @param event             event对象
     * @param streamRequestBody stream对象
     * @param responseListener  回调
     */
    void sendEvent(Event event, DcsStreamRequestBody streamRequestBody, IResponseListener responseListener);

    /**
     * 发送event带上clientContext
     *
     * @param event            event对象
     * @param responseListener 回调
     */
    void sentEventWithClientContext(Event event, IResponseListener responseListener);
}
