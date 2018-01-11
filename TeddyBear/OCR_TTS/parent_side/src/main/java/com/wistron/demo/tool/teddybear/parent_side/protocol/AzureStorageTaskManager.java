package com.wistron.demo.tool.teddybear.parent_side.protocol;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;
import com.microsoft.azure.storage.file.ListFileItem;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by king on 16-7-23.
 */
public class AzureStorageTaskManager extends BaseTaskManager {
    /**
     * Stores the storage connection string.
     */
    private static final String storageConnectionString =
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=wistron;" +
                    "AccountKey=0YsPd2AxX95eHdcqhemjl2LmM/M0L/bPTyWQylYNjAx/e0u2gtbZJ4Aw3iwcWCh9X9dUkCTN+jiedKBUme/JMg==";

    private static CloudFileDirectory mAzureStorageRootDir = null;

    private AzureStorageTaskManager() {
        blockingQueue = new LinkedBlockingDeque<>();
        mRequestResultChangedListeners = new ArrayList<>();
        new Thread(runNetworkRequest).start();
    }

    public static AzureStorageTaskManager getInstance(Context context) {
        if (mBaseTaskManager == null) {
            mBaseTaskManager = new AzureStorageTaskManager();
        }
        getRootDir();
        return (AzureStorageTaskManager) mBaseTaskManager;
    }

