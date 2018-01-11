package com.wistron.demo.tool.teddybear.parent_side;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.util.SortedList;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Time：16-9-21 10:02
 * Author：bob
 */
public class UsersIdentificationByBluetoothAdapter extends RecyclerView
        .Adapter<UsersIdentificationByBluetoothAdapter.MyViewHolder> {
    private SortedList<BtConfigDevice> mUserList;
    private Context context;
    private int curSelectedItem = -1;
    private UsersIdentificationByBluetoothAdapter.OnSelectedItemChangedListener selectedItemChangedListener;

    public UsersIdentificationByBluetoothAdapter(Context context) {
        this.context = context;
    }

    public void setUserList(SortedList<BtConfigDevice> list) {
        this.mUserList = list;
    }

    public void setOnSelectedItemChangedListener(UsersIdentificationByBluetoothAdapter.OnSelectedItemChangedListener l) {
        selectedItemChangedListener = l;
    }

    public interface OnSelectedItemChangedListener {
        void onSelectedItemChanged();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MyViewHolder holder = new MyViewHolder(LayoutInflater.from(
                parent.getContext()).inflate(R.layout.user_bluetooth_adapter_view, parent,
                false));
        return holder;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        if (position == curSelectedItem) {
            holder.mItemView.setCardBackgroundColor(Color.CYAN);
        } else {
            holder.mItemView.setCardBackgroundColor(Color.LTGRAY);
        }

        holder.mItemName.setText(mUserList.get(position).getUserName());
        holder.mItemEmail.setText(mUserList.get(position).getEmailAddress());
        holder.mItemMAC.setText(mUserList.get(position).getBtMacAddress());

        Typeface typeface1 = Typeface.createFromAsset(context.getAssets(), "fonts/calibril.ttf");
        holder.mItemName.setTypeface(typeface1);
        holder.mItemMAC.setTypeface(typeface1);
        holder.mItemEmail.setTypeface(typeface1);
    }

    public int getCurSelectedItem() {
        return curSelectedItem;
    }

    public void setCurSelectedItem(int position) {
        curSelectedItem = position;
    }

    public void resetCurSelectedItem() {
        curSelectedItem = -1;
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        CardView mItemView;
        TextView mItemName;
        TextView mItemMAC;
        TextView mItemEmail;

        public MyViewHolder(View itemView) {
            super(itemView);
            mItemView = (CardView) itemView;
            mItemName = (TextView) itemView.findViewById(R.id.user_view_name);
            mItemMAC = (TextView) itemView.findViewById(R.id.user_view_mac);
            mItemEmail = (TextView) itemView.findViewById(R.id.user_view_email);

            mItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int oldIndex = curSelectedItem;
                    curSelectedItem = getAdapterPosition();
                    UsersIdentificationByBluetoothAdapter.this.notifyItemChanged(oldIndex);
                    UsersIdentificationByBluetoothAdapter.this.notifyItemChanged(curSelectedItem);

                    if (selectedItemChangedListener != null) {
                        selectedItemChangedListener.onSelectedItemChanged();
                    }
                }
            });
        }
    }
}
