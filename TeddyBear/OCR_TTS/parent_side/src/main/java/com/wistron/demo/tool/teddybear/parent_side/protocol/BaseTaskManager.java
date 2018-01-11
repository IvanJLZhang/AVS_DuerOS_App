package com.wistron.demo.tool.teddybear.parent_side.protocol;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;

/**
 * Created by king on 16-7-23.
 */
public class BaseTaskManager {
    public static final String REQUEST_TAG_BERLIN = "berlin";
    public static final String REQUEST_TAG_BOB = "bob";
    public static final String REQUEST_TAG_BOB_SERVICE = "bob_service";
    public static final String REQUEST_TAG_KING = "king";

    public static final String REQUEST_ACTION_DOWNLOAD = "download";
    public static final String REQUEST_ACTION_UPLOAD = "upload";
    public static final String REQUEST_ACTION_MUTLI = "multi";
    public static final String REQUEST_ACTION_CREATEFOLDER = "createFolder";
    public static final String REQUEST_ACTION_CLEARALL = "clearall";
    public static final String REQUEST_ACTION_DELONE = "delelteOne";
    public static final String REQUEST_ACTION_EXISTFILE = "existfile";
    public static final String REQUEST_ACTION_RENAME = "rename";
    public static final String REQUEST_ACTION_DOWNALL = "downAll";
    public static final String REQUEST_ACTION_GET_FILES = "get_files";
    public static final String REQUEST_ACTION_GET_SUB_FOLDERS = "get_sub_folders";

    public static final int RESPONSE_CODE_PASS = 0;
    public static final int RESPONSE_CODE_FAIL_CONNECT = 1;
    public static final int RESPONSE_CODE_FAIL_DOWNLOAD = 2;
    public static final int RESPONSE_CODE_FAIL_UPLOAD = 3;
    public static final int RESPONSE_CODE_FAIL_C2 = 4;
    public static final int RESPONSE_CODE_FAIL_MULTI = 5;
    public static final int RESPONSE_CODE_FAIL_DELONE = 6;
    public static final int RESPONSE_CODE_FAIL_EXISTFILE = 7;
    public static final int RESPONSE_CODE_FAIL_RENAME = 8;
    public static final int RESPONSE_CODE_FAIL_DOWNALL = 9;
    public static final int RESPONSE_CODE_PASS_DELONE = 10;

    private static String mCurrentTag;

    protected static ArrayList<OnRequestResultChangedListener> mRequestResultChangedListeners;
    protected static BlockingQueue<String[]> blockingQueue;

    protected static BaseTaskManager mBaseTaskManager;

    public boolean addNetworkRequest(String... params) {
        synchronized (blockingQueue) {
            boolean isRequestExist = false;
            try {
                Iterator<String[]> elements = blockingQueue.iterator();
                while (elements.hasNext()) {
                    String[] element = elements.next();
                    if (element[0].equals(params[0]) || element[0].equals(mCurrentTag)) {
                        isRequestExist = true;
                        break;
                    }
                }
                if (!isRequestExist) {
                    blockingQueue.put(params);
                    Log.i("King", "BaseTaskManager: add a request= " + params[0]);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return isRequestExist;
        }
    }

    public void addAzureStorageChangedListener(OnRequestResultChangedListener listener) {
        synchronized (blockingQueue) {
            mRequestResultChangedListeners.add(listener);
        }
    }

    public void removeAzureStorageChangedListener(OnRequestResultChangedListener listener) {
        synchronized (blockingQueue) {
            mRequestResultChangedListeners.remove(listener);
        }
    }

    public abstract interface OnRequestResultChangedListener {
        public abstract void onRequestResultChangedListener(String tag, int responseCode);
    }

    protected Runnable runNetworkRequest = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    mCurrentTag = null;
                    String[] request = blockingQueue.take();
                    Log.i("King", "Network request: " + Arrays.toString(request));
                    mCurrentTag = request[0];

                    String action = request[1];
                    switch (action) {
                        case REQUEST_ACTION_DOWNLOAD:
                            downloadFile(request);
                            break;
                        case REQUEST_ACTION_UPLOAD:
                            uploadFile(request);
                            break;
                        case REQUEST_ACTION_CLEARALL:
                            createOrClearFolder(request);
                            break;
                        case REQUEST_ACTION_CREATEFOLDER:
                            createOrClearFolder(request);
                            break;
                        case REQUEST_ACTION_DELONE:
                            deleteOneFile(request);
                            break;
                        case REQUEST_ACTION_DOWNALL:
                            downAll(request);
                            break;
                        case REQUEST_ACTION_EXISTFILE:
                            existFile(request);
                            break;
                        case REQUEST_ACTION_RENAME:
                            rename(request);
                            break;
                        case REQUEST_ACTION_MUTLI:
                            uploadMultiFile(request);
                            break;
                        case REQUEST_ACTION_GET_FILES:
                            getFiles(false, request);
                            break;
                        case REQUEST_ACTION_GET_SUB_FOLDERS:
                            getFiles(true, request);
                            break;
                        default:
                            break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    protected void downloadFile(String... params) {
    }

    protected void uploadFile(String... params) {
    }

    protected void createOrClearFolder(String... params) {
    }

    protected void deleteOneFile(String... params) {
    }

    protected void downAll(String... params) {
    }

    protected void existFile(String... params) {
    }

    protected void rename(String... params) {
    }

    protected void uploadMultiFile(String... params) {
    }

    protected void getFiles(boolean isGetSubFolder, String... params) {
    }
}
