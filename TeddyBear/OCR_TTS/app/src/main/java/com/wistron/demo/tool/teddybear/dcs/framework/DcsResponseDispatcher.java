package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.framework.dispatcher.AudioData;
import com.wistron.demo.tool.teddybear.dcs.framework.dispatcher.DcsResponseBodyEnqueue;
import com.wistron.demo.tool.teddybear.dcs.framework.dispatcher.WithDialogIdBlockThread;
import com.wistron.demo.tool.teddybear.dcs.framework.dispatcher.WithoutDialogIdBlockThread;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * response调度中心，带有dialogRequestId directives按照顺序执行，对于speak指令播报完才执行下一个指令；
 * 设备端收到不带dialogRequestId的directives立即执行
 * <p>
 * Created by ivanjlzhang on 17-9-22.
 */

public class DcsResponseDispatcher {
    private final WithDialogIdBlockThread withDialogIdBlockThread;
    private final WithoutDialogIdBlockThread withoutDialogIdBlockThread;
    private final BlockingQueue<DcsResponseBody> dependentQueue;
    private final BlockingQueue<DcsResponseBody> independentQueue;
    private final DcsResponseBodyEnqueue dcsResponseBodyEnqueue;
    private final IDcsResponseHandler responseHandler;

    public DcsResponseDispatcher(final DialogRequestIdHandler dialogRequestIdHandler,
                                 final IDcsResponseHandler responseHandler) {
        this.responseHandler = responseHandler;
        dependentQueue = new LinkedBlockingDeque<>();
        independentQueue = new LinkedBlockingDeque<>();
        dcsResponseBodyEnqueue = new DcsResponseBodyEnqueue(dialogRequestIdHandler, dependentQueue,
                independentQueue);

        withDialogIdBlockThread = new WithDialogIdBlockThread(dependentQueue, responseHandler,
                "withDialogIdBlockThread");
        withoutDialogIdBlockThread = new WithoutDialogIdBlockThread(independentQueue, responseHandler,
                "withoutDialogIdBlockThread");
        withDialogIdBlockThread.start();
        withoutDialogIdBlockThread.start();
    }

    public void interruptDispatch() {
        // 先清空队列，比如播放一首歌：speak+play指令组合的方式，在speak播报过程中进行打断，play就不需要执行了
        withDialogIdBlockThread.clear();
        // 让其处于等待新的指令处理
        unBlockDependentQueue();
    }

    public void blockDependentQueue() {
        withDialogIdBlockThread.block();
    }

    public void unBlockDependentQueue() {
        withDialogIdBlockThread.unblock();
    }

    public void onResponseBody(DcsResponseBody responseBody) {
        dcsResponseBodyEnqueue.handleResponseBody(responseBody);
    }

    public void onAudioData(AudioData audioData) {
        dcsResponseBodyEnqueue.handleAudioData(audioData);
    }

    public void onParseFailed(String unParseMessage) {
        if (responseHandler != null) {
            responseHandler.onParseFailed(unParseMessage);
        }
    }

    public void release() {
        withDialogIdBlockThread.stopThread();
        withoutDialogIdBlockThread.stopThread();
    }

    public interface IDcsResponseHandler {
        void onResponse(DcsResponseBody responseBody);

        void onParseFailed(String unParseMessage);
    }
}
