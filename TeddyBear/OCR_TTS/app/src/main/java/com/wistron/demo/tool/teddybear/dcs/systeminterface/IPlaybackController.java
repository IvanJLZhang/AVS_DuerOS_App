package com.wistron.demo.tool.teddybear.dcs.systeminterface;

import com.wistron.demo.tool.teddybear.dcs.framework.IResponseListener;

/**
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IPlaybackController {
    void play(IResponseListener responseListener);

    void pause(IResponseListener responseListener);

    void previous(IResponseListener responseListener);

    void next(IResponseListener responseListener);

    void registerPlaybackListener(IPlaybackListener listener);

    interface IPlaybackListener {
        void onPlay(IResponseListener responseListener);

        void onPause(IResponseListener responseListener);

        void onPrevious(IResponseListener responseListener);

        void onNext(IResponseListener responseListener);
    }
}
