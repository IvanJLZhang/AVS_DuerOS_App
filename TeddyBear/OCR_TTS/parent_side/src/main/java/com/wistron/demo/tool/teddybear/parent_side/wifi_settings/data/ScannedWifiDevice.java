package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data;

/**
 * Created by aaron on 16-9-12.
 */
public class ScannedWifiDevice {

    public static final int SECURITY_NONE = 1;
    public static final int SECURITY_WPA = 2;
    public static final int SECURITY_WEP = 3;
    private String mDisplayName;
    private int mLevel;
    private String mSecurity;

    public void setDisplayName(String name) {
        mDisplayName = name;
    }

    public void setLevel(int level) {
        mLevel = level;
    }

    public void setSecurity(String security) {
        mSecurity = security;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public int getLevel() {
        return mLevel;
    }

    public String getSecurity() {
        return mSecurity;
    }

    public int getSecurityMode() {
        if (mSecurity.contains("WPA") || mSecurity.contains("wpa")) {
            return SECURITY_WPA;

        } else if (mSecurity.contains("WEP") || mSecurity.contains("wep")) {
            return SECURITY_WEP;
        } else {
            return SECURITY_NONE;
        }
    }
}
