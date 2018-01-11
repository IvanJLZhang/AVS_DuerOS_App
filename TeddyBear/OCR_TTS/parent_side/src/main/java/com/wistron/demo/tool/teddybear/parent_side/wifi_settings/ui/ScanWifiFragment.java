package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.Fragment;
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
import com.wistron.demo.tool.teddybear.parent_side.view.FlatTextView;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedWifiDevice;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.WifiDeviceAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aaron on 16-9-12.
 */
public class ScanWifiFragment extends Fragment implements View.OnClickListener {

    private View mContentView;
    private ArrayList<FlatButton> flatButtons = new ArrayList<FlatButton>();
    private ArrayList<FlatTextView> flatTextViews = new ArrayList<FlatTextView>();
    private FlatButton mRefresh;
    private ListView deviceListView;
    private WifiDeviceAdapter mDeviceAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mRefresh.performClick();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(inflater);
        return mContentView;
    }

    private void init(LayoutInflater inflater) {
        // converts the default values to dp to be compatible with different
        // screen sizes
        // FlatUI.initDefaultValues(getActivity());

        // Default theme should be set before content view is added
        //  FlatUI.setDefaultTheme(FlatUI.GRAPE);

        mContentView = inflater.inflate(R.layout.scan_wifi_fragment, null);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mContentView.setLayoutParams(p);

        mRefresh = (FlatButton) mContentView.findViewById(R.id.scan_wifi_refresh);
        mRefresh.setOnClickListener(this);

        deviceListView = (ListView) mContentView.findViewById(R.id.wifi_list);
        mDeviceAdapter = new WifiDeviceAdapter(getActivity(), R.layout.listitem_wifi_device,
                new ArrayList<ScannedWifiDevice>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                ScannedWifiDevice item = mDeviceAdapter.getItem(position);
                if (item != null) {
                    ((ScanWifiFragmentUI) getActivity()).showConnectDialog(item);
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

    public void updateWifiList(List<ScannedWifiDevice> devices) {
        mDeviceAdapter.update(devices);
    }

    public void setTheme(int theme) {
        for (FlatButton view : flatButtons) {
            view.getAttributes().setTheme(theme, getResources());
        }
        for (FlatTextView view : flatTextViews) {
            view.getAttributes().setTheme(theme, getResources());
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mRefresh) {
            mDeviceAdapter.clear();
            ((ScanWifiFragmentUI) getActivity()).sendScanWifiCommand();
        }
    }

    public interface ScanWifiFragmentUI {
        void sendScanWifiCommand();

        void showConnectDialog(ScannedWifiDevice device);
    }
}
