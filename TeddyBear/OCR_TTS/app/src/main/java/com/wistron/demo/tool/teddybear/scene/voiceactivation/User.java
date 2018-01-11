package com.wistron.demo.tool.teddybear.scene.voiceactivation;

public class User
        implements Comparable<User> {
    int confidenceLevel = SVAGlobal.getInstance().getSettingUserConfidenceLevel();
    int id;
    boolean launchGoogleNow = false;
    String name;

    User(String paramString, int paramInt) {
        this.name = paramString;
        this.id = paramInt;
    }

    User(String paramString, int paramInt, boolean paramBoolean) {
        this.name = paramString;
        this.confidenceLevel = paramInt;
        this.launchGoogleNow = paramBoolean;
    }

    public int compareTo(User paramUser) {
        return this.name.compareToIgnoreCase(paramUser.name);
    }

    public boolean equals(SoundModel soundModel) {
        boolean bool1 = false;
        if (soundModel != null) {
            bool1 = this.name.equals(soundModel.getName());
        }
        return bool1;
    }

    public int getConfidenceLevel() {
        return this.confidenceLevel;
    }

    public boolean getLaunch() {
        return this.launchGoogleNow;
    }

    public String getName() {
        return this.name;
    }

    public void setConfidenceLevel(int paramInt) {
        this.confidenceLevel = paramInt;
    }

    public void toggleLaunch() {
        launchGoogleNow = !launchGoogleNow;
    }
}