package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.AlertsDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.AudioPlayerDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.playbackcontroller.PlaybackControllerDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.ScreenDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.SpeakerControllerDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.SystemDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message.SetEndPointPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message.ThrowExceptionPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.VoiceInputDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.VoiceOutputDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.http.HttpConfig;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlatformFactory;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlaybackController;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IWebView;

import org.litepal.util.LogUtil;

/**
 * 创建语音输入、语音输出、扬声器、音频播放器、播放控制、闹钟、屏幕显示和系统等deviceModule
 * Created by ivanjlzhang on 17-9-22.
 */

public class DeviceModuleFactory {
    private static final String TAG = "DeviceModuleFactory";
    private final IDeviceModuleHandler deviceModuleHandler;
    private final IMediaPlayer dialogMediaPlayer;

    private VoiceInputDeviceModule voiceInputDeviceModule;
    private VoiceOutputDeviceModule voiceOutputDeviceModule;
    private SpeakerControllerDeviceModule speakerControllerDeviceModule;
    private AudioPlayerDeviceModule audioPlayerDeviceModule;
    private AlertsDeviceModule alertsDeviceModule;
    private SystemDeviceModule systemDeviceModule;
    private PlaybackControllerDeviceModule playbackControllerDeviceModule;
    private ScreenDeviceModule screenDeviceModule;

    // 数字越大，优先级越高，播放优先级
    private enum MediaChannel {
        SPEAK("dialog", 3),
        ALERT("alert", 2),
        AUDIO("audio", 1);

        private String channelName;
        private int priority;

        MediaChannel(String channelName, int priority) {
            this.channelName = channelName;
            this.priority = priority;
        }
    }

    public DeviceModuleFactory(final IDeviceModuleHandler deviceModuleHandler) {
        this.deviceModuleHandler = deviceModuleHandler;
        dialogMediaPlayer = deviceModuleHandler.getMultiChannelMediaPlayer()
                .addNewChannel(MediaChannel.SPEAK.channelName, MediaChannel.SPEAK.priority);
    }


    public void createVoiceInputDeviceModule() {
        /*
         * 传入VoiceOutput的MediaPlayer，因为根据dcs协议的规范
         * 对话通道：
         * 对应语音输入（Voice Input）和语音输出（Voice Output）端能力；
         * 用户在语音请求时，或者设备在执行Speak指令进行播报时，对话通道进入活跃状态
         */
        voiceInputDeviceModule = new VoiceInputDeviceModule(
                dialogMediaPlayer, deviceModuleHandler.getMessageSender(),
                deviceModuleHandler.getPlatformFactory().getVoiceInput(),
                deviceModuleHandler.getDialogRequestIdHandler(),
                deviceModuleHandler.getResponseDispatcher());
        deviceModuleHandler.addDeviceModule(voiceInputDeviceModule);
    }

    public VoiceInputDeviceModule getVoiceInputDeviceModule() {
        return voiceInputDeviceModule;
    }

    public void createVoiceOutputDeviceModule() {
        voiceOutputDeviceModule = new VoiceOutputDeviceModule(dialogMediaPlayer,
                deviceModuleHandler.getMessageSender());
        voiceOutputDeviceModule.addVoiceOutputListener(new VoiceOutputDeviceModule.IVoiceOutputListener() {
            @Override
            public void onVoiceOutputStarted() {
                LogUtil.d(TAG, "DcsResponseBodyEnqueue-onVoiceOutputStarted ok ");
                deviceModuleHandler.getResponseDispatcher().blockDependentQueue();
            }

            @Override
            public void onVoiceOutputFinished() {
                LogUtil.d(TAG, "DcsResponseBodyEnqueue-onVoiceOutputFinished ok ");
                deviceModuleHandler.getResponseDispatcher().unBlockDependentQueue();
            }
        });

        deviceModuleHandler.addDeviceModule(voiceOutputDeviceModule);
    }

