package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

/**
 * 存储音频数据
 * Created by ivanjlzhang on 17-9-22.
 */

public class AudioData {
    public String contentId;
    public byte[] partBytes;

    public AudioData(String contentId, byte[] partBytes) {
        this.contentId = contentId;
        this.partBytes = partBytes;
    }
}
