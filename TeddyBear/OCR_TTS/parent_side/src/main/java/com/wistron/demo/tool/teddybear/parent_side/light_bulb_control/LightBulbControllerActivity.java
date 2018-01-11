package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aaron on 17-3-8.
 */

public class LightBulbControllerActivity extends AppCompatActivity implements View.OnClickListener, LightsAdapter.OnCheckedChangeListener, LightControllerClient.onDataReceiveListener, LightControllerClient.onConnectListener {

    public static final String TAG = LightControllerClient.TAG;
    private static final int BASE = 0;
    private static final int MSG_UPDATE_LIST = BASE + 1;
    private static final int MSG_SHOW_CONN_SUCCESS_TOAST = BASE + 2;
    private static final int MSG_SHOW_CONN_FAIL_TOAST = BASE + 3;
    private static final int MSG_SHOW_GET_LIST_FAIL_TOAST = BASE + 4;
    private static final int TIMER_OUT_CONNECT = 3000;
    private static final int TIMER_OUT_GET_LIST = 30000;
    private LightControllerClient lightControllerClient;
    private Context mContext;
    private Button connBn, getLightsBn;
    private LightsAdapter lightsAdapter;
    private ListView listView;
    private List<Light> lightList;
    private ProgressDialog mProgressDialog;
    private Timer mTimer;
    private TimerTask mTimerTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        setContentView(R.layout.acivity_light_bulb_controller);
        connBn = (Button) findViewById(R.id.light_bulb_connect);
        connBn.setOnClickListener(this);
        getLightsBn = (Button) findViewById(R.id.light_bulb_get);
        getLightsBn.setOnClickListener(this);

        lightControllerClient = new LightControllerClient();
        lightControllerClient.setOnDataReceiveListener(this);
        lightControllerClient.setOnConnectListener(this);

        lightsAdapter = new LightsAdapter(this);
        listView = (ListView) findViewById(R.id.light_bulb_lights_list);
        listView.setAdapter(lightsAdapter);
        lightsAdapter.setOnCheckedChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lightControllerClient.release();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_UPDATE_LIST:
                    dismissProgressDialog();
                    stopTimer();
                    lightsAdapter.updateList(lightList);
                    break;
                case MSG_SHOW_CONN_SUCCESS_TOAST:
                    dismissProgressDialog();
                    stopTimer();
                    connBn.setEnabled(false);
                    Toast.makeText(mContext, getString(R.string.light_bulb_toast_msg_connect_success), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SHOW_CONN_FAIL_TOAST:
                    dismissProgressDialog();
                    stopTimer();
                    Toast.makeText(mContext, getString(R.string.light_bulb_toast_msg_connect_fail), Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SHOW_GET_LIST_FAIL_TOAST:
                    Toast.makeText(mContext, getString(R.string.light_bulb_toast_msg_get_list_fail), Toast.LENGTH_SHORT).show();
                default:
                    break;
            }

            super.handleMessage(msg);
        }
    };

    @Override
    public void onClick(View v) {
        if (v == connBn) {
            startTimeoutTimer(TIMER_OUT_CONNECT);
            showProgressDialog(getString(R.string.light_bulb_progress_dialog_msg_connect));
            lightControllerClient.connServer();

        } else if (v == getLightsBn) {
            startTimeoutTimer(TIMER_OUT_GET_LIST);
            showProgressDialog(getString(R.string.light_bulb_progress_dialog_msg_loading));
            lightControllerClient.getLightList();
        }
    }

    @Override
    public void onCheckedChanged(int position, CompoundButton compoundButton, boolean b) {
        Light light = (Light) listView.getItemAtPosition(position);
        if (b) {
            lightControllerClient.powerOn(light);
        } else {
            lightControllerClient.powerOff(light);

        }
    }

    @Override
    public void onDataReceive(String data) {
        Log.i(TAG, "receive : " + data);
        try {
            JSONObject jsonObject = new JSONObject(data);
            switchCommand(jsonObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void startTimeoutTimer(final int time) {
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                if (TIMER_OUT_CONNECT == time) {
                    mHandler.sendEmptyMessage(MSG_SHOW_CONN_FAIL_TOAST);
                } else if (TIMER_OUT_GET_LIST == time) {
                    mHandler.sendEmptyMessage(MSG_SHOW_CONN_FAIL_TOAST);
                }
            }
        };
        mTimer.schedule(mTimerTask, time);
    }

    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mTimerTask != null) {
            mTimerTask.cancel();
            mTimerTask = null;
        }
    }

    private void switchCommand(JSONObject jsonObject) {
        int cmd = JSONUtil.getCommand(jsonObject);
        switch (cmd) {
            case Light.CMD_GET_LIGHTS:
                lightList = JSONUtil.getLightList(jsonObject);
                if (lightList != null)
                    mHandler.sendEmptyMessage(MSG_UPDATE_LIST);
                break;
            default:
                break;
        }
    }

    private void showProgressDialog(String msg) {
        mProgressDialog = ProgressDialog.show(this, null, msg);
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onConnectSuccess() {
        mHandler.sendEmptyMessage(MSG_SHOW_CONN_SUCCESS_TOAST);
    }

    @Override
    public void onConnectFail() {
        mHandler.sendEmptyMessage(MSG_SHOW_CONN_FAIL_TOAST);
    }
}
