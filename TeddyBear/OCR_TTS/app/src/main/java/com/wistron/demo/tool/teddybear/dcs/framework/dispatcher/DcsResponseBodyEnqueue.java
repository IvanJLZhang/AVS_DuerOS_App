package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import com.wistron.demo.tool.teddybear.dcs.framework.DialogRequestIdHandler;
import com.wistron.demo.tool.teddybear.dcs.framework.message.AttachedContentPayload;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DialogRequestIdHeader;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Header;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;

import org.litepal.util.LogUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class DcsResponseBodyEnqueue {
    private static final String TAG = DcsResponseBodyEnqueue.class.getSimpleName();
    private final DialogRequestIdHandler dialogRequestIdHandler;
    private final Queue<DcsResponseBody> dependentQueue;
    private final Queue<DcsResponseBody> independentQueue;
    private final Queue<DcsResponseBody> incompleteResponseQueue;
    private final Map<String, AudioData> audioDataMap;

    public DcsResponseBodyEnqueue(DialogRequestIdHandler dialogRequestIdHandler,
                                  Queue<DcsResponseBody> dependentQueue,
                                  Queue<DcsResponseBody> independentQueue) {
        this.dialogRequestIdHandler = dialogRequestIdHandler;
        this.dependentQueue = dependentQueue;
        this.independentQueue = independentQueue;
        incompleteResponseQueue = new LinkedList<>();
        audioDataMap = new HashMap<>();
    }

    public synchronized void handleResponseBody(DcsResponseBody responseBody) {
        if (responseBody.getDirective() == null) {
            return;
        }

        incompleteResponseQueue.add(responseBody);
        matchAudioDataWithResponseBody();
    }

    public synchronized void handleAudioData(AudioData audioData) {
        audioDataMap.put(audioData.contentId, audioData);
        matchAudioDataWithResponseBody();
    }

    private void matchAudioDataWithResponseBody() {
        for (DcsResponseBody responseBody : incompleteResponseQueue) {
            Directive directive = responseBody.getDirective();
            if (directive == null) {
                return;
            }

            Payload payload = responseBody.getDirective().payload;
            if (payload instanceof AttachedContentPayload) {
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;
                String contentId = attachedContentPayload.getAttachedContentId();
                AudioData audioData = audioDataMap.remove(contentId);
                if (audioData != null) {
                    attachedContentPayload.setAttachedContent(contentId, audioData.partBytes);
                }
            }
        }

        findCompleteResponseBody();
    }

    private void findCompleteResponseBody() {
        Iterator<DcsResponseBody> iterator = incompleteResponseQueue.iterator();
        while (iterator.hasNext()) {
            DcsResponseBody responseBody = iterator.next();
            Payload payload = responseBody.getDirective().payload;
            if (payload instanceof AttachedContentPayload) {
                AttachedContentPayload attachedContentPayload = (AttachedContentPayload) payload;

                if (!attachedContentPayload.requiresAttachedContent()) {
                    // The front most directive IS complete.
                    enqueueResponseBody(responseBody);
                    iterator.remove();
                } else {
                    break;
                }
            } else {
                // Immediately enqueue any directive which does not contain audio content
                enqueueResponseBody(responseBody);
                iterator.remove();
            }
        }
    }

    private void enqueueResponseBody(DcsResponseBody responseBody) {
        LogUtil.d(TAG, "DcsResponseBodyEnqueue-RecordThread:" + responseBody.getDirective().rawMessage);
        Header header = responseBody.getDirective().header;
        DialogRequestIdHeader dialogRequestIdHeader = (DialogRequestIdHeader) header;
        if (dialogRequestIdHeader.getDialogRequestId() == null) {
            LogUtil.d(TAG, "DcsResponseBodyEnqueue-DialogRequestId  is null ,add to independentQueue");
            independentQueue.add(responseBody);
        } else if (dialogRequestIdHandler.isActiveDialogRequestId(dialogRequestIdHeader.getDialogRequestId())) {
            LogUtil.d(TAG, "DcsResponseBodyEnqueue-DialogRequestId  not  null,add to dependentQueue");
            dependentQueue.add(responseBody);
        }
    }
}
