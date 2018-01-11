package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.AttachedContentPayload;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import org.codehaus.jackson.annotate.JsonIgnore;

/**
 * Speak指令对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class SpeakPayload extends Payload implements AttachedContentPayload {
    public String url;
    public String format;
    public String token;

    @JsonIgnore
    public byte[] attachedContent;

    // start with cid:
    public void setUrl(String url) {
        this.url = url.substring(4);
    }

    @Override
    public boolean requiresAttachedContent() {
        return !hasAttachedContent();
    }

    @Override
    public boolean hasAttachedContent() {
        return attachedContent != null;
    }

    @Override
    public String getAttachedContentId() {
        return url;
    }

    @Override
    public byte[] getAttachedContent() {
        return attachedContent;
    }

    @Override
    public void setAttachedContent(String cid, byte[] data) {
        if (getAttachedContentId().equals(cid)) {
            this.attachedContent = data;
        } else {
            throw new IllegalArgumentException(
                    "Tried to add the wrong audio content to a Speak directive. This cid: "
                            + getAttachedContentId() + " other cid: " + cid);
        }
    }
}