    public void createSpeakControllerDeviceModule() {
        BaseMultiChannelMediaPlayer.ISpeakerController speakerController =
                deviceModuleHandler.getMultiChannelMediaPlayer().getSpeakerController();
        speakerControllerDeviceModule =
                new SpeakerControllerDeviceModule(speakerController,
                        deviceModuleHandler.getMessageSender());
        deviceModuleHandler.addDeviceModule(speakerControllerDeviceModule);
    }

    public void createAudioPlayerDeviceModule() {
        IMediaPlayer mediaPlayer = deviceModuleHandler.getMultiChannelMediaPlayer()
                .addNewChannel(MediaChannel.AUDIO.channelName,
                        MediaChannel.AUDIO.priority);
        audioPlayerDeviceModule = new AudioPlayerDeviceModule(mediaPlayer,
                deviceModuleHandler.getMessageSender());
        deviceModuleHandler.addDeviceModule(audioPlayerDeviceModule);
    }

    public AudioPlayerDeviceModule getAudioPlayerDeviceModule() {
        return audioPlayerDeviceModule;
    }

    public void createAlertsDeviceModule() {
        IMediaPlayer mediaPlayer = deviceModuleHandler.getMultiChannelMediaPlayer()
                .addNewChannel(MediaChannel.ALERT.channelName,
                        MediaChannel.ALERT.priority);
        alertsDeviceModule = new AlertsDeviceModule(mediaPlayer,
                deviceModuleHandler.getPlatformFactory().createAlertsDataStore(),
                deviceModuleHandler.getMessageSender(),
                deviceModuleHandler.getPlatformFactory().getMainHandler());

        alertsDeviceModule.addAlertListener(new AlertsDeviceModule.IAlertListener() {
            @Override
            public void onAlertStarted(String alertToken) {
            }
        });

        deviceModuleHandler.addDeviceModule(alertsDeviceModule);
    }

    public void createSystemDeviceModule() {
        systemDeviceModule = new SystemDeviceModule(deviceModuleHandler.getMessageSender());
        systemDeviceModule.addModuleListener(new SystemDeviceModule.IDeviceModuleListener() {
            @Override
            public void onSetEndpoint(SetEndPointPayload endPointPayload) {
                if (null != endPointPayload) {
                    String endpoint = endPointPayload.getEndpoint();
                    if (null != endpoint && endpoint.length() > 0) {
                        HttpConfig.setEndpoint(endpoint);
                    }
                }
            }

            @Override
            public void onThrowException(ThrowExceptionPayload throwExceptionPayload) {
                LogUtil.d(TAG, throwExceptionPayload.toString());
            }
        });
        deviceModuleHandler.addDeviceModule(systemDeviceModule);
    }

    public SystemDeviceModule getSystemDeviceModule() {
        return systemDeviceModule;
    }

    public SystemDeviceModule.Provider getSystemProvider() {
        return systemDeviceModule.getProvider();
    }

    public void createPlaybackControllerDeviceModule() {
        IPlaybackController playback = deviceModuleHandler.getPlatformFactory().getPlayback();
        playbackControllerDeviceModule = new PlaybackControllerDeviceModule(playback,
                deviceModuleHandler.getMessageSender(), alertsDeviceModule);
        deviceModuleHandler.addDeviceModule(playbackControllerDeviceModule);
    }

    public void createScreenDeviceModule() {
        IWebView webView = deviceModuleHandler.getPlatformFactory().getWebView();
        screenDeviceModule = new ScreenDeviceModule(webView, deviceModuleHandler.getMessageSender());
        deviceModuleHandler.addDeviceModule(screenDeviceModule);
    }

    public interface IDeviceModuleHandler {
        IPlatformFactory getPlatformFactory();

        DialogRequestIdHandler getDialogRequestIdHandler();

        IMessageSender getMessageSender();

        BaseMultiChannelMediaPlayer getMultiChannelMediaPlayer();

        void addDeviceModule(BaseDeviceModule deviceModule);

        DcsResponseDispatcher getResponseDispatcher();
    }
}
