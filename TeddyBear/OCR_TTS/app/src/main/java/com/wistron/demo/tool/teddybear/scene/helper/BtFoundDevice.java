package com.wistron.demo.tool.teddybear.scene.helper;

/**
 * Created by king on 16-9-22.
 */

public class BtFoundDevice {
    private String name;
    private String macAddress;
    private short rssi;

    public BtFoundDevice(String name, String macAddress, short rssi) {
        this.name = name;
        this.macAddress = macAddress;
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public short getRssi() {
        return rssi;
    }

    public void setRssi(short rssi) {
        this.rssi = rssi;
    }

    @Override
    public String toString() {
        return "[" + name + ", " + macAddress + ", " + rssi + "]";
    }
}
