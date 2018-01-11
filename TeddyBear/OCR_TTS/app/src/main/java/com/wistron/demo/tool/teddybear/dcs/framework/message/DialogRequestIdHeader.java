package com.wistron.demo.tool.teddybear.dcs.framework.message;

/**
 * 带请求会话Id头部
 * Created by ivanjlzhang on 17-9-22.
 */

public class DialogRequestIdHeader extends MessageIdHeader {
    // 请求会话id
    private String dialogRequestId;

    public DialogRequestIdHeader() {

    }

    public DialogRequestIdHeader(String nameSpace, String name, String dialogRequestId) {
        super(nameSpace, name);
        this.dialogRequestId = dialogRequestId;
    }

    public final String getDialogRequestId() {
        return dialogRequestId;
    }

    public final void setDialogRequestId(String dialogRequestId) {
        this.dialogRequestId = dialogRequestId;
    }

    @Override
    public String toString() {
        return String.format("%1$s dialogRequestId:%2$s", super.toString(), dialogRequestId);
    }
}
