package com.wistron.demo.tool.teddybear.dcs.framework.dispatcher;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.voiceoutput.ApiConstants;
import com.wistron.demo.tool.teddybear.dcs.framework.DcsResponseDispatcher;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;

import java.util.concurrent.BlockingQueue;

/**
 * 服务器返回的指令header中有dialogRequestId调度线程
 * Created by ivanjlzhang on 17-9-22.
 */

public class WithDialogIdBlockThread extends BaseBlockResponseThread {

    public WithDialogIdBlockThread(BlockingQueue<DcsResponseBody> responseBodyDeque,
                                   DcsResponseDispatcher.IDcsResponseHandler responseHandler,
                                   String threadName) {
        super(responseBodyDeque, responseHandler, threadName);
    }

    @Override
    boolean shouldBlock(DcsResponseBody responseBody) {
        // 如果是speak指令就立马阻塞
        String directiveName = responseBody.getDirective().getName();
        return directiveName != null && directiveName.length() > 0
                && directiveName.equals(ApiConstants.Directives.Speak.NAME);
    }
}
