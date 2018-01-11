package com.wistron.demo.tool.teddybear.dcs.devicemodule.playbackcontroller;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.AlertsDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.HandleDirectiveException;
import com.wistron.demo.tool.teddybear.dcs.framework.BaseDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.framework.IMessageSender;
import com.wistron.demo.tool.teddybear.dcs.framework.IResponseListener;
import com.wistron.demo.tool.teddybear.dcs.framework.message.ClientContext;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Event;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Header;
import com.wistron.demo.tool.teddybear.dcs.framework.message.MessageIdHeader;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlaybackController;

/**
 * 音频播放控制
 * 用户按了端上的播放/暂停等控制按钮，或者通过端上的GUI进行了此类操作时，上报PlayCommandIssued、PauseCommandIssued等事件
 * Created by ivanjlzhang on 17-9-22.
 */

public class PlaybackControllerDeviceModule extends BaseDeviceModule {
    private AlertsDeviceModule mAlertsDeviceModule;

    public enum PlaybackAction {
        PLAY,
        PAUSE,
        PREVIOUS,
        NEXT
    }

    public PlaybackControllerDeviceModule(IPlaybackController playback, IMessageSender messageSender,
                                          AlertsDeviceModule alertsDeviceModule) {
        super(ApiConstants.NAMESPACE, messageSender);
        this.mAlertsDeviceModule = alertsDeviceModule;
        playback.registerPlaybackListener(new IPlaybackController.IPlaybackListener() {
            @Override
            public void onPlay(IResponseListener responseListener) {
                handlePlaybackAction(PlaybackAction.PLAY, responseListener);
            }

            @Override
            public void onPause(IResponseListener responseListener) {
                handlePlaybackAction(PlaybackAction.PAUSE, responseListener);
            }

            @Override
            public void onPrevious(IResponseListener responseListener) {
                handlePlaybackAction(PlaybackAction.PREVIOUS, responseListener);
            }

            @Override
            public void onNext(IResponseListener responseListener) {
                handlePlaybackAction(PlaybackAction.NEXT, responseListener);
            }
        });
    }

    @Override
    public ClientContext clientContext() {
        return null;
    }

    @Override
    public void handleDirective(Directive directive) throws HandleDirectiveException {
    }

    @Override
    public void release() {
    }

    private void handlePlaybackAction(PlaybackAction action, IResponseListener responseListener) {
        switch (action) {
            case PLAY:
                if (mAlertsDeviceModule.hasActiveAlerts()) {
                    mAlertsDeviceModule.stopActiveAlert();
                } else {
                    Event event = createPlaybackControllerEvent(ApiConstants.Events.PlayCommandIssued.NAME);
                    messageSender.sentEventWithClientContext(event, responseListener);
                }
                break;
            case PAUSE:
                if (mAlertsDeviceModule.hasActiveAlerts()) {
                    mAlertsDeviceModule.stopActiveAlert();
                } else {
                    Event event = createPlaybackControllerEvent(ApiConstants.Events.PauseCommandIssued.NAME);
                    messageSender.sentEventWithClientContext(event, responseListener);
                }
                break;
            case PREVIOUS:
                Event event = createPlaybackControllerEvent(ApiConstants.Events.PreviousCommandIssued.NAME);
                messageSender.sentEventWithClientContext(event, responseListener);
                break;
            case NEXT:
                Event eventNext = createPlaybackControllerEvent(ApiConstants.Events.NextCommandIssued.NAME);
                messageSender.sentEventWithClientContext(eventNext, responseListener);
                break;
            default:
                break;
        }
    }

    private Event createPlaybackControllerEvent(String name) {
        Header header = new MessageIdHeader(ApiConstants.NAMESPACE, name);
        return new Event(header, new Payload());
    }
}
