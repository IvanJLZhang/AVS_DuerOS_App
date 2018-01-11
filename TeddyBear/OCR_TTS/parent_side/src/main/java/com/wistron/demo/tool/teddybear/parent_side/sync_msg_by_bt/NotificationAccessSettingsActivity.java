package com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class NotificationAccessSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PreferenceFragment settingsFragment = new NotificationAccessSettingsFragment();
        settingsFragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();
    }
}
