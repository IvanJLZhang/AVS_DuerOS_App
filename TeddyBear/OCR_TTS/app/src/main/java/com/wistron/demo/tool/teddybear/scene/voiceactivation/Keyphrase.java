package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.content.Intent;

import java.util.ArrayList;

public class Keyphrase
        implements Comparable<Keyphrase> {
    private Intent actionIntent;
    private String actionName;
    int confidenceLevel = SVAGlobal.getInstance().getSettingKeyPhraseConfidenceLevel();
    int id;
    private boolean isUdk = false;
    private boolean isUserVerificationEnabled;
    boolean launchGoogleNow = false;
    String name;
    private ArrayList<User> users = new ArrayList();

    Keyphrase(String paramString, int paramInt) {
        this.name = paramString;
        this.id = paramInt;
    }

    public Keyphrase(String paramString, int paramInt, boolean paramBoolean) {
        this.name = paramString;
        this.confidenceLevel = paramInt;
        this.launchGoogleNow = paramBoolean;
    }

    Keyphrase(String paramString1, String paramString2, Intent paramIntent, boolean paramBoolean) {
        this.name = paramString1;
        this.actionName = paramString2;
        this.actionIntent = paramIntent;
        this.isUserVerificationEnabled = paramBoolean;
        this.id = 0;
    }

    public void addUser(String paramString, int paramInt) {
        this.users.add(new User(paramString, paramInt));
    }

    public int compareTo(Keyphrase paramKeyphrase) {
        return this.name.compareToIgnoreCase(paramKeyphrase.name);
    }

    public boolean equals(Object paramObject) {
        if (this == paramObject)
            return true;
        if (paramObject == null)
            return false;
        if (getClass() != paramObject.getClass())
            return false;
        if (this.name != null) {
            if (this.name.equals(((Keyphrase) paramObject).name))
                return false;
        } else {
            if (((Keyphrase) paramObject).name == null)
                return false;
        }
        return true;
    }

    public Intent getActionIntent() {
        return this.actionIntent;
    }

    public String getActionName() {
        return this.actionName;
    }

    public int getConfidenceLevel() {
        return this.confidenceLevel;
    }

    public boolean getIsUdk() {
        return this.isUdk;
    }

    public boolean getIsUserVerificationEnabled() {
        return this.isUserVerificationEnabled;
    }

    public boolean getLaunch() {
        return this.launchGoogleNow;
    }

    public String getName() {
        return this.name;
    }

    public ArrayList<User> getUsers() {
        return this.users;
    }

    public int hashCode() {
        int i = 0;
        if (this.name != null) {
            i = this.name.hashCode();
        }
        return i + 31;
    }

    public void setActionIntent(Intent paramIntent) {
        this.actionIntent = paramIntent;
    }

    public void setActionName(String paramString) {
        this.actionName = paramString;
    }

    public void setConfidenceLevel(int paramInt) {
        this.confidenceLevel = paramInt;
    }

    public void setIsUdk(boolean paramBoolean) {
        this.isUdk = paramBoolean;
    }

    public void setIsUserVerificationEnabled(boolean paramBoolean) {
        this.isUserVerificationEnabled = paramBoolean;
    }

    public void toggleLaunch() {
        launchGoogleNow = !launchGoogleNow;
    }
}