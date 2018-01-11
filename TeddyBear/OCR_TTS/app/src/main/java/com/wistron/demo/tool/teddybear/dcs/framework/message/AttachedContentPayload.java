package com.wistron.demo.tool.teddybear.dcs.framework.message;

/**
 * 语音输出speak指令附加内容
 * Created by ivanjlzhang on 17-9-22.
 */

public interface AttachedContentPayload {
    boolean requiresAttachedContent();
    boolean hasAttachedContent();
    String getAttachedContentId();
    byte[] getAttachedContent();
    void setAttachedContent(String contentId, byte[] attachmentContent);
}
