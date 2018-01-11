package com.wistron.demo.tool.teddybear.dcs.systeminterface;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message.Alert;

import java.util.List;

/**
 * alert 存储的回调接口
 * Created by ivanjlzhang on 17-9-21.
 */

public interface IAlertDataStore {
    void readFromDisk(ReadResultListener listener);

    void writeToDisk(List<Alert> alerts, WriteResultListener listener);

    /**
     * 读取回调
     */
    interface ReadResultListener {
        void onSucceed(List<Alert> alerts);

        void onFailed(String errMsg);
    }

    /**
     * 写入回调
     */
    interface WriteResultListener {
        void onSucceed();

        void onFailed(String errMsg);
    }
}
