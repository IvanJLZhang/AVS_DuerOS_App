package com.wistron.demo.tool.teddybear.dcs.devicemodule.system;

/**
 * Created by ivanjlzhang on 17-9-21.
 */

public class HandleDirectiveException extends Exception {
    private ExceptionType exceptionType;

    public HandleDirectiveException(ExceptionType exceptionType, String message) {
        super(message);
        this.exceptionType = exceptionType;
    }

    public ExceptionType getExceptionType() {
        return exceptionType;
    }

    public enum ExceptionType {
        UNEXPECTED_INFORMATION_RECEIVED,
        UNSUPPORTED_OPERATION,
        INTERNAL_ERROR
    }
}
