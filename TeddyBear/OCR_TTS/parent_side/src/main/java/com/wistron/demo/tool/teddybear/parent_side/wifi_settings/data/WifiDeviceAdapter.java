package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by aaron on 16-9-12.
 */
public class WifiDeviceAdapter extends ArrayAdapter<ScannedWifiDevice> {

    private static final int LEVEL_0 = 0;
    private static final int LEVEL_1 = 1;
    private static final int LEVEL_2 = 2;
    private static final int LEVEL_3 = 3;
    private static final int LEVEL_4 = 4;
    private List<ScannedWifiDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;

    public WifiDeviceAdapter(Context context, int resource, List<ScannedWifiDevice> objects) {
        super(context, resource, objects);
        mResId = resource;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ScannedWifiDevice item = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.device_wifi_name);
        name.setText(item.getDisplayName());
        ImageView icon = (ImageView) convertView.findViewById(R.id.device_wifi_icon);
        int securityMode = item.getSecurityMode();
        int level = calculateLevel(item);
        if (securityMode == ScannedWifiDevice.SECURITY_NONE) {
            switch (level) {
                case LEVEL_0:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_0_dark);
                    break;
                case LEVEL_1:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_1_dark);
                    break;
                case LEVEL_2:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_2_dark);
                    break;
                case LEVEL_3:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_3_dark);
                    break;
                case LEVEL_4:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_4_dark);
                    break;
                default:
                    LEVEL_0:
                    icon.setImageResource(R.mipmap.ic_wifi_signal_0_dark);
                    break;
            }
        } else {
            switch (level) {
                case LEVEL_0:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_0_dark);
                    break;
                case LEVEL_1:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_1_dark);
                    break;
                case LEVEL_2:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_2_dark);
                    break;
                case LEVEL_3:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_3_dark);
                    break;
                case LEVEL_4:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_4_dark);
                    break;
                default:
                    LEVEL_0:
                    icon.setImageResource(R.mipmap.ic_wifi_lock_signal_0_dark);
                    break;
            }
        }
        return convertView;
    }

    public void update(List<ScannedWifiDevice> devices) {
        mList.clear();
        for (ScannedWifiDevice device : devices) {
            mList.add(device);
        }
        sortLevel();
        notifyDataSetChanged();
    }

    /**
     * Sort list with signal level
     */
    private void sortLevel() {
        Comparator<ScannedWifiDevice> itemComparator = new Comparator<ScannedWifiDevice>() {
            @Override
            public int compare(ScannedWifiDevice lhs, ScannedWifiDevice rhs) {
                if (lhs.getLevel() > rhs.getLevel()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        };
        Collections.sort(mList, itemComparator);
    }

    private int calculateLevel(ScannedWifiDevice device) {
        int signal = WifiManager.calculateSignalLevel(device.getLevel(), 100);
        if (signal <= 100 && signal > 80) {
            return LEVEL_4;
        } else if (signal <= 80 && signal > 60) {
            return LEVEL_3;
        } else if (signal <= 60 && signal > 40) {
            return LEVEL_2;
        } else if (signal <= 40 && signal > 20) {
            return LEVEL_1;
        } else {
            return LEVEL_0;
        }
    }
}
