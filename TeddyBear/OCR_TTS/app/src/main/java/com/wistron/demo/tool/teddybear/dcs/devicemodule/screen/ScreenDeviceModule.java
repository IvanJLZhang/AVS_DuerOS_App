package com.wistron.demo.tool.teddybear.dcs.devicemodule.screen;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.message.HtmlPayload;
import com.wistron.demo.tool.teddybear.dcs.devicemodule.screen.message.LinkClickedPayload;
import com.wistron.demo.tool.teddybear.dcs.framework.BaseDeviceModule;
import com.wistron.demo.tool.teddybear.dcs.framework.IMessageSender;
import com.wistron.demo.tool.teddybear.dcs.framework.message.ClientContext;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Directive;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Event;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Header;
import com.wistron.demo.tool.teddybear.dcs.framework.message.MessageIdHeader;
import com.wistron.demo.tool.teddybear.dcs.framework.message.Payload;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IWebView;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class ScreenDeviceModule extends BaseDeviceModule {
    private final IWebView webView;

    public ScreenDeviceModule(IWebView webView, IMessageSender messageSender) {
        super(ApiConstants.NAMESPACE, messageSender);
        this.webView = webView;
        webView.addWebViewListener(new IWebView.IWebViewListener() {
            @Override
            public void onLinkClicked(String url) {
                sendLinkClickedEvent(url);
            }
        });
    }

    @Override
    public ClientContext clientContext() {
        return null;
    }

    @Override
    public void handleDirective(Directive directive) {
        String name = directive.header.getName();
        if (name.equals(ApiConstants.Directives.HtmlView.NAME)) {
            handleHtmlPayload(directive.getPayload());
        }
    }

    @Override
    public void release() {
    }

    private void handleHtmlPayload(Payload payload) {
        if (payload instanceof HtmlPayload) {
            HtmlPayload htmlPayload = (HtmlPayload) payload;
            webView.loadUrl(htmlPayload.getUrl());
        }
    }

    private void sendLinkClickedEvent(String url) {
        String name = ApiConstants.Events.LinkClicked.NAME;
        Header header = new MessageIdHeader(getNameSpace(), name);

        LinkClickedPayload linkClickedPayload = new LinkClickedPayload(url);
        Event event = new Event(header, linkClickedPayload);
        if (messageSender != null) {
            messageSender.sendEvent(event);
        }
    }
}
