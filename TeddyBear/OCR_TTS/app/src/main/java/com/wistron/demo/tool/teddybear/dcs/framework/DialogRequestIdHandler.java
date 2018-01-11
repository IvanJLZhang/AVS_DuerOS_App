package com.wistron.demo.tool.teddybear.dcs.framework;

import java.util.UUID;

/**
 * 生成dialogRequestId
 * Created by ivanjlzhang on 17-9-21.
 */

public class DialogRequestIdHandler {
    private String activeDialogRequestId;

    public DialogRequestIdHandler() {
    }

    public String createActiveDialogRequestId() {
        activeDialogRequestId = UUID.randomUUID().toString();
        return activeDialogRequestId;
    }

    /**
     * 判断当前dialogRequestId是否活跃的
     *
     * @param dialogRequestId dialogRequestId
     * @return dialogRequestId与activeDialogRequestId相等则返回true，否则返回false
     */
    public Boolean isActiveDialogRequestId(String dialogRequestId) {
        return activeDialogRequestId != null && activeDialogRequestId.equals(dialogRequestId);
    }
}
