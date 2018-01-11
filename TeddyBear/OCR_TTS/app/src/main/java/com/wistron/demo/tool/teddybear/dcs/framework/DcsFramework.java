package com.wistron.demo.tool.teddybear.dcs.framework;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.HandleDirectiveException;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.system.SystemDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.framework.DcsResponseDispatcher.IDcsResponseHandler;
import com.wistron.demo.tool.teddybear.dcs.framework.DeviceModuleFactory.IDeviceModuleHandler;
import com.wistron.demo.tool.teddybear.dcs.framework.message.ClientContext;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsResponseBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.DcsStreamRequestBody;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Event;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IPlatformFactory;
import com.wistron.demo.tool.teddybear.dcs.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * dcs 业务处理框架(directives)， 用于分发指令(events)， 事件和创建deviceModule等
 * Created by ivanjlzhang on 17-9-21.
 */

public class DcsFramework {
    private static final String TAG = DcsFramework.class.getSimpleName();
    // 管理平台相关的对象
    private final IPlatformFactory platformFactory;
    // 管理deviceModules
    private final HashMap<String, BaseDeviceModule> dispatchDeviceModules;
    // 创建会话Id
    private final DialogRequestIdHandler dialogRequestIdHandler;
    // 基于通道活跃状态和优先级进行mediaPlayer调度
    private final BaseMultiChannelMediaPlayer multiChannelMediaPlayer;
    // 创建deviceModule工厂
    private DeviceModuleFactory deviceModuleFactory;
    // 和服务器端保持长连接、发送events和接收directives和维持心跳
    private DcsClient dcsClient;
    // 用于DeviceModules发送events
    private IMessageSender messageSender;
    // 服务器端返回response调度中心
    private DcsResponseDispatcher dcsResponseDispatcher;

    public DcsFramework(IPlatformFactory platformFactory) {
        this.platformFactory = platformFactory;
        dispatchDeviceModules = new HashMap<>();
        dialogRequestIdHandler = new DialogRequestIdHandler();
        multiChannelMediaPlayer = new PauseStrategyMultiChannelMediaPlayer(platformFactory);

        createMessageSender();
        createDcsClient();
        createDeviceModuleFactory();
    }

    public void release() {
        for (BaseDeviceModule deviceModule : dispatchDeviceModules.values()) {
            deviceModule.release();
        }
        dcsClient.release();
        dcsResponseDispatcher.release();
    }

    public DeviceModuleFactory getDeviceModuleFactory() {
        return deviceModuleFactory;
    }

    private ArrayList<ClientContext> clientContexts() {
        ArrayList<ClientContext> clientContexts = new ArrayList<>();
        for (BaseDeviceModule deviceModule : dispatchDeviceModules.values()) {
            ClientContext clientContext = deviceModule.clientContext();
            if (clientContext != null) {
                clientContexts.add(clientContext);
            }
        }

        return clientContexts;
    }

    private void handleDirective(Directive directive) {
        String namespace = directive.header.getNamespace();
        try {
            BaseDeviceModule deviceModule = dispatchDeviceModules.get(namespace);
            if (deviceModule != null) {
                deviceModule.handleDirective(directive);
            } else {
                String message = "No device to handle the directive";
                throw new HandleDirectiveException(
                        HandleDirectiveException.ExceptionType.UNSUPPORTED_OPERATION, message);
            }
        } catch (HandleDirectiveException exception) {
            getSystemDeviceModule().sendExceptionEncounteredEvent(directive.rawMessage,
                    exception.getExceptionType(), exception.getMessage());
        } catch (Exception exception) {
            getSystemDeviceModule().sendExceptionEncounteredEvent(directive.rawMessage,
                    HandleDirectiveException.ExceptionType.INTERNAL_ERROR,
                    exception.getMessage()
            );
        }
    }

    private void createDcsClient() {
        IDcsResponseHandler responseHandler = new IDcsResponseHandler() {
            @Override
            public void onResponse(final DcsResponseBody responseBody) {
                platformFactory.getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtil.d(TAG, "DcsResponseBodyEnqueue-handleDirective-MSG:"
                                + responseBody.getDirective().rawMessage);
                        handleDirective(responseBody.getDirective());
                    }
                });
            }

            @Override
            public void onParseFailed(String unParsedMessage) {
                LogUtil.d(TAG, "DcsResponseBodyEnqueue-handleDirective-onParseFailed");
                String message = "parse failed";
                getSystemDeviceModule().sendExceptionEncounteredEvent(unParsedMessage,
                        HandleDirectiveException.ExceptionType.UNEXPECTED_INFORMATION_RECEIVED,
                        message);
            }
        };

        dcsResponseDispatcher = new DcsResponseDispatcher(dialogRequestIdHandler, responseHandler);
        dcsClient = new DcsClient(dcsResponseDispatcher, new DcsClient.IDcsClientListener() {
            @Override
            public void onConnected() {
                LogUtil.d(TAG, "onConnected");
                getSystemDeviceModule().sendSynchronizeStateEvent();
            }

            @Override
            public void onUnconnected() {
                LogUtil.d(TAG, "onUnconnected");
            }
        });
        dcsClient.startConnect();
    }

    private void createMessageSender() {

        messageSender = new IMessageSender() {
            @Override
            public void sendEvent(Event event) {
                sendEventRequest(event, null, null);
            }

            @Override
            public void sendEvent(Event event, IResponseListener responseListener) {
                sendEventRequest(event, null, responseListener);
            }

            @Override
            public void sendEvent(Event event, DcsStreamRequestBody streamRequestBody, IResponseListener responseListener) {
                DcsRequestBody requestBody = new DcsRequestBody(event);
                requestBody.setClientContext(clientContexts());
                dcsClient.sendRequest(requestBody, streamRequestBody, responseListener);
            }

            @Override
            public void sentEventWithClientContext(Event event, IResponseListener responseListener) {
                sendEventRequest(event, clientContexts(), responseListener);

            }
        };
    }

    private void sendEventRequest(Event event,
                                  ArrayList<ClientContext> clientContexts,
                                  IResponseListener responseListener) {
        DcsRequestBody dcsRequestBody = new DcsRequestBody(event);
        dcsRequestBody.setClientContext(clientContexts);
        dcsClient.sendRequest(dcsRequestBody, responseListener);
    }

    private void createDeviceModuleFactory() {
        deviceModuleFactory = new DeviceModuleFactory(new IDeviceModuleHandler() {
            @Override
            public IPlatformFactory getPlatformFactory() {
                return platformFactory;
            }

            @Override
            public DialogRequestIdHandler getDialogRequestIdHandler() {
                return dialogRequestIdHandler;
            }

            @Override
            public IMessageSender getMessageSender() {
                return messageSender;
            }

            @Override
            public BaseMultiChannelMediaPlayer getMultiChannelMediaPlayer() {
                return multiChannelMediaPlayer;
            }

            @Override
            public void addDeviceModule(BaseDeviceModule deviceModule) {
                DcsFramework.this.addDeviceModule(deviceModule);
            }

            @Override
            public DcsResponseDispatcher getResponseDispatcher() {
                return dcsResponseDispatcher;
            }
        });
    }

    private void addDeviceModule(BaseDeviceModule deviceModule) {
        dispatchDeviceModules.put(deviceModule.getNameSpace(), deviceModule);
    }

    private SystemDeviceModule getSystemDeviceModule() {
        return deviceModuleFactory.getSystemDeviceModule();
    }
}
