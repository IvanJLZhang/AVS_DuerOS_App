package com.wistron.demo.tool.teddybear.scene.useless;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.SettingsActivity;
import com.wistron.demo.tool.teddybear.ocr_tts.OcrTtsActivity;
import com.wistron.demo.tool.teddybear.scene.MonitorModeService;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String[] neededPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private boolean isStartMonitorMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            if (i >= neededPermissions.length) {
                findView();
            }
        } else {
            findView();
        }
    }

    private void findView() {
        findViewById(R.id.demo_item_ocr_tts).setOnClickListener(this);
        findViewById(R.id.demo_item_scene).setOnClickListener(this);

        findViewById(R.id.start_monitor_mode).setOnClickListener(this);
        findViewById(R.id.start_monitor_mode).setEnabled(false);
        try {
            PackageInfo packinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.version_info)).setText(String.format(getString(R.string.version_info), packinfo.versionName, packinfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        startMonitorModeService();
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
                launchOcrIntent.setClass(this, OcrTtsActivity.class);
                launchOcrIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchOcrIntent);
                break;
            case R.id.demo_item_scene:
                Intent launchSceneIntent = new Intent();
                launchSceneIntent.setClass(this, SceneActivity.class);
                launchSceneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchSceneIntent);
                break;
            case R.id.start_monitor_mode:
                isStartMonitorMode = !isStartMonitorMode;
                Intent startIntent = new Intent();
                startIntent.setAction(MonitorModeService.ACTION);
                if (isStartMonitorMode) {
                    startIntent.putExtra("action", "start");
                    ((Button) findViewById(R.id.start_monitor_mode)).setText(MainActivity.this.
                            getString(R.string.demo_title_monitor_mode_stop));
                } else {
                    startIntent.putExtra("action", "stop");
                    ((Button) findViewById(R.id.start_monitor_mode)).setText(MainActivity.this.
                            getString(R.string.demo_title_monitor_mode_start));
                }
                sendBroadcast(startIntent);
                break;
            default:
                break;
        }
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
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You should agree all of the permissions, force exit! please retry", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            findView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }


}
