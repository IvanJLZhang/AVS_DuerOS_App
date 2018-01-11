package com.wistron.demo.tool.teddybear.parent_side;

import static android.R.attr.name;

/**
 * Created by king on 16-9-22.
 */

public class BtConfigDevice {
    private String userName;
    private String btMacAddress;
    private String emailAddress;
    private String btName;

    public void setBtName(String btName) {
        this.btName = btName;
    }

    public String getBtName() {

        return btName;
    }

    public BtConfigDevice(String btMacAddress, String userName, String emailAddress, String btName) {
        this.btMacAddress = btMacAddress.trim();
        this.userName = userName.trim();
        this.emailAddress = emailAddress.trim().toLowerCase();
        this.btName = btName;
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
