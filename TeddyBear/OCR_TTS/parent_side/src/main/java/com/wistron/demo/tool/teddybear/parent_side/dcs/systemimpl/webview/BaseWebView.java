package com.wistron.demo.tool.teddybear.parent_side.dcs.systemimpl.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.wistron.demo.tool.teddybear.parent_side.dcs.systeminterface.IWebView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ivanjlzhang on 17-9-19.
 */

public class BaseWebView extends WebView implements IWebView {
    private final List<IWebViewListener> webViewListeners =
            Collections.synchronizedList(new ArrayList<IWebViewListener>());

    public BaseWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseWebView(Context context) {
        super(context);

        init(context);
    }

    public BaseWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void init(Context context){
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        if (Build.VERSION.SDK_INT < 19) {
            removeJavascriptInterface("searchBoxJavaBridge_");
        }

        WebSettings localWebSettings = this.getSettings();
        try {
            // 禁用file协议,http://www.tuicool.com/articles/Q36ZfuF, 防止Android WebView File域攻击
            localWebSettings.setAllowFileAccess(false);
            localWebSettings.setSupportZoom(false);
            localWebSettings.setBuiltInZoomControls(false);
            localWebSettings.setUseWideViewPort(true);
            localWebSettings.setDomStorageEnabled(true);
            localWebSettings.setLoadWithOverviewMode(true);
            localWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            localWebSettings.setPluginState(WebSettings.PluginState.ON);
            // 启用数据库
            localWebSettings.setDatabaseEnabled(true);
            // 设置定位的数据库路径
            String dir = context.getApplicationContext().getDir("database", Context.MODE_PRIVATE).getPath();
            localWebSettings.setGeolocationDatabasePath(dir);
            localWebSettings.setGeolocationEnabled(true);
            localWebSettings.setJavaScriptEnabled(true);
            localWebSettings.setSavePassword(false);
            String agent = localWebSettings.getUserAgentString();

            localWebSettings.setUserAgentString(agent);
            // setCookie(context, ".baidu.com", bdussCookie);

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        this.setWebViewClient(new BridgeWebViewClient());
    }

    @Override
    public void setDownloadListener(DownloadListener listener) {
        super.setDownloadListener(listener);
    }

    private WebViewClientListener webViewClientListen;

    public void setWebViewClientListen(WebViewClientListener webViewClientListen) {
        this.webViewClientListen = webViewClientListen;
    }

    /**
     * 枚举网络加载返回状态 STATUS_FALSE:false
     * STATUS_TRUE:true
     * STATUS_UNKNOW:不知道
     * NET_UNKNOWN:未知网络
     */
    public enum LoadingWebStatus {
        STATUS_FALSE, STATUS_TRUE, STATUS_UNKNOW
    }
    @Override
    public void linkClicked(String url) {
        fireLinkClicked(url);
    }
    void fireLinkClicked(String url){
        for (IWebViewListener listener : webViewListeners){
            listener.onLinkClicked(url);
    }
}
    @Override
    public void addWebViewListener(IWebViewListener listener) {
        this.webViewListeners.add(listener);
    }
    public interface WebViewClientListener {
        LoadingWebStatus shouldOverrideUrlLoading(WebView view, String url);

        void onPageStarted(WebView view, String url, Bitmap favicon);

        void onPageFinished(WebView view, String url);

        void onReceivedError(WebView view, int errorCode, String description, String failingUrl);
    }
    public class BridgeWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LoadingWebStatus loadWebStatus = LoadingWebStatus.STATUS_UNKNOW;
            String mUrl = url;
            try {
                mUrl = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            if (null != webViewClientListen) {
                loadWebStatus = webViewClientListen.shouldOverrideUrlLoading(view, url);
            }
            if (LoadingWebStatus.STATUS_FALSE == loadWebStatus) {
                return false;
            } else if (LoadingWebStatus.STATUS_TRUE == loadWebStatus) {
                return true;
            } else {
                return super.shouldOverrideUrlLoading(view, url);
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (null != webViewClientListen) {
                webViewClientListen.onPageStarted(view, url, favicon);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (null != webViewClientListen) {
                webViewClientListen.onPageFinished(view, url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            if (null != webViewClientListen) {
                webViewClientListen.onReceivedError(view, errorCode, description, failingUrl);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            // 当发生证书认证错误时，采用默认的处理方法handler.cancel()，停止加载问题页面
            handler.cancel();
        }
    }
}
