package com.wistron.demo.tool.teddybear.scene.ssr_helper;

import android.util.Log;


public class ConfigData {
    private static final String TAG = "SourceTrackingConfig";
    private int mBeam;
    private int mGainStep;
    private long mLastUpdated;
    private boolean mMute;
    private int mNSLevel;
    private int mRotation;
    private boolean mWNREnbled;
    private long pollConfigPeriod;
    private ConfigValue[] values;

    class ConfigValue implements Comparable<ConfigValue> {
        int enabled;
        int startAngle;

        public ConfigValue(int angle, int en) {
            this.startAngle = 0;
            this.enabled = 0;
            this.startAngle = angle;
            this.enabled = en;
        }

        public int compareTo(ConfigValue other) {
            if (this == other) {
                return 0;
            }
            if (this.startAngle < other.startAngle) {
                return -1;
            }
            if (this.startAngle > other.startAngle) {
                return 1;
            }
            return 0;
        }
    }

    public ConfigData() {
        this.values = new ConfigValue[4];
        this.mLastUpdated = 0;
        this.mGainStep = 0;
        this.pollConfigPeriod = SSRConsts.DEFAULT_POLL_CONFIG_PERIOD_MS;
        this.mWNREnbled = true;
        this.mRotation = 1;
        this.mMute = false;
        this.mNSLevel = 0;
        for (int i = 0; i < getData().length; i++) {
            this.values[i] = new ConfigValue(0, 0);
        }
    }

    public ConfigData(int[] sectorAngles, int[] sectorEnabled, boolean wnr, int beam) {
        this.values = new ConfigValue[4];
        this.mLastUpdated = 0;
        this.mGainStep = 0;
        this.pollConfigPeriod = SSRConsts.DEFAULT_POLL_CONFIG_PERIOD_MS;
        this.mWNREnbled = true;
        this.mRotation = 1;
        this.mMute = false;
        this.mNSLevel = 0;
        for (int i = 0; i < getData().length; i++) {
            this.values[i] = new ConfigValue(sectorAngles[i], sectorEnabled[i]);
        }
        this.mWNREnbled = wnr;
        this.mBeam = beam;
    }

    public ConfigData(int[] sectorAngles, int[] sectorEnabled, int wnr, int beam) {
        boolean z = false;
        this.values = new ConfigValue[4];
        this.mLastUpdated = 0;
        this.mGainStep = 0;
        this.pollConfigPeriod = SSRConsts.DEFAULT_POLL_CONFIG_PERIOD_MS;
        this.mWNREnbled = true;
        this.mRotation = 1;
        this.mMute = false;
        this.mNSLevel = 0;
        for (int i = 0; i < getData().length; i++) {
            this.values[i] = new ConfigValue(sectorAngles[i], sectorEnabled[i]);
        }
        if (wnr != 0) {
            z = true;
        }
        this.mWNREnbled = z;
        this.mBeam = beam;
    }

    public void update(int[] sectorAngles, int[] sectorEnabled) {
        for (int i = 0; i < getData().length; i++) {
            this.values[i].startAngle = sectorAngles[i];
            this.values[i].enabled = sectorEnabled[i];
        }
        this.mLastUpdated = System.currentTimeMillis();
    }

    public void update(int[] sectorAngles, int[] sectorEnabled, int wnr, int beam) {
        for (int i = 0; i < getData().length; i++) {
            this.values[i].startAngle = sectorAngles[i];
            this.values[i].enabled = sectorEnabled[i];
        }
        this.mLastUpdated = System.currentTimeMillis();
        this.mWNREnbled = wnr != 0;
        this.mBeam = beam;
    }

    public void update(ConfigData newConfig) {
        if (newConfig != null) {
            Log.d(TAG, "User Config is");
            printLog(TAG);
            Log.d(TAG, "New Config is ");
            newConfig.printLog(TAG);
            for (int i = 0; i < getData().length; i++) {
                this.values[i].startAngle = newConfig.values[i].startAngle;
                this.values[i].enabled = newConfig.values[i].enabled;
            }
            this.mGainStep = newConfig.mGainStep;
            this.mWNREnbled = newConfig.mWNREnbled;
            this.mBeam = newConfig.mBeam;
            this.mLastUpdated = System.currentTimeMillis();
        }
    }

    public void setPollConfigPeriod(long period) {
        this.pollConfigPeriod = period;
    }

    public boolean isStale() {
        return System.currentTimeMillis() - this.mLastUpdated > this.pollConfigPeriod;
    }

    public void setSectorEnabled(int[] sectorEnabled) {
        for (int i = 0; i < this.values.length; i++) {
            this.values[i].enabled = sectorEnabled[i];
        }
    }

    public void setStartAngles(int[] sectorAngles) {
        for (int i = 0; i < this.values.length; i++) {
            this.values[i].startAngle = sectorAngles[i];
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigData)) {
            return false;
        }
        ConfigData lhs = (ConfigData) o;
        int i = 0;
        while (i < getData().length) {
            if (this.values[i].enabled == lhs.values[i].enabled && this.values[i].startAngle == lhs.values[i].startAngle) {
                i++;
            } else {
                Log.d(TAG, "Did not match at " + i + "; enabled=" + this.values[i].enabled + " and " + lhs.values[i].enabled + "; values " + this.values[i].startAngle + " and " + lhs.values[i].startAngle);
                return false;
            }
        }
        return true;
    }

    public void printLog(String tag) {
        for (int i = 0; i < getData().length; i++) {
            Log.d(TAG, "Sector " + i + ": enabled=" + getData()[i].enabled + ", minAngle=" + getData()[i].startAngle);
        }
    }

    public void printLog() {
        printLog(TAG);
    }

    public void setGainStep(int gainStep) {
        this.mGainStep = gainStep;
    }

    public int[] getStartAngles() {
        int[] returnValue = new int[this.values.length];
        for (int i = 0; i < getData().length; i++) {
            returnValue[i] = this.values[i].startAngle;
        }
        return returnValue;
    }

    public int[] getEnabled() {
        int[] returnValue = new int[getData().length];
        for (int i = 0; i < getData().length; i++) {
            returnValue[i] = getData()[i].enabled;
        }
        return returnValue;
    }

    public ConfigValue[] getData() {
        return this.values;
    }

    public ConfigValue getData(int index) {
        if (index < 0 || index >= this.values.length) {
            return null;
        }
        return this.values[index];
    }

    public void setData(ConfigValue[] value) {
        this.values = value;
    }

    public void setData(ConfigValue newValue, int index) {
        if (index >= 0 && index < this.values.length) {
            this.values[index] = newValue;
        }
    }

    public int getGainStep() {
        return this.mGainStep;
    }

    public void setWNREnabled(boolean enabled) {
        this.mWNREnbled = enabled;
    }

    public boolean isWNREnabled() {
        return this.mWNREnbled;
    }

    public void setBeam(int scale) {
        this.mBeam = scale;
    }

    public int getBeam() {
        return this.mBeam;
    }

    public void setOrientation(int rotation) {
        this.mRotation = rotation;
    }

    public int getOrientation() {
        return this.mRotation;
    }

    public void setMute(boolean isChecked) {
        this.mMute = isChecked;
    }

    public boolean getMute() {
        return this.mMute;
    }

    public void setNSLevel(int nslevel) {
        this.mNSLevel = nslevel;
    }

    public int getNSLevel() {
        return this.mNSLevel;
    }
}
