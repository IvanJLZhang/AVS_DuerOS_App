package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedWifiDevice;

/**
 * Created by aaron on 16-8-17.
 */
public class ConnectDialog extends Dialog implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static final int TIME_OUT = 30 * 1000; //ms
    private ScannedWifiDevice device;
    private Context mContext;
    private TextView mTitle;
    private CheckBox mShowPassword;
    private EditText mPassword;
    private Button mConnect, mCancel;
    private int mSecurityMode;
    private LinearLayout mLayout;
    private CharSequence tempPassword;
    private int MIN_PASSWORD = 8;

    public ConnectDialog(Context context, ScannedWifiDevice device) {
        super(context);
        mContext = context;
        this.device = device;

        init();
    }

    private void init() {
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog);

        mLayout = (LinearLayout) findViewById(R.id.dialog_pwd_Layout);
        mTitle = (TextView) findViewById(R.id.dialog_title);
        mShowPassword = (CheckBox) findViewById(R.id.dialog_show_password);
        mPassword = (EditText) findViewById(R.id.dialog_password);
        mConnect = (Button) findViewById(R.id.dialog_connect);
        mCancel = (Button) findViewById(R.id.dialog_cancel);
        mConnect.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        mShowPassword.setOnCheckedChangeListener(this);
        mPassword.addTextChangedListener(new PasswordTextChangedListener());
        mPassword.setOnKeyListener(onKey);
        setTitle();

        mSecurityMode = device.getSecurityMode();
        if (mSecurityMode == ScannedWifiDevice.SECURITY_NONE) {
            mLayout.setVisibility(View.GONE);
            mShowPassword.setVisibility(View.GONE);
            mConnect.setEnabled(true);
        } else {
            mLayout.setVisibility(View.VISIBLE);
            mShowPassword.setVisibility(View.VISIBLE);
        }
    }

    private void setTitle() {
        mTitle.setText(device.getDisplayName());
    }


    @Override
    public void onClick(View v) {
        if (v == mCancel) {
            ((ConnectDialogUI) mContext).onConnectDialogCancel();
        } else if (v == mConnect) {
            if (mSecurityMode == ScannedWifiDevice.SECURITY_NONE) {
                ((ConnectDialogUI) mContext).onConnectDialogOK(ScannedWifiDevice.SECURITY_NONE, device.getDisplayName(), "", TIME_OUT);
            } else {
                ((ConnectDialogUI) mContext).onConnectDialogOK(device.getSecurityMode(), device.getDisplayName(), mPassword.getText().toString(), TIME_OUT);
            }

        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        } else {
            mPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
    }

    private class PasswordTextChangedListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            tempPassword = s;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            mPassword.setSelection(tempPassword.length());
            if (tempPassword.length() >= MIN_PASSWORD) {
                mConnect.setEnabled(true);
            } else {
                mConnect.setEnabled(false);
            }
        }
    }

    View.OnKeyListener onKey = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm.isActive()) {

                    imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
                }
                return true;

            }
            return false;
        }
    };

    public interface ConnectDialogUI {
        void onConnectDialogOK(int securityMode, String ssid, String password, int timeout);

        void onConnectDialogCancel();
    }

}
