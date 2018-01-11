/*
 * Copyright (C) 2013 youten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data;

import android.net.wifi.p2p.WifiP2pDevice;

public class ScannedP2pDevice {
    public static final int CONNECTED = 0;
    public static final int INVITED = 1;
    public static final int FAILED = 2;
    public static final int AVAILABLE = 3;
    public static final int UNAVAILABLE = 4;
    private static final String UNKNOWN = "Unknown";
    private WifiP2pDevice mDevice;
    private String mDisplayName;
    private int mStatus = UNAVAILABLE;

    public ScannedP2pDevice(WifiP2pDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("wifiDevice is null");
        }
        mDevice = device;
        setDisplayName(device.deviceName);
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            setDisplayName(UNKNOWN);
        }
        setStatus(device.status);
    }

    public WifiP2pDevice getDevice() {
        return mDevice;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

}
