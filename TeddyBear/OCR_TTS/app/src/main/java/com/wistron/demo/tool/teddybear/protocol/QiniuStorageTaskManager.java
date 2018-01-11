package com.wistron.demo.tool.teddybear.protocol;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

import static com.wistron.demo.tool.teddybear.protocol.QiniuUtils.getUploadToken;


/**
 * Created by king on 16-9-29.
 */

public class QiniuStorageTaskManager extends BaseTaskManager {
    public static final String TAG = "QiniuStorageTaskManager";

    private static final String ACCESS_KEY = "mIg8vv7_aVSeFB2XDNYZ4-wH6Q6e14471ZQRG3i0";
    private static final String SECRET_KEY = "zXDUIrsxBWxwUJmfNpAaqAd8qq_hceomTA3ZXOa6";
    private static final String BUCKET_NAME = "teddy-bear";
    private static final String DOMAIN_NAME = "http://oe5phuhpo.bkt.clouddn.com/";

    private UploadManager uploadManager;

    private QiniuStorageTaskManager() {
        blockingQueue = new LinkedBlockingDeque<>();
        mRequestResultChangedListeners = new ArrayList<>();
        uploadManager = new UploadManager();
        new Thread(runNetworkRequest).start();
    }

    public static QiniuStorageTaskManager getInstance(Context context) {
        if (mBaseTaskManager == null) {
            mBaseTaskManager = new QiniuStorageTaskManager();
        }
        return (QiniuStorageTaskManager) mBaseTaskManager;
    }

    @Override
    protected void getFiles(boolean isGetSubFolder, String... params) {
        super.getFiles(isGetSubFolder, params);
        String localFile = params[2];
        String remoteFolder = "";
        if (params.length >= 4) {
            remoteFolder = params[3];
        }
        int responseCode = RESPONSE_CODE_FAIL_DOWNLOAD;

        try {
            JSONObject resultObject = QiniuUtils.getFiles(remoteFolder, isGetSubFolder);
            if (resultObject != null) {
                Properties properties = new Properties();
                if (isGetSubFolder) {
                    JSONArray folders = resultObject.getJSONArray("commonPrefixes");
                    if (folders != null) {
                        for (int i = 0; i < folders.length(); i++) {
                            String folder = folders.getString(i);
                            String folderName = folder.substring(0, folder.length() - 1);
                            if (!folderName.equalsIgnoreCase(SceneCommonHelper.REMOTE_FOLDER_COMMON)) {
                                properties.put(folderName, "");
                            }
                        }
                    }
                } else {
                    JSONArray files = resultObject.getJSONArray("items");
                    if (files != null) {
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject file = files.getJSONObject(i);
                            String fileName = file.getString("key");
                            properties.put(fileName.substring(fileName.lastIndexOf("/") + 1), "");
                        }
                    }
                }

                properties.store(new FileOutputStream(localFile), "");
                responseCode = RESPONSE_CODE_PASS;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "getFiles exception = " + e.getMessage());
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void downloadFile(String... params) {
        super.downloadFile(params);
        String remoteFolder = params[2];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        String remoteFileName = params[3];
        String localFilePath = params[4];
        int responseCode = RESPONSE_CODE_FAIL_DOWNLOAD;

        if (QiniuUtils.downloadFile(remoteFolder + remoteFileName, localFilePath)) {
            responseCode = RESPONSE_CODE_PASS;
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }


    @Override
    protected void uploadFile(String... params) {
        super.uploadFile(params);
        final String fromTag = params[0];
        String localFile = params[2];
        String remoteFolder = params[3];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        String remoteFileName = params[4];

        File uploadFile = new File(localFile);
        //设置上传后文件的key
        String upkey = remoteFolder + remoteFileName;
        String uptoken = getUploadToken(upkey);
        uploadManager.put(uploadFile, upkey, uptoken, new UpCompletionHandler() {
            public void complete(String key, ResponseInfo rinfo, JSONObject response) {
                Log.i(TAG, "responseInfo = " + rinfo);
                int responseCode = RESPONSE_CODE_FAIL_UPLOAD;
                try {
                    if (rinfo.isOK()) {
                        responseCode = RESPONSE_CODE_PASS;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "uploadFile exception = " + e.getMessage());
                }
                for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
                    listener.onRequestResultChangedListener(fromTag, responseCode);
                }
            }
        }, new UploadOptions(null, null, false, new UpProgressHandler() {
            public void progress(String key, double percent) {
                Log.i(TAG, "uploadFile: " + key + "-> " + percent);
            }
        }, null));
    }

    @Override
    protected void uploadMultiFile(final String... params) {
        super.uploadMultiFile(params);
        final String[] localFile = params[2].split(";");
        final String remoteFolder = params[3].endsWith("/") ? params[3] : params[3] + "/";
        final String[] remoteFileName = params[4].split(";");

        new AsyncTask<String, Void, Void>() {
            int result = RESPONSE_CODE_FAIL_UPLOAD;
            boolean isUploading = true;

            @Override
            protected Void doInBackground(String... params) {
                for (int i = 0; i < localFile.length; i++) {
                    File uploadFile = new File(localFile[i]);
                    //设置上传后文件的key
                    String upkey = remoteFolder + remoteFileName[i];
                    isUploading = true;
                    String uptoken = getUploadToken(upkey);
                    uploadManager.put(uploadFile, upkey, uptoken, new UpCompletionHandler() {
                        public void complete(String key, ResponseInfo rinfo, JSONObject response) {
                            try {
                                if (rinfo.isOK()) {
                                    result = RESPONSE_CODE_PASS;
                                    isUploading = false;
                                } else {
                                    result = RESPONSE_CODE_FAIL_UPLOAD;
                                    isUploading = false;
                                    return;
                                }
                            } catch (Exception e) {
                                isUploading = false;
                                e.printStackTrace();
                                Log.i(TAG, "uploadFile exception = " + e.getMessage());
                            }

                        }
                    }, new UploadOptions(null, null, false, new UpProgressHandler() {
                        public void progress(String key, double percent) {
                            Log.i(TAG, "uploadFile: " + key + "-> " + percent);
                        }
                    }, null));

                    do {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (isUploading);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
                    listener.onRequestResultChangedListener(params[0], result);
                }
            }
        }.execute("");
    }

    @Override
    protected void createOrClearFolder(String... params) {
        super.createOrClearFolder(params);
        String commandTag = params[1];
        String remoteFolder = params[2];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        int responseCode = RESPONSE_CODE_FAIL_C2;


        if (commandTag.equals(REQUEST_ACTION_CREATEFOLDER)) {
            final String fromTag = params[0];
            //设置上传后文件的key
            String upkey = remoteFolder + "demo.txt";
            String uptoken = getUploadToken(upkey);
            uploadManager.put("demo".getBytes(), upkey, uptoken, new UpCompletionHandler() {
                public void complete(String key, ResponseInfo rinfo, JSONObject response) {
                    int responseCode = RESPONSE_CODE_FAIL_C2;
                    try {
                        if (rinfo.isOK()) {
                            responseCode = RESPONSE_CODE_PASS;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "createOrClearFolder exception = " + e.getMessage());
                    }
                    for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
                        listener.onRequestResultChangedListener(fromTag, responseCode);
                    }
                }
            }, new UploadOptions(null, null, false, new UpProgressHandler() {
                public void progress(String key, double percent) {
                    Log.i(TAG, "createOrClearFolder: " + key + "-> " + percent);
                }
            }, null));
        } else if (commandTag.equals(REQUEST_ACTION_CLEARALL)) {
            try {
                JSONObject resultObject = QiniuUtils.getFiles(remoteFolder, false);
                if (resultObject != null) {
                    JSONArray files = resultObject.getJSONArray("items");
                    if (files != null) {
                        boolean deleteResult = true;
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject file = files.getJSONObject(i);
                            String fileName = file.getString("key");
                            if (!fileName.endsWith("demo.txt")) {
                                // delete file
                                deleteResult &= QiniuUtils.deleteFile(fileName);
                            }
                        }
                        responseCode = deleteResult ? RESPONSE_CODE_PASS : RESPONSE_CODE_FAIL_C2;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "createOrClearFolder exception = " + e.getMessage());
            }

            for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
                listener.onRequestResultChangedListener(params[0], responseCode);
            }
        }
    }

