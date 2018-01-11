package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.Fragment;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.P2pDeviceAdapter;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedP2pDevice;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.wifi.WifiController;

import java.util.ArrayList;

/**
 * Created by aaron on 16-9-28.
 */
public class ScanP2pFragment extends Fragment implements View.OnClickListener {

    private View mContentView;
    private FlatButton mRefresh;
    private WifiController mWifiController;
    private P2pDeviceAdapter mDeviceAdapter;
    private ListView deviceListView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(inflater);
        return mContentView;
    }

    private void init(LayoutInflater inflater) {
        mWifiController = new WifiController(getActivity());
        mContentView = inflater.inflate(R.layout.scan_p2p_fragment, null);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mContentView.setLayoutParams(p);

        mRefresh = (FlatButton) mContentView.findViewById(R.id.scan_p2p_refresh);
        mRefresh.setOnClickListener(this);

        deviceListView = (ListView) mContentView.findViewById(R.id.p2p_list);
        mDeviceAdapter = new P2pDeviceAdapter(getActivity(), R.layout.listitem_p2p_device,
                new ArrayList<ScannedP2pDevice>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                ScannedP2pDevice item = mDeviceAdapter.getItem(position);
                if (item != null) {
                    //connect p2p
                    int status = item.getStatus();
                    if (status == ScannedP2pDevice.AVAILABLE) {
                        ((ScanP2pFragmentUI) getActivity()).connectDevice(item.getDevice());
                    } else if (status == ScannedP2pDevice.INVITED) {
                        ((ScanP2pFragmentUI) getActivity()).cancelConnect();
                    }
                }
            }
        });
    }

    public void setScanButtonClickable(boolean able) {
        if (able) {
            mRefresh.setText(R.string.Refresh);
            mRefresh.setClickable(true);
        } else {
            mRefresh.setText(R.string.Refreshing);
            mRefresh.setClickable(false);
        }
    }

    public void updateList(WifiP2pDeviceList devices) {
        mDeviceAdapter.update(devices);
    }

    @Override
    public void onClick(View v) {
        if (mRefresh.equals(v)) {
            ((ScanP2pFragmentUI) getActivity()).cancelConnect();
            mDeviceAdapter.clear();
            ((ScanP2pFragmentUI) getActivity()).discoverPeers();
        }
    }

    public interface ScanP2pFragmentUI {
        void connectDevice(WifiP2pDevice device);

        void cancelConnect();

        void discoverPeers();
    }
}
