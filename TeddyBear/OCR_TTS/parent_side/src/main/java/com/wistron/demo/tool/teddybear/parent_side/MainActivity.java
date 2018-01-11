package com.wistron.demo.tool.teddybear.parent_side;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.parent_side.avs.AvsLoginActivity;
import com.wistron.demo.tool.teddybear.parent_side.dcs.DcsLoginActivity;
import com.wistron.demo.tool.teddybear.parent_side.light_bulb_control.LightBulbControllerActivity;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.OcrTtsSettingsActivity;
import com.wistron.demo.tool.teddybear.parent_side.parent_app.MessageScene;
import com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt.NotificationAccessSettingsActivity;
import com.wistron.demo.tool.teddybear.parent_side.useless.SettingsActivity;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatUI;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui.SetupActivity;
import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui.SetupWifiActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String[] neededPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FlatUI.initDefaultValues(this);
        FlatUI.setDefaultTheme(FlatUI.SKY);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = 0;
            for (; i < neededPermissions.length; i++) {
                if (checkSelfPermission(neededPermissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(neededPermissions, REQUEST_CODE_ASK_PERMISSIONS);
                    break;
                }
            }
        }
        findView();
        initial();
    }

    private void findView() {
        setDisplayFonts();

        findViewById(R.id.demo_item_ocr_tts).setOnClickListener(this);
        findViewById(R.id.demo_item_scene).setOnClickListener(this);
        findViewById(R.id.demo_item_monitor_mode).setOnClickListener(this);
        findViewById(R.id.demo_item_children_management).setOnClickListener(this);
        findViewById(R.id.demo_item_enroll_speaker).setOnClickListener(this);
        findViewById(R.id.demo_item_wifi_setting_bt).setOnClickListener(this);
        findViewById(R.id.demo_item_wifi_setting_p2p).setOnClickListener(this);
        findViewById(R.id.demo_item_bluetooth_setting).setOnClickListener(this);
        findViewById(R.id.demo_item_avs).setOnClickListener(this);
        findViewById(R.id.demo_item_baidu).setOnClickListener(this);
        findViewById(R.id.demo_item_notification_access).setOnClickListener(this);
        findViewById(R.id.demo_item_light_bulb_control_mode).setOnClickListener(this);

        try {
            PackageInfo packinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version_info)).setText(String.format(getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initial() {
        startMonitorModeService();
    }

    private void setDisplayFonts() {
        //set display fonts
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        ((FlatButton) findViewById(R.id.demo_item_ocr_tts)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_scene)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_monitor_mode)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_children_management)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_enroll_speaker)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_wifi_setting_bt)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_wifi_setting_p2p)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_bluetooth_setting)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_avs)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_baidu)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_notification_access)).setTypeface(typeface1);
        ((FlatButton) findViewById(R.id.demo_item_light_bulb_control_mode)).setTypeface(typeface1);

        ((TextView) findViewById(R.id.version_info)).setTypeface(typeface1);
        ((TextView) findViewById(R.id.demo_group_title_pre_settings)).setTypeface(typeface1, Typeface.BOLD);
        ((TextView) findViewById(R.id.demo_group_title_scenario_settings)).setTypeface(typeface1, Typeface.BOLD);
    }

    private void startMonitorModeService() {
        Intent launchMonitorModeIntent = new Intent();
        launchMonitorModeIntent.setClass(this, MonitorModeService.class);
        startService(launchMonitorModeIntent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.demo_item_ocr_tts:
                Intent launchOcrIntent = new Intent();
                launchOcrIntent.setClass(this, OcrTtsSettingsActivity.class);
                launchOcrIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchOcrIntent);
                break;
            case R.id.demo_item_scene:
                Intent launchParentAppIntent = new Intent();
                launchParentAppIntent.setClass(this, MessageScene.class);
                launchParentAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchParentAppIntent);
                break;
            case R.id.demo_item_monitor_mode:
                Intent launchMonitorModeIntent = new Intent();
                launchMonitorModeIntent.setClass(this, MonitorModeActivity.class);
                launchMonitorModeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchMonitorModeIntent);
                break;
            case R.id.demo_item_children_management:
                Intent launchChildrenManagementIntent = new Intent();
                launchChildrenManagementIntent.setClass(this, ChildrenManagementActivity.class);
                launchChildrenManagementIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchChildrenManagementIntent);
                break;
            case R.id.demo_item_enroll_speaker:
                Intent launchEnrollSpeakerIntent = new Intent();
                launchEnrollSpeakerIntent.setClass(this, EnrollSpeaker.class);
                launchEnrollSpeakerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchEnrollSpeakerIntent);
                break;
            case R.id.demo_item_wifi_setting_bt:
                Intent launchWifiSettingsBTIntent = new Intent();
                launchWifiSettingsBTIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchWifiSettingsBTIntent.setClass(this, SetupActivity.class);
                startActivity(launchWifiSettingsBTIntent);
                break;
            case R.id.demo_item_wifi_setting_p2p:
                Intent launchWifiSettingsP2PIntent = new Intent();
                launchWifiSettingsP2PIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launchWifiSettingsP2PIntent.setClass(this, SetupWifiActivity.class);
                startActivity(launchWifiSettingsP2PIntent);
                break;
            case R.id.demo_item_bluetooth_setting:
                Intent launchSvaUsersIntent = new Intent();
                launchSvaUsersIntent.setClass(this, UsersIdentificationByBluetoothActivity.class);
                launchSvaUsersIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchSvaUsersIntent);
                break;
            case R.id.demo_item_avs:
//                Intent launchAVSIntent = new Intent();
//                launchAVSIntent.setClass(MainActivity.this, AccessTokenActivity.class);
//                launchAVSIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(launchAVSIntent);
                Intent launchAVSIntent = new Intent();
                launchAVSIntent.setClass(MainActivity.this, AvsLoginActivity.class);
                launchAVSIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchAVSIntent);
                break;
            case R.id.demo_item_baidu:
                Intent launchDCSIntent = new Intent();
                launchDCSIntent.setClass(MainActivity.this, DcsLoginActivity.class);
                launchDCSIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchDCSIntent);
                break;
            case R.id.demo_item_notification_access:
                Intent launchNotificationAccessSettingsIntent = new Intent();
                launchNotificationAccessSettingsIntent.setClass(this, NotificationAccessSettingsActivity.class);
                launchNotificationAccessSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchNotificationAccessSettingsIntent);
                break;
            case R.id.demo_item_light_bulb_control_mode:
                Intent launchLightBulbControlIntent = new Intent();
                launchLightBulbControlIntent.setClass(this, LightBulbControllerActivity.class);
                launchLightBulbControlIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchLightBulbControlIntent);
                break;
            default:
                break;
        }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_item_children_management) {
            Intent intent = new Intent(this, ChildrenManagementActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.scene_menu_item_enroll_speaker) {
            Intent enrollSpeakerIntent = new Intent(this, EnrollSpeaker.class);
            startActivity(enrollSpeakerIntent);
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = getSharedPreferences("monitormodeservice", Activity.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
    }
}
