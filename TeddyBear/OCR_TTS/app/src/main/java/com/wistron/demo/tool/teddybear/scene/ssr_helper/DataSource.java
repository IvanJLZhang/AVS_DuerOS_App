package com.wistron.demo.tool.teddybear.scene.ssr_helper;


import java.util.Arrays;

public class DataSource {
    private final String LOG_TAG;
    public float[] data;
    public int[] interfererAngle;
    private int mCurrentIndex;
    private float mCurrentValueFactor;
    private int mNoiseLevel;
    private int mNoiseLevelAfter;
    private float[] mRawData;
    private float mSmoothingFactor;
    public int targetAngle;
    public int[] vad;

    public DataSource() {
        this.data = new float[SSRConsts.CONFIG_MAX_SECTOR_ANGLE];
        this.targetAngle = 0;
        this.interfererAngle = new int[3];
        this.vad = new int[4];
        this.mSmoothingFactor = 0.0f;
        this.mCurrentValueFactor = 0.0f;
        this.mCurrentIndex = 0;
        this.LOG_TAG = "DataSource";
        this.mRawData = new float[SSRConsts.CONFIG_MAX_SECTOR_ANGLE];
        this.mNoiseLevel = 0;
        this.mNoiseLevelAfter = 0;
        Arrays.fill(this.data, 0.0f);
    }

    public static DataSource obtain(DataSource src) {
        try {
            DataSource dst = new DataSource();
            System.arraycopy(src.data, 0, dst.data, 0, src.data.length);
            System.arraycopy(src.vad, 0, dst.vad, 0, src.vad.length);
            System.arraycopy(src.interfererAngle, 0, dst.interfererAngle, 0, src.interfererAngle.length);
            dst.targetAngle = src.targetAngle;
            return dst;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setSmoothingFactor(int smoothingValue) {
        this.mSmoothingFactor = ((float) smoothingValue) / 100.0f;
        this.mCurrentValueFactor = 1.0f - this.mSmoothingFactor;
    }

    public void add(float value) {
        value = Math.min(value, 100.0f);
        if (this.mCurrentIndex < 0 || this.mCurrentIndex >= SSRConsts.CONFIG_MAX_SECTOR_ANGLE) {
            this.mCurrentIndex = 0;
        }
        this.mRawData[this.mCurrentIndex] = value;
        float[] fArr = this.data;
        int i = this.mCurrentIndex;
        if (this.data[this.mCurrentIndex] > 0.0f) {
            value = (this.mSmoothingFactor * this.data[this.mCurrentIndex]) + (this.mCurrentValueFactor * value);
        }
        fArr[i] = value;
        this.mCurrentIndex++;
    }

    public float[] getRawData() {
        return this.mRawData;
    }

    public void setNoiseLevel(int round) {
        this.mNoiseLevel = round;
    }

    public int getNoiseLevel() {
        return this.mNoiseLevel;
    }

    public void setNoiseLevelAfter(int round) {
        this.mNoiseLevelAfter = round;
    }

    public int getNoiseLevelAfter() {
        return this.mNoiseLevelAfter;
    }
}
