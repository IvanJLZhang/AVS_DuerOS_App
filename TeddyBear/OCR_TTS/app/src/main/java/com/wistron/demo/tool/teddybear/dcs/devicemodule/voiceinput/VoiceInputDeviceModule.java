package com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.HandleDirectiveException;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.message.ListenStartedPayload;
import com.wistron.demo.tool.teddybear.dcs.framework.BaseDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.framework.DcsResponseDispatcher;
import com.wistron.demo.tool.teddybear.dcs.framework.DialogRequestIdHandler;
import com.wistron.demo.tool.teddybear.dcs.framework.IMessageSender;
import com.wistron.demo.tool.teddybear.dcs.framework.IResponseListener;
import com.wistron.demo.tool.teddybear.dcs.framework.message.ClientContext;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DialogRequestIdHeader;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Event;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioInput;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IMediaPlayer;

import org.litepal.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class VoiceInputDeviceModule extends BaseDeviceModule {
    private static final String TAG = "VoiceInputDeviceModule";
    private final IAudioInput audioInput;
    private final List<IVoiceInputListener> voiceInputListeners;
    private final IMediaPlayer mediaPlayer;
    private final DialogRequestIdHandler dialogRequestIdHandler;
    private final DcsResponseDispatcher dcsResponseDispatcher;

    public VoiceInputDeviceModule(final IMediaPlayer mediaPlayer,
                                  IMessageSender messageSender,
                                  final IAudioInput audioInput,
                                  DialogRequestIdHandler dialogRequestIdHandler,
                                  DcsResponseDispatcher dcsResponseDispatcher) {
        super(ApiConstants.NAMESPACE, messageSender);
        this.audioInput = audioInput;
        this.voiceInputListeners = Collections.synchronizedList(new ArrayList<IVoiceInputListener>());
        this.mediaPlayer = mediaPlayer;
        this.dialogRequestIdHandler = dialogRequestIdHandler;
        this.dcsResponseDispatcher = dcsResponseDispatcher;

        this.audioInput.registerAudioInputListener(new IAudioInput.IAudioInputListener() {
            @Override
            public void onStartRecord(DcsStreamRequestBody dcsStreamRequestBody) {
                stopSpeaker();
                // 发送网络请求
                sendListenStartedEvent(dcsStreamRequestBody, new IResponseListener() {
                    @Override
                    public void onSucceed(int statusCode) {
                        fireOnSucceed(statusCode);
                        // 没有下发新的语音speak-stream
                        if (statusCode == 204) {
                            // 设置对话通道为非活跃状态
                            mediaPlayer.setActive(false);
                        } else {
                            mediaPlayer.setActive(true);
                        }
                    }

                    @Override
                    public void onFailed(String errorMessage) {
                        LogUtil.d(TAG, "onFailed,errorMessage:" + errorMessage);
                        fireOnFailed(errorMessage);
                        audioInput.stopRecord();
                        mediaPlayer.setActive(false);
                    }
                });

                fireOnStartRecord();
            }

            @Override
            public void onStopRecord() {
                fireFinishRecord();
            }
        });
    }


    @Override
    public ClientContext clientContext() {
        return null;
    }

    @Override
    public void handleDirective(Directive directive) throws HandleDirectiveException {
        String name = directive.getName();
        if (name.equals(ApiConstants.Directives.StopListen.NAME)) {
            audioInput.stopRecord();
        } else if (name.equals(ApiConstants.Directives.Listen.NAME)) {
            audioInput.startRecord();
        } else {
            String message = "No device to handle the directive";
            throw new HandleDirectiveException(
                    HandleDirectiveException.ExceptionType.UNSUPPORTED_OPERATION,
                    message);
        }
    }

    @Override
    public void release() {
        voiceInputListeners.clear();
    }

    /**
     * 停止speaker对话通道的语音播放
     */
    private void stopSpeaker() {
        mediaPlayer.setActive(true);
        mediaPlayer.stop();
        dcsResponseDispatcher.interruptDispatch();
    }

    private void sendListenStartedEvent(DcsStreamRequestBody streamRequestBody, IResponseListener responseListener) {
        String dialogRequestId = dialogRequestIdHandler.createActiveDialogRequestId();
        String name = ApiConstants.Events.ListenStarted.NAME;
        DialogRequestIdHeader header = new DialogRequestIdHeader(getNameSpace(), name, dialogRequestId);
        Payload payload = new ListenStartedPayload(ListenStartedPayload.FORMAT);
        Event event = new Event(header, payload);
        messageSender.sendEvent(event, streamRequestBody, responseListener);
    }

    private void fireOnStartRecord() {
        for (IVoiceInputListener listener : voiceInputListeners) {
            listener.onStartRecord();
        }
    }

    private void fireFinishRecord() {
        for (IVoiceInputListener listener : voiceInputListeners) {
            listener.onFinishRecord();
        }
    }

    private void fireOnSucceed(int statusCode) {
        for (IVoiceInputListener listener : voiceInputListeners) {
            listener.onSucceed(statusCode);
        }
    }

    private void fireOnFailed(String errorMessage) {
        for (IVoiceInputListener listener : voiceInputListeners) {
            listener.onFailed(errorMessage);
        }
    }

    public void addVoiceInputListener(IVoiceInputListener listener) {
        this.voiceInputListeners.add(listener);
    }

    public interface IVoiceInputListener {
        /**
         * 开始录音的回调
         */
        void onStartRecord();

        /**
         * 结束录音的回调
         */
        void onFinishRecord();

        /**
         * 录音-网络请求成功
         *
         * @param statusCode 网络返回状态码
         */
        void onSucceed(int statusCode);

        /**
         * 录音-网络请求失败
         *
         * @param errorMessage 错误信息
         */
        void onFailed(String errorMessage);
    }
}
