package com.wistron.demo.tool.teddybear.dcs.systeminterface;

/**
 * 唤醒接口
 * Created by ivanjlzhang on 17-9-22.
 */

public interface IWakeUp {
    /**
     * 开始唤醒
     * 1.初始化唤醒词
     * 2.打开麦克风，并开始音频唤醒的解码
     */
    void startWakeUp();

    /**
     * 停止唤醒，调用停止录音
     */
    void stopWakeUp();

    /**
     * 释放资源，比如调用底层库释放资源等
     */
    void releaseWakeUp();

    /**
     * 唤醒结果的回调监听
     *
     * @param listener 监听实现
     */
    void addWakeUpListener(IWakeUpListener listener);

    /**
     * 唤醒结果的回调接口
     */
    interface IWakeUpListener {
        /**
         * 唤醒成功后回调
         */
        void onWakeUpSucceed();
    }
}
