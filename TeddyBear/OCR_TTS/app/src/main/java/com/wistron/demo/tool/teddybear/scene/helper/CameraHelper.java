package com.wistron.demo.tool.teddybear.scene.helper;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.wistron.demo.tool.teddybear.R;

public class CameraHelper extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera_helper);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            NewCameraTakeFragment newCameraTakeFragment = new NewCameraTakeFragment();
            newCameraTakeFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(android.R.id.content, newCameraTakeFragment).commit();
        } else {
            CameraTakeFragment cameraTakeFragment = new CameraTakeFragment();
            cameraTakeFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().replace(android.R.id.content, cameraTakeFragment).commit();
        }
    }
}
