package com.wistron.demo.tool.teddybear.dcs.systemimpl;

import com.wistron.demo.tool.teddybear.dcs.framework.IResponseListener;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlaybackController;

/**
 * 播放控制
 * Created by ivanjlzhang on 17-9-23.
 */

public class PlaybackControllerImpl implements IPlaybackController {
    private IPlaybackListener playbackListener;

    @Override
    public void play(IResponseListener responseListener) {
        if (playbackListener == null) {
            return;
        }
        playbackListener.onPlay(responseListener);
    }

    @Override
    public void pause(IResponseListener responseListener) {
        if (playbackListener == null) {
            return;
        }
        playbackListener.onPause(responseListener);
    }

    @Override
    public void previous(IResponseListener responseListener) {
        if (playbackListener == null) {
            return;
        }
        playbackListener.onPrevious(responseListener);
    }

    @Override
    public void next(IResponseListener responseListener) {
        if (playbackListener == null) {
            return;
        }
        playbackListener.onNext(responseListener);
    }

    @Override
    public void registerPlaybackListener(IPlaybackListener listener) {
        this.playbackListener = listener;
    }
}
