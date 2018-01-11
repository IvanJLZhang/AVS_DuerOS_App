package com.wistron.demo.tool.teddybear.dcs.systemimpl;

import android.content.Context;
import android.os.Looper;

import com.wistron.demo.tool.teddybear.dcs.systemimpl.alert.AlertsFileDataStoreImpl;
import com.wistron.demo.tool.teddybear.dcs.systemimpl.audioinput.AudioVoiceInputImpl;
import com.wistron.demo.tool.teddybear.dcs.systemimpl.player.MediaPlayerImpl;
import com.wistron.demo.tool.teddybear.dcs.systemimpl.wakeup.WakeUpImpl;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAlertDataStore;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioInput;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioRecord;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IHandler;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlatformFactory;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlaybackController;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IWakeUp;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IWebView;

import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class PlatformFactoryImpl implements IPlatformFactory {
    private IHandler mainHandler;
    private IAudioInput voiceInput;
    private IWebView webView;
    private IPlaybackController playback;
    private Context context;
    private IAudioRecord audioRecord;
    private LinkedBlockingDeque<byte[]> linkedBlockingDeque = new LinkedBlockingDeque<>();

    public PlatformFactoryImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public IHandler createHandler() {
        return new HandlerImpl();
    }

    @Override
    public IHandler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new HandlerImpl(Looper.getMainLooper());
        }

        return mainHandler;
    }

    @Override
    public IAudioRecord getAudioRecord() {
        if (audioRecord == null) {
            audioRecord = new AudioRecordThread(linkedBlockingDeque);
        }
        return audioRecord;
    }

    @Override
    public void releaseAudioRecord(){
        if(audioRecord != null){
            audioRecord = null;
        }
    }
    @Override
    public IWakeUp getWakeUp() {
        return new WakeUpImpl(context, linkedBlockingDeque);
    }

    @Override
    public IAudioInput getVoiceInput() {
        if (voiceInput == null) {
            voiceInput = new AudioVoiceInputImpl(linkedBlockingDeque);
        }

        return voiceInput;
    }

    @Override
    public IMediaPlayer createMediaPlayer() {
        return new MediaPlayerImpl();
    }

    public IAlertDataStore createAlertsDataStore() {
        return new AlertsFileDataStoreImpl();
    }

    @Override
    public IWebView getWebView() {
        return webView;
    }

    @Override
    public IPlaybackController getPlayback() {
        if (playback == null) {
            playback = new PlaybackControllerImpl();
        }

        return playback;
    }

    public void setWebView(IWebView webView) {
        this.webView = webView;
    }

}
