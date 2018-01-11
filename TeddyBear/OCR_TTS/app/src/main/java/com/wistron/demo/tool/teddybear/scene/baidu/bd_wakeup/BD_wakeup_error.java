package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

/**
 * Created by ivanjlzhang on 17-9-13.
 */

public class BD_wakeup_error {
    public static String WakeupError(int errorCode){
        String message = null;
        switch (errorCode) {
            case 1:
                message = "params error";
                break;
            case 2:
                message = "network request error";
                break;
            case 3:
                message = "server recognizer error";
                break;
            case 4:
                message = "no available network";
                break;
            default:
                message = "unknown error:" + errorCode;
                break;
        }
        return message;
    }
}
