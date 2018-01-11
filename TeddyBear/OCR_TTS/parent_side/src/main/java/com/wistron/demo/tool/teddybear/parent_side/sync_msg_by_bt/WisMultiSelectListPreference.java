package com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.MultiSelectListPreference;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by king on 17-1-10.
 */

public class WisMultiSelectListPreference extends MultiSelectListPreference {
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Drawable[] mIcons;
    private Set<String> mValues = new HashSet<String>();
    private Set<String> mNewValues = new HashSet<String>();
    private boolean mPreferenceChanged;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WisMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WisMultiSelectListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WisMultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WisMultiSelectListPreference(Context context) {
        super(context);
    }

    public void setAppsList(List<ResolveInfo> mAppsList) {
        CharSequence[] tempEntries = new CharSequence[mAppsList.size()];
        CharSequence[] tempEntryValues = new CharSequence[mAppsList.size()];
        mIcons = new Drawable[mAppsList.size()];
        mValues = getValues();
        Set<String> tempValues = new HashSet<String>();
        for (int i = 0; i < mAppsList.size(); i++) {
            ResolveInfo info = mAppsList.get(i);
            tempEntries[i] = info.loadLabel(getContext().getPackageManager());
            tempEntryValues[i] = info.activityInfo.packageName;
            mIcons[i] = info.loadIcon(getContext().getPackageManager());
            if (mValues.contains(info.activityInfo.packageName)) {
                tempValues.add(info.activityInfo.packageName);
            }
        }

        setEntries(tempEntries);
        setEntryValues(tempEntryValues);
        setValues(tempValues);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        //super.onPrepareDialogBuilder(builder);

        mEntries = getEntries();
        mEntryValues = getEntryValues();
        mValues = getValues();

        if (mEntries == null || mEntryValues == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference requires an entries array and " +
                            "an entryValues array.");
        }

        /*boolean[] checkedItems = getSelectedItems();
        builder.setMultiChoiceItems(mEntries, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            mPreferenceChanged |= mNewValues.add(mEntryValues[which].toString());
                        } else {
                            mPreferenceChanged |= mNewValues.remove(mEntryValues[which].toString());
                        }
                    }
                });*/

        builder.setAdapter(new AppsListAdapter(getContext(), R.layout.apps_item_multi_select_layout, mEntryValues), null);

        mNewValues.clear();
        mNewValues.addAll(mValues);
    }

    private boolean[] getSelectedItems() {
        final CharSequence[] entries = mEntryValues;
        final int entryCount = entries.length;
        final Set<String> values = mValues;
        boolean[] result = new boolean[entryCount];

        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entries[i].toString());
        }

        return result;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        //super.onDialogClosed(positiveResult);

        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (callChangeListener(values)) {
                setValues(values);
            }
        }
        mPreferenceChanged = false;
    }

    private class Holder {
        RelativeLayout mRootView;
        ImageView mAppIcon;
        TextView mAppTitle;
        CheckBox mCheckedState;
    }

    private class AppsListAdapter extends ArrayAdapter<CharSequence> {


        public AppsListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull CharSequence[] objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final Holder mHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.apps_item_multi_select_layout, null);
                mHolder = new Holder();
                mHolder.mRootView = (RelativeLayout) convertView.findViewById(R.id.app_item_root);
                mHolder.mAppTitle = (TextView) convertView.findViewById(R.id.app_item_title);
                mHolder.mCheckedState = (CheckBox) convertView.findViewById(R.id.app_item_check_state);
                mHolder.mAppIcon = (ImageView) convertView.findViewById(R.id.app_item_icon);
                convertView.setTag(mHolder);
            } else {
                mHolder = (Holder) convertView.getTag();
            }

            mHolder.mAppTitle.setText(mEntries[position]);
            mHolder.mAppIcon.setImageDrawable(mIcons[position]);
            mHolder.mCheckedState.setChecked(mNewValues.contains(mEntryValues[position]));
            mHolder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("King", "click = " + position);
                    boolean isChecked = !mHolder.mCheckedState.isChecked();
                    mHolder.mCheckedState.setChecked(isChecked);
                    if (isChecked) {
                        mPreferenceChanged |= mNewValues.add(mEntryValues[position].toString());
                    } else {
                        mPreferenceChanged |= mNewValues.remove(mEntryValues[position].toString());
                    }
                }
            });

            return convertView;
        }
    }

}
