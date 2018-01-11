package com.wistron.demo.tool.teddybear.dcs.framework.message;

import java.util.UUID;

/**
 * 带有MessageId的Header
 * Created by ivanjlzhang on 17-9-21.
 */

public class MessageIdHeader extends Header {
    private String messageId;

    public MessageIdHeader() {
        super();
    }

    public MessageIdHeader(String nameSpace, String name) {
        super(nameSpace, name);

        messageId = UUID.randomUUID().toString();
    }

    public final void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public final String getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return String.format("%1$s id:%2$s", super.toString(), messageId);
    }
}
