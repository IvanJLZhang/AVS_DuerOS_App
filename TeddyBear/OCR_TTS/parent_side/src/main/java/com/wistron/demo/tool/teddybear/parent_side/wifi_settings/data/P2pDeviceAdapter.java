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

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * スキャンされたBLEデバイスリストのAdapter
 */
public class P2pDeviceAdapter extends ArrayAdapter<ScannedP2pDevice> {
    private static final String PREFIX_RSSI = "RSSI:";
    private List<ScannedP2pDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;

    public P2pDeviceAdapter(Context context, int resId, List<ScannedP2pDevice> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannedP2pDevice item = (ScannedP2pDevice) getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.device_name);
        name.setText(item.getDisplayName());
        TextView status = (TextView) convertView.findViewById(R.id.device_status);
        switch (item.getStatus()) {
            case ScannedP2pDevice.UNAVAILABLE:
                status.setText(R.string.status_unavailable);
                break;
            case ScannedP2pDevice.CONNECTED:
                status.setText(R.string.status_connected);
                break;
            case ScannedP2pDevice.FAILED:
                status.setText(R.string.status_failed);
                break;
            case ScannedP2pDevice.AVAILABLE:
                status.setText(R.string.status_available);
                break;
            case ScannedP2pDevice.INVITED:
                status.setText(R.string.status_invited);
                break;
        }

        return convertView;
    }

    /**
     * add or update BluetoothDevice
     */
    public void update(WifiP2pDeviceList newDevices) {
        mList.clear();
        Collection<WifiP2pDevice> collection = newDevices.getDeviceList();
        Iterator it = collection.iterator();
        while (it.hasNext()) {
            WifiP2pDevice device = (WifiP2pDevice) it.next();
            mList.add(new ScannedP2pDevice(device));
        }

        notifyDataSetChanged();
    }
}
