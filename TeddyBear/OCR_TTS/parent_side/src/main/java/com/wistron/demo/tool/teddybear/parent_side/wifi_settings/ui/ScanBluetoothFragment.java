package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatTextView;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.BTDeviceAdapter;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.data.ScannedDevice;

import java.util.ArrayList;

/**
 * Created by aaron on 16-9-9.
 */
public class ScanBluetoothFragment extends Fragment implements View.OnClickListener {
    private View mContentView;
    private ArrayList<FlatButton> flatButtons = new ArrayList<FlatButton>();
    private ArrayList<FlatTextView> flatTextViews = new ArrayList<FlatTextView>();
    private FlatButton mRefresh;
    private ProgressBar mProgressBar;
    private BTDeviceAdapter mDeviceAdapter;
    private ListView deviceListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(inflater);
        return mContentView;
    }

    public void setLoadingView(boolean enable) {
        mRefresh.setEnabled(!enable);
        deviceListView.setEnabled(!enable);
        if (enable) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    public void updateList(BluetoothDevice device) {
        mDeviceAdapter.update(device);
    }

    private void init(LayoutInflater inflater) {
        // converts the default values to dp to be compatible with different
        // screen sizes
        // FlatUI.initDefaultValues(getActivity());

        // Default theme should be set before content view is added
        // FlatUI.setDefaultTheme(FlatUI.GRAPE);

        mContentView = inflater.inflate(R.layout.scan_bt_fragment, null);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mContentView.setLayoutParams(p);

        //BT scan layout
        mRefresh = (FlatButton) mContentView.findViewById(R.id.scan_refresh);
        mRefresh.setOnClickListener(this);
        mProgressBar = (ProgressBar) mContentView.findViewById(R.id.scan_bt_loading);
        flatButtons.add((FlatButton) mContentView.findViewById(R.id.scan_refresh));
        flatTextViews.add((FlatTextView) mContentView.findViewById(R.id.scan_bt_title));

        deviceListView = (ListView) mContentView.findViewById(R.id.bt_list);
        mDeviceAdapter = new BTDeviceAdapter(getActivity(), R.layout.listitem_device,
                new ArrayList<ScannedDevice>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                ScannedDevice item = mDeviceAdapter.getItem(position);
                if (item != null) {
                    ((ScanBluetoothFragmentUI) getActivity()).connectBluetooth(item.getDevice());
                }
            }
        });

        setLoadingView(true);
    }

    @Override
    public void onClick(View v) {
        ((ScanBluetoothFragmentUI) getActivity()).reFreshBluetooth();
        setLoadingView(true);
        mDeviceAdapter.clear();
    }

    public void setTheme(int theme) {
        for (FlatButton view : flatButtons) {
            view.getAttributes().setTheme(theme, getResources());
        }
        for (FlatTextView view : flatTextViews) {
            view.getAttributes().setTheme(theme, getResources());
        }
    }

    public interface ScanBluetoothFragmentUI {
        void reFreshBluetooth();

        void connectBluetooth(BluetoothDevice device);
    }
}
