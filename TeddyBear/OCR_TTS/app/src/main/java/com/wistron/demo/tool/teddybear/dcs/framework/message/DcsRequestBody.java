package com.wistron.demo.tool.teddybear.dcs.framework.message;

import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.ArrayList;

/**
 * 网络请求的消息体
 * Created by ivanjlzhang on 17-9-22.
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class DcsRequestBody {
    private ArrayList<ClientContext> clientContext;
    private Event event;

    public DcsRequestBody(Event event) {
        this.event = event;
    }

    public void setClientContext(ArrayList<ClientContext> clientContexts) {
        this.clientContext = clientContexts;
    }

    public ArrayList<ClientContext> getClientContext() {
        return clientContext;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Event getEvent() {
        return event;
    }
}
