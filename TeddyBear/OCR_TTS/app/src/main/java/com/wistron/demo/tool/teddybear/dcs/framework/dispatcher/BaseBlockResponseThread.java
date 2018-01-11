package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import com.wistron.demo.tool.teddybear.dcs.framework.DcsResponseDispatcher;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;

import java.util.concurrent.BlockingQueue;

/**
 * 单独线程阻塞控制response回调处理
 * Created by ivanjlzhang on 17-9-22.
 */

public abstract class BaseBlockResponseThread extends Thread {
    private static final String TAG = BaseBlockResponseThread.class.getSimpleName();
    private BlockingQueue<DcsResponseBody> responseBodyDeque;
    private DcsResponseDispatcher.IDcsResponseHandler responseHandler;
    private volatile boolean block;
    private volatile boolean isStop;

    public BaseBlockResponseThread(BlockingQueue<DcsResponseBody> responseBodyDeque,
                                   DcsResponseDispatcher.IDcsResponseHandler responseHandler, String threadName) {
        this.responseBodyDeque = responseBodyDeque;
        this.responseHandler = responseHandler;
        setName(threadName);
    }

    public synchronized void block() {
        block = true;
    }

    public synchronized void unblock() {
        block = false;
        notify();
    }

    public synchronized void clear() {
        responseBodyDeque.clear();
    }

    public synchronized void stopThread() {
        clear();
        isStop = true;
        this.interrupt();
    }

    @Override
    public void run() {
        while (!isStop) {
            try {
                synchronized (this) {
                    if (block) {
                        wait();
                    }
                }

                if (responseHandler != null) {
                    DcsResponseBody responseBody = responseBodyDeque.take();
                    responseHandler.onResponse(responseBody);

                    if (shouldBlock(responseBody)) {
                        block = true;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    abstract boolean shouldBlock(DcsResponseBody responseBody);
}
