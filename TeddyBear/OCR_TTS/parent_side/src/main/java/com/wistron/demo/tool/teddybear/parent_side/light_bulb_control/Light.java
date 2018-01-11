package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

/**
 * Created by aaron on 17-2-14.
 */

public class Light {

    private static final int BASE = 0;
    public static final int POWER_ON = 1;
    public static final int POWER_OFF = 0;
    public static final int CMD_LIGHT_ON = BASE + 1;    //power on one light
    public static final int CMD_LIGHT_OFF = BASE + 2;   //power off one light
    public static final int CMD_LIGHTS_ON = BASE + 3;   //power on many lights
    public static final int CMD_LIGHTS_OFF = BASE + 4;  //power off many lights
    public static final int CMD_GET_LIGHTS = BASE + 5;      //get light list from server
    public static final int CMD_LIGHT_CALL_BACK = BASE + 6; //get one light power on/off result from server
    public static final int CMD_LIGHTS_CALL_BACK = BASE + 7;    //get mangy lisghts power on/off result from server

    private int mState = 0;
    private int id;
    private String name;
    private String type;

    public Light() {
    }

    public void setState(int state) {
        mState = state;
    }

    public int getState() {
        return mState;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
