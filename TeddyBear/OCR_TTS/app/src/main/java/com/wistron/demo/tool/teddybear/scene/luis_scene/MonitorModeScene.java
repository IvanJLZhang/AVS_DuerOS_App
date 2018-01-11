package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.MonitorModeService;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Time：16-5-10 16:40
 * Author：bob
 */
public class MonitorModeScene extends SceneBase {

    public MonitorModeScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        super.simulate();
        try {
            boolean hasAction = false;
            boolean toStartMonitor = false;
            for (int i = 0; i < sceneParams.length(); i++) {
                String entitiesType = sceneParams.getJSONObject(i).getString(LuisHelper.TAG_TYPE);
                if (LuisHelper.ENTITIES_TYPE_ON_OFF_START.equals(entitiesType)) {
                    hasAction = true;
                    toStartMonitor = true;
                    break;
                } else if (LuisHelper.ENTITIES_TYPE_ON_OFF_STOP.equals(entitiesType)) {
                    hasAction = true;
                    toStartMonitor = false;
                    break;
                }
            }
            if (hasAction) {
                if (toStartMonitor) {
                    Log.i("King", "to start monitor mode...");
                    startMonitorModeService();
                    updateLog(context.getString(R.string.luis_log_monitor_start));
                } else {
                    Log.i("King", "to stop monitor mode...");
                    stopMonitorModeService();
                    updateLog(context.getString(R.string.luis_log_monitor_stop));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SceneCommonHelper.closeLED();
    }

    private void startMonitorModeService() {
        Intent launchMonitorModeIntent = new Intent();
        launchMonitorModeIntent.setClass(context, MonitorModeService.class);
        context.startService(launchMonitorModeIntent);
    }

    private void stopMonitorModeService() {
        Intent stopMonitorModeIntent = new Intent();
        stopMonitorModeIntent.setClass(context, MonitorModeService.class);
        context.stopService(stopMonitorModeIntent);
    }
}
