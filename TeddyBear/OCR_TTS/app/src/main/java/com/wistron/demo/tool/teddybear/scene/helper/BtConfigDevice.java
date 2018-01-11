package com.wistron.demo.tool.teddybear.scene.helper;

import static android.R.attr.name;

/**
 * Created by king on 16-9-22.
 */

public class BtConfigDevice {
    private String userName;
    private String btMacAddress;
    private String emailAddress;

    public BtConfigDevice(String btMacAddress, String userName, String emailAddress) {
        this.btMacAddress = btMacAddress.trim();
        this.userName = userName.trim();
        this.emailAddress = emailAddress.trim().toLowerCase();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBtMacAddress() {
        return btMacAddress;
    }

    public void setBtMacAddress(String btMacAddress) {
        this.btMacAddress = btMacAddress;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String toString() {
        return "[" + name + ", " + btMacAddress + ", " + emailAddress + "]";
    }
}
