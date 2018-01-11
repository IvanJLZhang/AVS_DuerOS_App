package com.wistron.demo.tool.teddybear.dcs.systemimpl.player;

import java.io.InputStream;

/**
 * mp3流保存接口
 * Created by ivanjlzhang on 17-9-23.
 */

public interface IAudioStreamStore {
    /**
     * 保存mp3流到文件中(存储设备中)
     */
    void save(InputStream inputStream);

    /**
     * 取消保存
     */
    void cancel();

    /**
     * 播报完成后处理的操作，比如删除保存的文件
     */
    void speakAfter();

    /**
     * 保存回调监听
     *
     * @param listener listener
     */
    void setOnStoreListener(OnStoreListener listener);

    /**
     * 保存回调接口
     */
    interface OnStoreListener {
        void onStart();

        void onComplete(String path);

        void onError(String errorMessage);
    }

    class SimpleOnStoreListener implements OnStoreListener {
        @Override
        public void onStart() {
        }

        @Override
        public void onComplete(String path) {
        }

        @Override
        public void onError(String errorMessage) {
        }
    }
}
