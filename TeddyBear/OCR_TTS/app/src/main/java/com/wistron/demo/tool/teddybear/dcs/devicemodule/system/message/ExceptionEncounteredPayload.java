package com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.HandleDirectiveException;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

/**
 * ExceptionEncountered事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class ExceptionEncounteredPayload extends Payload {
    private String unparsedDirective;
    private Error error;

    public ExceptionEncounteredPayload(String unparsedDirective, HandleDirectiveException.ExceptionType type, String message) {
        this.unparsedDirective = unparsedDirective;
        Error error = new Error(type, message);
        this.error = error;
    }

    public void setUnparsedDirective(String unparsedDirective) {
        this.unparsedDirective = unparsedDirective;
    }

    public String getUnparsedDirective() {
        return unparsedDirective;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public Error getError() {
        return error;
    }

    public static class Error {
        public HandleDirectiveException.ExceptionType type;
        public String message;

        public Error(HandleDirectiveException.ExceptionType type, String message) {
            this.type = type;
            this.message = message;
        }
    }
}
