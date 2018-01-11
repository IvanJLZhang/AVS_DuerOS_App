package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message;

import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;

/**
 * Audio Player模块上报PlaybackFailed事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class PlaybackFailedPayload extends Payload {
    private String token;
    private PlaybackStatePayload currentPlaybackState;
    private ErrorStructure error;

    public PlaybackFailedPayload(String token, PlaybackStatePayload playbackState,
                                 IMediaPlayer.ErrorType errorType) {
        this.token = token;
        this.currentPlaybackState = playbackState;
        error = new ErrorStructure(errorType);
    }

    public String getToken() {
        return token;
    }

    public PlaybackStatePayload getCurrentPlaybackState() {
        return currentPlaybackState;
    }

    public ErrorStructure getError() {
        return error;
    }

    private static final class ErrorStructure {
        private IMediaPlayer.ErrorType type;
        private String message;

        public ErrorStructure(IMediaPlayer.ErrorType type) {
            this.type = type;
            this.message = type.getMessage();
        }

        public IMediaPlayer.ErrorType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
