package com.wistron.demo.tool.teddybear.dcs.http.exceptions;

/**
 * 网络模块的异常处理，比如参数检查
 * Created by ivanjlzhang on 17-9-22.
 */

public class Exceptions {
    public static void illegalArgument(String msg, Object... params) {
        throw new IllegalArgumentException(String.format(msg, params));
    }
}
