package com.wistron.demo.tool.teddybear.parent_side;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

/**
 * 时间：16-4-15 15:39
 * 作者：bob
 */
public class ErrorCode {
    public final static int SUCCESS = 1000;
    public final static int E_NOSDCARD = 1001;
    public final static int E_STATE_RECODING = 1002;
    public final static int E_UNKOWN = 1003;


    public static String getErrorInfo(Context vContext, int vType) throws NotFoundException {
        switch (vType) {
            case SUCCESS:
                return "success";
            case E_NOSDCARD:
                return "error_no_sdcard";
            case E_STATE_RECODING:
                return "error_state_record";
            case E_UNKOWN:
            default:
                return "error_unknown";

        }
    }
}
