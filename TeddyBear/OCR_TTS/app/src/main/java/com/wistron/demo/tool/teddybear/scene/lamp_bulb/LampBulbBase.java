package com.wistron.demo.tool.teddybear.scene.lamp_bulb;

import android.content.Context;
import android.os.Handler;

/**
 * Created by king on 17-2-28.
 */

public abstract class LampBulbBase {
    protected Context mContext;
    protected Handler mMainHandler;
    protected onBulbStateChangedListener mListener;

    public enum BlubsAction {
        NOTHING,
        TURN_ON_LIGHT,
        TURN_OFF_LIGHT,
        ADJUST_BRIGHT,
        //....
    }

    protected BlubsAction mBlubsAction = BlubsAction.NOTHING;

    public abstract void startSearch();

    public abstract void turnLightState(boolean on);

    public void adjustBright(int bright) {
    }

    public abstract void destroy();

    public LampBulbBase(Context mContext) {
        this.mContext = mContext;
    }

    public void setOnBulbStateChangedListener(onBulbStateChangedListener listener) {
        mListener = listener;
    }

    public interface onBulbStateChangedListener {
        void searchStart();

        void searchEnd();

        void updateBulbLog(String log);

        void updateBulbLogAndSpeak(String log);
    }
}
