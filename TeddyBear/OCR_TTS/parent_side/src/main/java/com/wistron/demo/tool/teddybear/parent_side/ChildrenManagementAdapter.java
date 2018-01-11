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
 * Created by king on 16-5-3.
 */
public class ChildrenManagementAdapter extends RecyclerView.Adapter<ChildrenManagementAdapter
        .RecyclerHolder> {
    private SortedList<Child> mChildrenList;
    private OnSelectedItemChangedListener selectedItemChangedListener;
    private int curSelectedItem = -1;

    private Context context;

    public ChildrenManagementAdapter(SortedList<Child> mChildrenList) {
        this.mChildrenList = mChildrenList;
    }

    public ChildrenManagementAdapter(Context context) {
        this.context = context;
    }

    public void setChildrenList(SortedList<Child> mChildrenList) {
        this.mChildrenList = mChildrenList;
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

    public void setOnSelectedItemChangedListener(OnSelectedItemChangedListener l) {
        selectedItemChangedListener = l;
    }

    public interface OnSelectedItemChangedListener {
        void onSelectedItemChanged();
    }

    @Override
    public RecyclerHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View mView = LayoutInflater.from(parent.getContext()).inflate(R.layout.children_management_view,
                parent, false);
        RecyclerHolder mHolder = new RecyclerHolder(mView);
        return mHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerHolder holder, int position) {
        if (position == curSelectedItem) {
            holder.mItemView.setCardBackgroundColor(Color.CYAN);
        } else {
            holder.mItemView.setCardBackgroundColor(Color.LTGRAY);
        }
        holder.mItemName.setText(mChildrenList.get(position).getName());
        holder.mItemSN.setText(mChildrenList.get(position).getSn());

        Typeface typeface1 = Typeface.createFromAsset(context.getAssets(), "fonts/calibril.ttf");

        holder.mItemName.setTypeface(typeface1);
        holder.mItemSN.setTypeface(typeface1);
    }

    @Override
    public int getItemCount() {
        return mChildrenList.size();
    }

    public class RecyclerHolder extends RecyclerView.ViewHolder {
        CardView mItemView;
        TextView mItemName;
        TextView mItemSN;

        public RecyclerHolder(final View itemView) {
            super(itemView);
            mItemView = (CardView) itemView;
            mItemName = (TextView) itemView.findViewById(R.id.child_view_name);
            mItemSN = (TextView) itemView.findViewById(R.id.child_view_sn);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int oldIndex = curSelectedItem;
                    curSelectedItem = getAdapterPosition();
                    ChildrenManagementAdapter.this.notifyItemChanged(oldIndex);
                    ChildrenManagementAdapter.this.notifyItemChanged(curSelectedItem);

                    if (selectedItemChangedListener != null) {
                        selectedItemChangedListener.onSelectedItemChanged();
                    }
                }
            });
        }
    }
}
