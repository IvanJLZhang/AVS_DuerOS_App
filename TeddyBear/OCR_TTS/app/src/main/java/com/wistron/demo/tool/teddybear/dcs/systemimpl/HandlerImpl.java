package com.wistron.demo.tool.teddybear.dcs.systemimpl;

import android.os.Handler;
import android.os.Looper;

import com.wistron.demo.tool.teddybear.dcs.systeminterface.IHandler;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class HandlerImpl implements IHandler {
    private final Handler handler;

    public HandlerImpl() {
        handler = new Handler();
    }

    public HandlerImpl(Looper looper) {
        handler = new Handler(looper);
    }

    @Override
    public boolean post(Runnable runnable) {
        return handler.post(runnable);
    }
}
