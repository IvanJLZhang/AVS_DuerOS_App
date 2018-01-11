package com.wistron.demo.tool.teddybear.parent_side.parent_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by tanbo on 16-4-15.
 */
public class ParentListAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    private Context context;

    private ArrayList<HashMap<String, Object>> hashMapArrayList;

    public ParentListAdapter(Context context, ArrayList<HashMap<String, Object>> parent_app_arreylist) {
        this.mInflater = LayoutInflater.from(context);
        this.hashMapArrayList = parent_app_arreylist;
        this.context = context;
    }

    public int getCount() {
        return hashMapArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return hashMapArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ParentAppViewHolder parentAppViewHolder;
        String[] cach_listview_string = new String[]{"parent_app_head_image", "parent_app_speech_text"};
        int[] cach_listview_int = new int[]{R.id.head_image_in_list, R.id.speech_text_in_list};
        if (convertView == null) {
            parentAppViewHolder = new ParentAppViewHolder();
            convertView = mInflater.inflate(R.layout.listview_parent_app, null);
            parentAppViewHolder.speech_text = (TextView) convertView.findViewById(R.id.speech_text_in_list);
            parentAppViewHolder.head_image = (ImageView) convertView.findViewById(R.id.head_image_in_list);
            convertView.setTag(parentAppViewHolder);
        } else {
            parentAppViewHolder = (ParentAppViewHolder) convertView.getTag();
        }
        final Map map_get_postion = hashMapArrayList.get(position);
        for (int i = 0; i < cach_listview_int.length; i++) {
            final View v = convertView.findViewById(cach_listview_int[i]);
            final Object object = map_get_postion.get(cach_listview_string[i]);
            String text = object == null ? "" : object.toString();
            if (v instanceof ImageView && object instanceof Bitmap) {
                parentAppViewHolder.head_image.setImageBitmap((Bitmap) object);
            } else {
            }
            if (v instanceof TextView) ((TextView) v).setText(text);

        }

        return convertView;
    }


}

