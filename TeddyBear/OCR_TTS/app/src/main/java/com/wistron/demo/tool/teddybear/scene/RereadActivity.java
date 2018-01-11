package com.wistron.demo.tool.teddybear.scene;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.RereadAudioRecordHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

public class RereadActivity extends AppCompatActivity {
    private TextView tv_RereadStatus;

    private MainHandler mMainHandler;

    private RereadAudioRecordHelper mAudioRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_reread_scene);

        findView();
        initial();
    }

    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SceneActivity.MSG_UPDATE_LOG:
                    updateLog(msg.obj.toString());
                    break;
                default:
                    break;
            }
        }
    }

    private void updateLog(String log) {
        tv_RereadStatus.setText(log);
        Log.i("King", "RereadActivity --> " + log);
    }

    private void initial() {
        mMainHandler = new MainHandler();
        updateLog(getString(R.string.reread_status_tips));

        mAudioRecord = new RereadAudioRecordHelper(this, mMainHandler);
        mAudioRecord.initial();
    }

    private void findView() {
        tv_RereadStatus = (TextView) findViewById(R.id.reread_scene_status);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int repeatCount = event.getRepeatCount();
        if (keyCode == KeyEvent.KEYCODE_F12
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (repeatCount == 0) {
                event.startTracking();
            }
        } else {
            finish();
        }
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F12
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.i("King", "onKeyLongPress = " + event.getRepeatCount());
            mAudioRecord.startRecord("reread.wav");
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F12
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Log.i("King", "onKeyUp = " + event.getRepeatCount());
            mAudioRecord.stopRecord();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SceneCommonHelper.closeLED();
        mAudioRecord.exit();
    }
}
