package com.wistron.demo.tool.teddybear.dcs.systeminterface;

/**
 * 定义平台相关工厂
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IPlatformFactory {
    IHandler createHandler();
    IHandler getMainHandler();
    IAudioRecord getAudioRecord();
    void releaseAudioRecord();


    IWakeUp getWakeUp();
    IAudioInput getVoiceInput();
    IMediaPlayer createMediaPlayer();

    IAlertDataStore createAlertsDataStore();
    IWebView getWebView();
    void setWebView(IWebView webView);
    IPlaybackController getPlayback();
}
