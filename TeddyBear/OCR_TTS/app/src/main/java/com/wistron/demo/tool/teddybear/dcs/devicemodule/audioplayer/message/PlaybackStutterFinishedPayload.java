package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message;

/**
 * Audio Player模块上报PlaybackStutterFinished事件对应的payload结构
 * Created by ivanjlzhang on 17-9-22.
 */

public class PlaybackStutterFinishedPayload extends AudioPlayerPayload {
    public long stutterDurationInMilliseconds;

    public PlaybackStutterFinishedPayload(String token, long offsetInMilliseconds,
                                          long stutterDurationInMilliseconds) {
        super(token, offsetInMilliseconds);
        this.stutterDurationInMilliseconds = stutterDurationInMilliseconds;
    }
}
