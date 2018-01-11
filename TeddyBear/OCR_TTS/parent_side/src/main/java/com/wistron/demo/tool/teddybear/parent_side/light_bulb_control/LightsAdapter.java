package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aaron on 17-2-17.
 */

public class LightsAdapter extends BaseAdapter {

    private List<Light> lightList;
    private LayoutInflater layoutInflater;
    private ListItemView listItemView;
    private OnCheckedChangeListener onCheckedChangeListener;


    private final class ListItemView {
        public TextView name;
        public Switch power;
        public TextView type;
    }

    public LightsAdapter(Context context) {
        layoutInflater = LayoutInflater.from(context);
        this.lightList = new ArrayList<>();
    }

    public void updateList(List<Light> lights) {
        lightList.clear();
        lightList.addAll(lights);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return lightList.size();
    }

    @Override
    public Object getItem(int i) {
        return lightList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            listItemView = new ListItemView();
            view = layoutInflater.inflate(R.layout.listitem_light_bulb, null);
            listItemView.name = (TextView) view.findViewById(R.id.light_bulb_item_name);
            listItemView.power = (Switch) view.findViewById(R.id.light_bulb_item_power);
            listItemView.type = (TextView) view.findViewById(R.id.light_bulb_item_type);
            view.setTag(listItemView);
        } else {
            listItemView = (ListItemView) view.getTag();
        }
        listItemView.name.setText(lightList.get(i).getName());
        listItemView.type.setText(lightList.get(i).getType());
        if (lightList.get(i).getState() == Light.POWER_ON) {
            listItemView.power.setChecked(true);
        } else {
            listItemView.power.setChecked(false);
        }
        listItemView.power.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                onCheckedChangeListener.onCheckedChanged(i, compoundButton, b);
            }
        });
        return view;
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        onCheckedChangeListener = listener;
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(int position, CompoundButton compoundButton, boolean b);
    }
}
