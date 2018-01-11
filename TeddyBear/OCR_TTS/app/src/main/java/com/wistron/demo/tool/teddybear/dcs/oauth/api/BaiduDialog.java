package com.wistron.demo.tool.teddybear.dcs.oauth.api;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.wistron.demo.tool.teddybear.dcs.systemimpl.webview.BaseWebView;

/**
 * 自定义的Dialog UI类，用来展示WebView界面，即用户登录和授权的页面
 * Created by ivanjlzhang on 17-9-19.
 */

public class BaiduDialog extends Dialog {
    private static final FrameLayout.LayoutParams MATCH = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT);
    private static final String TAG = "BaiduDialog";
    private final String mUrl;
    private final BaiduDialogListener mListener;
    private ProgressDialog mSpinner;
    private BaseWebView mWebView;
    private FrameLayout mContent;
    private RelativeLayout webViewContainer;

    public BaiduDialog(Context context, String url, BaiduDialogListener listener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置ProgressDialog的样式
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("login...");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mContent = new FrameLayout(getContext());
        setUpWebView();
        addContentView(mContent, MATCH);
    }

    private void setUpWebView(){
        webViewContainer = new RelativeLayout(getContext());
        mWebView = new BaseWebView(getContext().getApplicationContext());
        mWebView.setWebViewClient(new BdWebViewClient());
        mWebView.loadUrl(mUrl);
        mWebView.setLayoutParams(MATCH);
        mWebView.setVisibility(View.INVISIBLE);
        webViewContainer.addView(mWebView);
        mContent.addView(webViewContainer, MATCH);
    }
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        mListener.onCancel();
        return super.onKeyDown(keyCode, event);
    }


    private class BdWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "Redirect URL: " + url);
            /*
             * 如果url的地址为bdconnect://success，即使用User-Agent方式获取用户授权的redirct
             * url,则截取url中返回的各种token参数，
             * 如果出错，则通过listener的相应处理方式回调
             */
            if (url.startsWith(BaiduOauthImplictionGrant.SUCCESS_URI)) {
                Bundle values = OauthNetUtil.parseUrl(url);
                if (values != null && !values.isEmpty()) {
                    String error = values.getString("error");
                    // 用户取消授权返回error=access_denied
                    if ("access_denied".equals(error)) {
                        mListener.onCancel();
                        BaiduDialog.this.dismiss();
                        return true;
                    }
                    // 请求出错时返回error=1100&errorDesp=error_desp
                    String errorDesp = values.getString("error_description");
                    if (error != null && errorDesp != null) {
                        mListener.onBaiduException(new BaiduException(error, errorDesp));
                        BaiduDialog.this.dismiss();
                        return true;
                    }
                    mListener.onComplete(values);
                    BaiduDialog.this.dismiss();
                    return true;
                }
            } else if (url.startsWith(BaiduOauthImplictionGrant.CANCEL_URI)) {
                mListener.onCancel();
                BaiduDialog.this.dismiss();
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            mListener.onError(new BaiduDialogError(description, errorCode, failingUrl));
            BaiduDialog.this.dismiss();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG, "Webview loading URL: " + url);
            super.onPageStarted(view, url, favicon);
            mSpinner.show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mSpinner.dismiss();
            mContent.setBackgroundColor(Color.TRANSPARENT);
            mWebView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mSpinner.dismiss();
        webViewContainer.removeView(mWebView);
        mWebView.removeAllViews();
        mWebView.destroy();

    }

    /**
     * 用于BaiduDialog回调接口
     */
    public interface BaiduDialogListener {
        /**
         * BaiduDailog请求成功时，执行该法方法，实现逻辑包括存储value信息，跳转activity的过程
         *
         * @param values token信息的key-value存储
         */
        void onComplete(Bundle values);

        /**
         * 当BaiduDialog执行发生BaiduException时执行的方法。
         *
         * @param e BaiduException信息
         */
        void onBaiduException(BaiduException e);

        /**
         * 发生DialogError时执行的方法
         *
         * @param e BaiduDialogError异常类
         */
        void onError(BaiduDialogError e);

        /**
         * 当BaiduDialog执行取消操作时，执行该方法
         */
        void onCancel();
    }
}
