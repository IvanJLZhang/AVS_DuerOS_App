package com.wistron.demo.tool.teddybear.dcs.systeminterface;

/**
 * 录音采集接口
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IAudioRecord {
    /**
     * start record
     */
    void startRecord();

    /**
     * stop record
     */
    void stopRecord();
}
