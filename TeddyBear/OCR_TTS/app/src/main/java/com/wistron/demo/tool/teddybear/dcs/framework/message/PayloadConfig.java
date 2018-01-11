package com.wistron.demo.tool.teddybear.dcs.framework.message;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message.DeleteAlertPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message.SetAlertPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message.ClearQueuePayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message.PlayPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.message.StopPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.message.HtmlPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message.AdjustVolumePayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message.SetMutePayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.message.SetVolumePayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message.SetEndPointPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.message.ThrowExceptionPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.message.ListenPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.message.SpeakPayload;

import java.util.HashMap;

/**
 * Created by ivanjlzhang on 17-9-21.
 */

public class PayloadConfig {
    private final HashMap<String, Class<?>> payloadClass;

    private static class PayloadConfigHolder {
        private static final PayloadConfig instance = new PayloadConfig();
    }

    public static PayloadConfig getInstance() {
        return PayloadConfigHolder.instance;
    }

    private PayloadConfig() {
        payloadClass = new HashMap<>();

        // AudioInputImpl
        String namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.ApiConstants.NAMESPACE;

        String name = com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceinput.ApiConstants.Directives.Listen.NAME;
        insertPayload(namespace, name, ListenPayload.class);

        // VoiceOutput
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.ApiConstants.Directives.Speak.NAME;
        insertPayload(namespace, name, SpeakPayload.class);

        // audio
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.ApiConstants.Directives.Play.NAME;
        insertPayload(namespace, name, PlayPayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.ApiConstants.Directives.Stop.NAME;
        insertPayload(namespace, name, StopPayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.audioplayer.ApiConstants.Directives.ClearQueue.NAME;
        insertPayload(namespace, name, ClearQueuePayload.class);

        //  alert
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.ApiConstants.Directives.SetAlert.NAME;
        insertPayload(namespace, name, SetAlertPayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.ApiConstants.Directives.DeleteAlert.NAME;
        insertPayload(namespace, name, DeleteAlertPayload.class);

        // SpeakController
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.ApiConstants.Directives.SetVolume.NAME;
        insertPayload(namespace, name, SetVolumePayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.ApiConstants.Directives.AdjustVolume.NAME;
        insertPayload(namespace, name, AdjustVolumePayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.speakController.ApiConstants.Directives.SetMute.NAME;
        insertPayload(namespace, name, SetMutePayload.class);

        // System
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.system.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.system.ApiConstants.Directives.SetEndpoint.NAME;
        insertPayload(namespace, name, SetEndPointPayload.class);

        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.system.ApiConstants.Directives.ThrowException.NAME;
        insertPayload(namespace, name, ThrowExceptionPayload.class);

        // Screen
        namespace = com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.ApiConstants.NAMESPACE;
        name = com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.ApiConstants.Directives.HtmlView.NAME;
        insertPayload(namespace, name, HtmlPayload.class);
    }

    void insertPayload(String namespace, String name, Class<?> type) {
        final String key = namespace + name;
        payloadClass.put(key, type);
    }

    Class<?> findPayloadClass(String namespace, String name) {
        final String key = namespace + name;
        return payloadClass.get(key);
    }
}
