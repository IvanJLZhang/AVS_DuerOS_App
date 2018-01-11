package com.wistron.demo.tool.teddybear.scene.ssr_helper;

/**
 * Created by king on 16-9-12.
 */

public class VoiceAngleRange {
    private int angleRange; // 0~11
    private int validAngleNum;
    private float angleSum;

    public VoiceAngleRange(int angleRange) {
        this.angleRange = angleRange;
    }

    public int getValidAngleNum() {
        return validAngleNum;
    }

    public void setValidAngleNum(int validAngleNum) {
        this.validAngleNum = validAngleNum;
    }

    public float getAngleSum() {
        return angleSum;
    }

    public void setAngleSum(float angleSum) {
        this.angleSum = angleSum;
    }
}
