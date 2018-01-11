package com.wistron.demo.tool.teddybear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.SceneActivity;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        Log.i("King", "Boot to launch SceneActivity...");

        Intent launchSceneIntent = new Intent();
        launchSceneIntent.setClass(context, SceneActivity.class);
        launchSceneIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launchSceneIntent);
    }
}
