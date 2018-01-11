package com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.report;

import java.util.concurrent.TimeUnit;

/**
 * audio 播放统计OffsetInMilliseconds
 * Created by ivanjlzhang on 17-9-22.
 */

public class AudioPlayerTimer {
    private long startNano;
    private long elapsedTimeMs;
    private long totalStreamLength;
    private boolean isPlaying = false;

    public synchronized void start() {
        startNano = System.nanoTime();
        isPlaying = true;
    }

    public synchronized void stop() {
        if (isPlaying) {
            elapsedTimeMs += getCurrentOffsetInMilliseconds();
            isPlaying = false;
        }
    }

    public synchronized long getOffsetInMilliseconds() {
        long offset = elapsedTimeMs + (isPlaying ? getCurrentOffsetInMilliseconds() : 0);
        if (totalStreamLength > 0) {
            offset = Math.min(totalStreamLength, offset);
        }
        return offset;
    }

    public void reset() {
        reset(0);
    }

    public void reset(long startPosition) {
        reset(startPosition, -1);
    }

    public synchronized void reset(long startPosition, long maxPosition) {
        elapsedTimeMs = startPosition;
        isPlaying = false;
        startNano = System.nanoTime();
        totalStreamLength = maxPosition;
    }

    private long getCurrentOffsetInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - startNano, TimeUnit.NANOSECONDS);
    }
}