    private static void getRootDir() {
        if (mAzureStorageRootDir == null) {
            try {
                CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                // Create the file storage client.
                CloudFileClient fileClient = storageAccount.createCloudFileClient();
                // Get a reference to the file share
                CloudFileShare share = fileClient.getShareReference("teddybear");
                //Get a reference to the root directory for the share.
                mAzureStorageRootDir = share.getRootDirectoryReference();
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "AzureStorageTaskManager::getRootDir exception= " + e.getMessage());
            }
        }
    }

    @Override
    protected void getFiles(boolean isGetSubFolder, String... params) {
        super.getFiles(isGetSubFolder, params);
        String localFile = params[2];
        int responseCode = RESPONSE_CODE_FAIL_DOWNLOAD;

        getRootDir();
        if (mAzureStorageRootDir != null &&
                mAzureStorageRootDir.listFilesAndDirectories() != null) {
            try {
                Properties properties = new Properties();
                for (ListFileItem fileItem : mAzureStorageRootDir.listFilesAndDirectories()) {
                    if (fileItem instanceof CloudFileDirectory) {
                        String folderName = ((CloudFileDirectory) fileItem).getName();
                        if (!folderName.equalsIgnoreCase(CommonHelper.REMOTE_FOLDER_COMMON)) {
                            properties.put(folderName, "");
                        }
                    } else if (fileItem instanceof CloudFile) {
                        if (!isGetSubFolder) {
                            properties.put(((CloudFile) fileItem).getName(), "");
                        }
                    }
                }
                properties.store(new FileOutputStream(localFile), "");
                responseCode = RESPONSE_CODE_PASS;
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("King", "AzureStorageTaskManager::getFiles exception = " + e.getMessage());
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void downloadFile(String... params) {
        super.downloadFile(params);
        String remoteFolder = params[2];
        String remoteFileName = params[3];
        String localFilePath = params[4];
        int responseCode = RESPONSE_CODE_FAIL_DOWNLOAD;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                }
                if (tempDir != null && tempDir.exists()) {
                    File localFile = new File(localFilePath);
                    if (!localFile.exists()) {
                        if (!localFile.getParentFile().exists()) {
                            localFile.getParentFile().mkdirs();
                        }
                        localFile.createNewFile();
                    }

                    CloudFile tempFile = tempDir.getFileReference(remoteFileName);
                    tempFile.downloadToFile(localFilePath);
                    Log.i("King", "AzureStorageTaskManager::downloadFile result = true");
                    responseCode = RESPONSE_CODE_PASS;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void uploadFile(String... params) {
        super.uploadFile(params);
        String localFile = params[2];
        String remoteFolder = params[3];
        String remoteFileName = params[4];
        int responseCode = RESPONSE_CODE_FAIL_UPLOAD;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    String[] folders = remoteFolder.split(File.separator);
                    for (String folder : folders) {
                        if (!TextUtils.isEmpty(folder)) {
                            tempDir = tempDir.getDirectoryReference(folder);
                            tempDir.createIfNotExists();
                        }
                    }
                }
                CloudFile cloudFile = tempDir.getFileReference(remoteFileName);
                cloudFile.uploadFromFile(localFile);
                responseCode = RESPONSE_CODE_PASS;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void uploadMultiFile(String... params) {
        super.uploadMultiFile(params);
        String[] localFile = params[2].split(";");
        String remoteFolder = params[3];
        String[] remoteFileName = params[4].split(";");
        int responseCode = RESPONSE_CODE_FAIL_UPLOAD;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    String[] folders = remoteFolder.split(File.separator);
                    for (String folder : folders) {
                        if (!TextUtils.isEmpty(folder)) {
                            tempDir = tempDir.getDirectoryReference(folder);
                            tempDir.createIfNotExists();
                        }
                    }
                }
                for (int i = 0; i < localFile.length; i++) {
                    CloudFile cloudFile = tempDir.getFileReference(remoteFileName[i]);
                    cloudFile.uploadFromFile(localFile[i]);
                }
                responseCode = RESPONSE_CODE_PASS;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void createOrClearFolder(String... params) {
        super.createOrClearFolder(params);
        String commendTag = params[1];
        String remoteFolder = params[2];
        int responseCode = RESPONSE_CODE_FAIL_C2;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                switch (commendTag) {
                    case REQUEST_ACTION_CREATEFOLDER:
                        Log.i("berlin", "TaskManager create Folder");
                        String[] folders = remoteFolder.split(File.separator);
                        for (String folder : folders) {
                            if (!TextUtils.isEmpty(folder)) {
                                tempDir = tempDir.getDirectoryReference(folder);
                                tempDir.createIfNotExists();
                            }
                        }
                        responseCode = RESPONSE_CODE_PASS;
                        break;
                    case REQUEST_ACTION_CLEARALL:
                        Log.i("berlin", "TaskManager clear All file ");
                        if (!TextUtils.isEmpty(remoteFolder)) {
                            tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                            for (ListFileItem fileItem : tempDir.listFilesAndDirectories()) {
                                if (fileItem instanceof CloudFileDirectory) {
                                    CloudFile tempFile = tempDir.getFileReference(((CloudFileDirectory) fileItem).getName());
                                    tempFile.deleteIfExists();
                                }
                            }
                        }
                        responseCode = RESPONSE_CODE_PASS;
                        break;
                    default:
                        break;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void deleteOneFile(String... params) {
        super.deleteOneFile(params);
        String remoteFolder = params[2];
        String remoteFileName = params[3];
        int responseCode = RESPONSE_CODE_FAIL_DELONE;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                }
                CloudFile tempFile = tempDir.getFileReference(remoteFileName);
                tempFile.deleteIfExists();
                Log.i("King", "AzureStorageTaskManager::deleteOneFile result = true");
                responseCode = RESPONSE_CODE_PASS_DELONE;
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            }
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

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                }
                if (tempDir != null && tempDir.exists()) {
                    for (ListFileItem fileItem : tempDir.listFilesAndDirectories()) {
                        if (fileItem instanceof CloudFile) {
                            String fileName = ((CloudFile) fileItem).getName();
                            File localFile = new File(localFolder + fileName);
                            if (!localFile.exists()) {
                                if (!localFile.getParentFile().exists()) {
                                    localFile.getParentFile().mkdirs();
                                }
                                localFile.createNewFile();
                            }
                            if ((!localFile.exists()) || fileName.equals("message.txt")) {
                                CloudFile tempFile = tempDir.getFileReference(fileName);
                                tempFile.downloadToFile(localFile.getAbsolutePath());
                            }
                        }
                    }
                    Log.i("King", "AzureStorageTaskManager::downAll result = true");
                    responseCode = RESPONSE_CODE_PASS;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void existFile(String... params) {
        super.existFile(params);
        String remoteFolder = params[2];
        String remoteFileName = params[3];
        int responseCode = RESPONSE_CODE_FAIL_EXISTFILE;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                }
                CloudFile tempFile = tempDir.getFileReference(remoteFileName);
                boolean isExist = tempFile.exists();
                Log.i("King", "AzureStorageTaskManager::deleteOneFile result = " + isExist);
                if (isExist) {
                    responseCode = RESPONSE_CODE_PASS;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }

    @Override
    protected void rename(String... params) {
        super.rename(params);
        String remoteFolder = params[2];
        String remoteFileNameFrom = params[3];
        String remoteFileNameTo = params[4];
        int responseCode = RESPONSE_CODE_FAIL_RENAME;

        getRootDir();
        if (mAzureStorageRootDir != null) {
            CloudFileDirectory tempDir = mAzureStorageRootDir;
            try {
                if (!TextUtils.isEmpty(remoteFolder)) {
                    tempDir = mAzureStorageRootDir.getDirectoryReference(remoteFolder);
                }
                CloudFile fromFile = tempDir.getFileReference(remoteFileNameFrom);
                CloudFile toFile = tempDir.getFileReference(remoteFileNameTo);
                if (fromFile.exists()) {
                    toFile.startCopy(fromFile);
                    fromFile.deleteIfExists();
                    responseCode = RESPONSE_CODE_PASS;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (StorageException e) {
                e.printStackTrace();
            }
        }

        for (OnRequestResultChangedListener listener : mRequestResultChangedListeners) {
            listener.onRequestResultChangedListener(params[0], responseCode);
        }
    }
}
