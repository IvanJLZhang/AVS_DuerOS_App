package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import com.wistron.demo.tool.teddybear.dcs.framework.DcsResponseDispatcher;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;

import java.util.concurrent.BlockingQueue;

/**
 * 服务器返回的指令header中没有dialogRequestId调度线程
 * Created by ivanjlzhang on 17-9-22.
 */

public class WithoutDialogIdBlockThread extends BaseBlockResponseThread {
    public WithoutDialogIdBlockThread(BlockingQueue<DcsResponseBody> responseBodyDeque,
                                      DcsResponseDispatcher.IDcsResponseHandler responseHandler,
                                      String threadName) {
        super(responseBodyDeque, responseHandler, threadName);
    }

    @Override
    boolean shouldBlock(DcsResponseBody responseBody) {
        return false;
    }
}
