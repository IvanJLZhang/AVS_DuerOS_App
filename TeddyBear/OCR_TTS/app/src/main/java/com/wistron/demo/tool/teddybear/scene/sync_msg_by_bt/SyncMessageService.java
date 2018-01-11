package com.wistron.demo.tool.teddybear.scene.sync_msg_by_bt;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import java.util.ArrayList;

public class SyncMessageService extends Service {
    private final String TAG = "SyncMessageService";
    private BluetoothChatHandler mChatHandler;

    private ToSpeak mToSpeak;
    private ArrayList<String> mMsgList = new ArrayList<>();
    private boolean isReadingMsg;

    private SceneActivity mBaseActivity;

    public SyncMessageService() {
    }

    public class SyncMsgServiceBinder extends Binder {
        public SyncMessageService getService(SceneActivity activity) {
            mBaseActivity = activity;
            return SyncMessageService.this;
        }
    }

    private SyncMsgServiceBinder syncMsgServiceBinder = new SyncMsgServiceBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return syncMsgServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mToSpeak = ToSpeak.getInstance(this);

        mChatHandler = new BluetoothChatHandler(this, BluetoothChatHandler.MODE_SERVER);

        mChatHandler.setOnWisBluetoothChatStateChangedListener(mBTChatStateChangedListener);
        mChatHandler.setOnWisBluetoothChatDataChangedListener(mBTChatDataChangedListener);
        mChatHandler.startServer();
    }

    private BluetoothChatHandler.OnWisBluetoothChatStateChangedListener mBTChatStateChangedListener = new BluetoothChatHandler.OnWisBluetoothChatStateChangedListener() {

        @Override
        public void onStateIsListening() {
            // TODO Auto-generated method stub
            Log.i(TAG, "Listening client ");
        }

        @Override
        public void onStateIsIdle() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStateIsDisconnect(String remoteName) {
            // TODO Auto-generated method stub
            Log.i(TAG, "disconnect client : " + remoteName);
        }

        @Override
        public void onStateIsConnecting() {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStateIsConnected(String remoteName) {
            // TODO Auto-generated method stub
            Log.i(TAG, "connected client : " + remoteName);
        }

        @Override
        public void onStateIsConnectFail() {
            // TODO Auto-generated method stub

        }

    };

    private BluetoothChatHandler.OnWisBluetoothChatDataChangedListener mBTChatDataChangedListener = new BluetoothChatHandler.OnWisBluetoothChatDataChangedListener() {

        @Override
        public void onReadMessage(String from, String msg) {
            // TODO Auto-generated method stub
            String log = String.format("Get data from Client(%1$s): %2$s", from, msg);
            Toast.makeText(SyncMessageService.this, log, Toast.LENGTH_LONG).show();
            Log.i(TAG, "--> " + log);
            mMsgList.add(msg);
            if (!isReadingMsg) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        isReadingMsg = true;
                        if (mMsgList.size() > 0) {
                            mBaseActivity.pauseCurrentScene();
                        }
                        while (isReadingMsg && mMsgList.size() > 0) {
                            String msg = mMsgList.get(0);
                            mToSpeak.toSpeak(String.format(SceneCommonHelper.getString(SyncMessageService.this, R.string.phone_msg_got_new), msg), false);
                            mMsgList.remove(0);
                        }
                        isReadingMsg = false;
                    }
                }).start();
            }
        }
    };

    public void stopSyncMessageServiceReading() {
        if (isReadingMsg) {
            isReadingMsg = false;
            mToSpeak.stop();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mChatHandler.stop();
    }
}
