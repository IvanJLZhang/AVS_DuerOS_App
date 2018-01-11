package com.wistron.demo.tool.teddybear.scene.helper;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wistron.demo.tool.teddybear.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.protocol.QiniuStorageTaskManager;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;

import java.io.File;


/**
 * Created by king on 16-9-30.
 */

public class SyncCloudConfig {
    private Context context;
    private Handler mMainHandler;

    private BaseTaskManager mStorageTaskManager = null;
    private String mCurNetworkOpt;

    public SyncCloudConfig(Context context, Handler mMainHandler) {
        this.context = context;
        this.mMainHandler = mMainHandler;
    }

    public void syncCloudSettings() {
        if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_AZURE) {
            mStorageTaskManager = AzureStorageTaskManager.getInstance(context);
        } else if (SceneCommonHelper.DEFAULT_STORAGE == SceneCommonHelper.STORAGE_QINIU) {
            mStorageTaskManager = QiniuStorageTaskManager.getInstance(context);
        }
        mStorageTaskManager.addAzureStorageChangedListener(mResultChangedListener);

        // create SN folder
        mCurNetworkOpt = BaseTaskManager.REQUEST_ACTION_CREATEFOLDER;
        mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_KING, BaseTaskManager.REQUEST_ACTION_CREATEFOLDER, Build.SERIAL);
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {

        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_KING)) {
                Log.i("King", "SyncCloudConfig: Network response = " + responseCode);
                if (mCurNetworkOpt.equals(BaseTaskManager.REQUEST_ACTION_CREATEFOLDER)) {
                    mCurNetworkOpt = BaseTaskManager.REQUEST_ACTION_CREATEFOLDER;
                    // sync SVA BT config
                    File mLocalFile = new File(context.getFilesDir(), SceneCommonHelper.REMOTE_SVA_BT_FILE);
                    mCurNetworkOpt = BaseTaskManager.REQUEST_ACTION_DOWNLOAD;
                    mStorageTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_KING, mCurNetworkOpt, SceneCommonHelper.REMOTE_FOLDER_COMMON, SceneCommonHelper.REMOTE_SVA_BT_FILE, mLocalFile.getAbsolutePath());
                } else if (mCurNetworkOpt.equals(BaseTaskManager.REQUEST_ACTION_DOWNLOAD)) {
                    String logMsg = "Succeed in syncing SVA BT config.\n";
                    if (responseCode != BaseTaskManager.RESPONSE_CODE_PASS) {
                        logMsg = "Failed to sync SVA BT config.\n";
                    }

                    Message msg = new Message();
                    msg.what = SceneActivity.MSG_UPDATE_LOG;
                    msg.obj = logMsg;
                    mMainHandler.sendMessage(msg);

                    mStorageTaskManager.removeAzureStorageChangedListener(this);
                }
            }
        }
    };
}
