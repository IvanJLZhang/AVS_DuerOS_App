package com.wistron.demo.tool.teddybear.dcs.systemimpl.alert;

import android.os.Handler;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message.Alert;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAlertDataStore;
import com.wistron.demo.tool.teddybear.dcs.util.CommonUtil;
import com.wistron.demo.tool.teddybear.dcs.util.FileUtil;
import com.wistron.demo.tool.teddybear.dcs.util.LogUtil;
import com.wistron.demo.tool.teddybear.dcs.util.ObjectMapperUtil;

import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.type.TypeReference;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by ivanjlzhang on 17-9-23.
 */

public class AlertsFileDataStoreImpl implements IAlertDataStore {
    private static final String TAG = AlertsFileDataStoreImpl.class.getSimpleName();
    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler();

    public AlertsFileDataStoreImpl() {
    }

    @Override
    public void readFromDisk(final IAlertDataStore.ReadResultListener listener) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                FileReader fis;
                BufferedReader br = null;
                ObjectReader reader = ObjectMapperUtil.instance()
                        .getObjectReader()
                        .withType(new TypeReference<List<Alert>>() {
                        });
                try {
                    File file = FileUtil.getAlarmFile();
                    if (file == null) {
                        postReadFailed(listener, "create file failed file is null");
                        return;
                    }
                    fis = new FileReader(file);
                    br = new BufferedReader(fis);
                    List<Alert> alerts = reader.readValue(br);
                    postReadSucceed(listener, alerts);
                } catch (final FileNotFoundException e) {
                    postReadFailed(listener, "Failed to load alerts from disk.");
                } catch (IOException e) {
                    postReadFailed(listener, e.getMessage());
                } finally {
                    CommonUtil.closeQuietly(br);
                }
            }
        });
    }

    private void postReadFailed(final IAlertDataStore.ReadResultListener listener, final String errorMessage) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFailed(errorMessage);
                }
            });
        }
    }

    private void postReadSucceed(final IAlertDataStore.ReadResultListener listener, final List<Alert> alerts) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSucceed(alerts);
                }
            });
        }
    }

    @Override
    public void writeToDisk(final List<Alert> alerts, final IAlertDataStore.WriteResultListener listener) {
        mExecutor.execute(new Runnable() {

            @Override
            public void run() {
                ObjectWriter writer = ObjectMapperUtil.instance().getObjectWriter();
                PrintWriter out = null;
                try {
                    File file = FileUtil.getAlarmFile();
                    if (file == null) {
                        postWriteFailed(listener, "create file failed file is null ");
                        return;
                    }
                    out = new PrintWriter(file);
                    out.print(writer.writeValueAsString(alerts));
                    out.flush();
                    LogUtil.e(TAG, "start postWriteSucceed");
                    postWriteSucceed(listener);
                } catch (IOException e) {
                    LogUtil.e(TAG, "Failed to write to disk", e);
                    postWriteFailed(listener, "Failed to write to disk,error:" + e.getMessage());
                } finally {
                    CommonUtil.closeQuietly(out);
                }
            }
        });
    }

    private void postWriteFailed(final IAlertDataStore.WriteResultListener listener, final String errorMessage) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFailed(errorMessage);
                }
            });
        }
    }

    private void postWriteSucceed(final IAlertDataStore.WriteResultListener listener) {
        if (listener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onSucceed();
                }
            });
        }
    }
}