    @Override
    protected void deleteOneFile(String... params) {
        super.deleteOneFile(params);
        String remoteFolder = params[2];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        String remoteFileName = params[3];
        int responseCode = RESPONSE_CODE_FAIL_DELONE;

        if (QiniuUtils.deleteFile(remoteFolder + remoteFileName)) {
            responseCode = RESPONSE_CODE_PASS_DELONE;
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void downAll(String... params) {
        super.downAll(params);
        String remoteFolder = params[2];
        String localFolder = params[3];
        int responseCode = RESPONSE_CODE_FAIL_DOWNALL;

        try {
            JSONObject resultObject = QiniuUtils.getFiles(remoteFolder, false);
            if (resultObject != null) {
                JSONArray files = resultObject.getJSONArray("items");
                if (files != null) {
                    boolean downloadResult = true;
                    for (int i = 0; i < files.length(); i++) {
                        JSONObject file = files.getJSONObject(i);
                        String fileName = file.getString("key");
                        downloadResult &= QiniuUtils.downloadFile(fileName, localFolder + "/" + fileName.substring(fileName.lastIndexOf("/") + 1));
                    }
                    if (downloadResult) {
                        responseCode = RESPONSE_CODE_PASS;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "getFiles exception = " + e.getMessage());
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void existFile(String... params) {
        super.existFile(params);
        String remoteFolder = params[2];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        String remoteFileName = params[3];
        int responseCode = RESPONSE_CODE_FAIL_EXISTFILE;

        if (QiniuUtils.existFile(remoteFolder + remoteFileName)) {
            responseCode = RESPONSE_CODE_PASS;
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void rename(String... params) {
        super.rename(params);
        String remoteFolder = params[2];
        if (!remoteFolder.endsWith("/")) {
            remoteFolder += "/";
        }
        String remoteFileNameFrom = params[3];
        String remoteFileNameTo = params[4];
        int responseCode = RESPONSE_CODE_FAIL_RENAME;

        if (QiniuUtils.rename(remoteFolder + remoteFileNameFrom, remoteFolder + remoteFileNameTo)) {
            responseCode = RESPONSE_CODE_PASS;
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }
}
