package com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.parent_side.R;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by king on 17-1-9.
 */

public class NotificationAccessSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    // SMB params
    public static final String KEY_SWITCH = "notification_access_switch";
    public static final String KEY_APPS = "notification_access_apps";
    public CheckBoxPreference pref_Switch;
    public WisMultiSelectListPreference pref_Apps;

    public NotificationAccessSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_access_settings_preference);

        pref_Switch = (CheckBoxPreference) findPreference(KEY_SWITCH);
        pref_Switch.setOnPreferenceChangeListener(this);

        pref_Apps = (WisMultiSelectListPreference) findPreference(KEY_APPS);
        pref_Apps.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSwitchState();
        updateAppsState(true, null);
    }

    private void updateAppsState(boolean needUpdateList, Set<String> values) {
        //List<ApplicationInfo> mInstalledApps = getActivity().getPackageManager().getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> mInstalledApps = getActivity().getPackageManager().queryIntentActivities(queryIntent, 0);
        Collections.sort(mInstalledApps, new ResolveInfo.DisplayNameComparator(getActivity().getPackageManager()));

        if (values == null) {
            values = pref_Apps.getValues();
        }
        int grantedCount = 0;
        for (ResolveInfo info : mInstalledApps) {
            if (values.contains(info.activityInfo.packageName)) {
                grantedCount++;
            }
        }

        if (grantedCount == 1) {
            pref_Apps.setSummary(getString(R.string.notification_access_apps_summary_one));
        } else if (grantedCount > 1) {
            pref_Apps.setSummary(String.format(getString(R.string.notification_access_apps_summary_more), grantedCount));
        } else {
            pref_Apps.setSummary(getString(R.string.notification_access_apps_summary_none));
        }

        if (needUpdateList) {
            pref_Apps.setAppsList(mInstalledApps);
        }
    }

    private void updateSwitchState() {
        boolean isHaveNotificationAccess = false;
        for (String service : NotificationManagerCompat.getEnabledListenerPackages(getActivity())) {
            if (TextUtils.equals(getActivity().getPackageName(), service)) {
                isHaveNotificationAccess = true;
                break;
            }
        }
        if (isHaveNotificationAccess) {
            pref_Switch.setChecked(true);
        } else {
            pref_Switch.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.i("King", "key = " + preference.getKey() + ", newValue = " + newValue);
        if (preference.getKey().equals(KEY_SWITCH)) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            return true;
        } else if (preference.getKey().equals(KEY_APPS)) {
            updateAppsState(false, (Set<String>) newValue);
            return true;
        }
        return false;
    }
}
