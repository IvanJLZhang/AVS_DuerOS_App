package com.wistron.demo.tool.teddybear.parent_side.dcs.systeminterface;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public interface IWebView {
    void loadUrl(String url);
    void linkClicked(String url);

    void addWebViewListener(IWebViewListener listener);

    interface IWebViewListener {
        void onLinkClicked(String url);
    }
}
