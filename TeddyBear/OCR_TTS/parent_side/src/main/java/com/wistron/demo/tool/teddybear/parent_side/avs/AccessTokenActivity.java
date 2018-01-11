package com.wistron.demo.tool.teddybear.parent_side.avs;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.view.FlatButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class AccessTokenActivity extends AppCompatActivity {
    private ProgressDialog waitingDialog;
    AccessToken accessToken = null;
    private BaseTaskManager mTaskManager;

    private String mRemoteFolder = "common/";
    private String mRemoteFileName = "token.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_token);

        initView();
    }

    private void initView() {
        accessToken = new AccessToken(AccessTokenActivity.this);
        accessToken.setAccessTokenListener(accessTokenListener);

        FlatButton button = (FlatButton) findViewById(R.id.demo_item_avs_login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetAccessTokenWithAmazon getAccessTokenWithAmazon = new GetAccessTokenWithAmazon();
                getAccessTokenWithAmazon.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mTaskManager.addAzureStorageChangedListener(mResultChangedListener);

        waitingDialog = new ProgressDialog(this);
        waitingDialog.setMessage("Sync the information to cloud, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);
    }

    private class GetAccessTokenWithAmazon extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... params) {
            accessToken.authorizeUser();
            return null;
        }
    }

    private AccessToken.AccessTokenListener accessTokenListener = new AccessToken.AccessTokenListener() {
        @Override
        public void success() {
            mMainHandler.sendEmptyMessage(Common.MSG_OTHERS);

            saveTokenToTxtFileAndSyncToCloud();
        }

        @Override
        public void failure() {
            mMainHandler.sendEmptyMessage(Common.MSG_GET_ACCESSTOKEN_FAILURE);
        }
    };

    private void saveTokenToTxtFileAndSyncToCloud() {
        SharedPreferences preferences = getSharedPreferences(Common.TOKEN_PREFERENCE_KEY, Context
                .MODE_PRIVATE);

        StringBuffer buffer = new StringBuffer();
        buffer.append(preferences.getString(Common.PREF_ACCESS_TOKEN, "") + ":");
        buffer.append(preferences.getString(Common.PREF_REFRESH_TOKEN, ""));

        File file = new File(getExternalFilesDir(null), "token.txt");
        if (file.exists()) {
            file.delete();
        }

        boolean isSaveSuccess = false;
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(buffer.toString());
            writer.flush();
            isSaveSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(Common.TAG, "save token faile: " + e.toString());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (isSaveSuccess) {
            mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB,
                    BaseTaskManager.REQUEST_ACTION_UPLOAD, file.getAbsolutePath(),
                    mRemoteFolder, mRemoteFileName);
        }
    }


    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager
            .OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {

                    mMainHandler.sendEmptyMessage(responseCode);
                    Log.i(Common.TAG, "upload success");

                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    Message msg = mMainHandler.obtainMessage(responseCode);
                    msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server);
                    mMainHandler.sendMessage(msg);

                    Log.i(Common.TAG, getString(R.string.msg_cant_connect_ftp_server));
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD) {
                    Message msg = mMainHandler.obtainMessage(responseCode);
                    msg.obj = "sync fail: Failed to upload";
                    mMainHandler.sendMessage(msg);

                    Log.i(Common.TAG, "sync failed");
                }
            }
        }
    };

    private Handler mMainHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == Common.MSG_OTHERS) {
                waitingDialog.show();
            } else if (msg.what == Common.MSG_GET_ACCESSTOKEN_FAILURE) {
                Toast.makeText(AccessTokenActivity.this, "Get access token faile, please re-try", Toast
                        .LENGTH_LONG).show();
            } else {
                if (waitingDialog.isShowing()) {
                    waitingDialog.dismiss();
                }
                if (msg.what == BaseTaskManager.RESPONSE_CODE_PASS) {
                    Toast.makeText(AccessTokenActivity.this, "Sync success!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AccessTokenActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
                }
            }
            return false;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTaskManager != null) {
            mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
        }
    }
}
