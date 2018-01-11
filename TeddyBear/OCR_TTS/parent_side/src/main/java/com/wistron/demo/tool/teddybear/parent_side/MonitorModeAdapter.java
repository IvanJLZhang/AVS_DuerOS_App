package com.wistron.demo.tool.teddybear.parent_side;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Time：16-5-3 18:30
 * Author：bob
 */
public class MonitorModeAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {
    private LayoutInflater mInflater;
    private ArrayList<MonitorModeActivity.TeddyBear> list;
    private Context context;

    public MonitorModeAdapter(Context context, ArrayList<MonitorModeActivity.TeddyBear> list
            , Callback callback) {
        this.mInflater = LayoutInflater.from(context);
        this.list = list;
        this.context = context;
        this.mCallback = callback;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.monitor_mode_list_item, null);
            holder.tvName = (TextView) convertView.findViewById(R.id.child_item_name);
            holder.tvWarningLogInfo = (TextView) convertView.findViewById(R.id.child_item_warning_log_info);
            holder.aSwitch = (Switch) convertView.findViewById(R.id.child_item_on_off);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.tvName.setText(list.get(position).name);

        if (null != list.get(position).warning_log_info &&
                list.get(position).warning_log_info.length() > 0) {
            holder.tvWarningLogInfo.setText(list.get(position).warning_log_info);
        } else {
            holder.tvWarningLogInfo.setText("");
        }

        if (list.get(position).status) {
            holder.aSwitch.setChecked(true);
        } else {
            holder.aSwitch.setChecked(false);
        }

        holder.aSwitch.setOnCheckedChangeListener(this);
        holder.aSwitch.setTag(position);

        if (!list.get(position).service_status) {
            holder.aSwitch.setEnabled(false);
        } else {
            holder.aSwitch.setEnabled(true);
        }


        return convertView;
    }

    private Callback mCallback;

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mCallback.onCheckedChanged(buttonView, isChecked);
    }

    public interface Callback {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked);
    }

    class ViewHolder {
        TextView tvName;
        TextView tvWarningLogInfo;
        Switch aSwitch;
    }
}
