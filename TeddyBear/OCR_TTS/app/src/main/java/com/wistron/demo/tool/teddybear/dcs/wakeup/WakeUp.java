package com.wistron.demo.tool.teddybear.dcs.wakeup;

import com.wistron.demo.tool.teddybear.dcs.systeminterface.IAudioRecord;
import com.wistron.demo.tool.teddybear.dcs.systeminterface.IWakeUp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ivanjlzhang on 17-9-22.
 */

public class WakeUp {
    private IWakeUp iWakeUp;
    private List<IWakeUp.IWakeUpListener> wakeUpListeners;
    private IAudioRecord iAudioRecord;

    public WakeUp(IWakeUp iWakeUp, IAudioRecord iAudioRecord) {
        this.iWakeUp = iWakeUp;
        this.iAudioRecord = iAudioRecord;
        this.wakeUpListeners = Collections.synchronizedList(new LinkedList<IWakeUp.IWakeUpListener>());
        this.iWakeUp.addWakeUpListener(new IWakeUp.IWakeUpListener() {
            @Override
            public void onWakeUpSucceed() {
                fireOnWakeUpSucceed();
            }
        });
        // 启动音频采集
        this.iAudioRecord.startRecord();
    }

    private void fireOnWakeUpSucceed() {
        for (IWakeUp.IWakeUpListener listener : wakeUpListeners) {
            listener.onWakeUpSucceed();
        }
    }

    /**
     * 开始唤醒，麦克风处于打开状态，一旦检测到有音频开始唤醒解码
     */
    public void startWakeUp() {
        iWakeUp.startWakeUp();
    }

    /**
     * 停止唤醒，关闭麦克风
     */
    public void stopWakeUp() {
        iWakeUp.stopWakeUp();
    }

    /**
     * 释放资源-解码so库资源
     */
    public void releaseWakeUp() {
        iAudioRecord.stopRecord();
        iWakeUp.releaseWakeUp();
    }

    /**
     * 添加唤醒成功后的监听
     */
    public void addWakeUpListener(IWakeUp.IWakeUpListener listener) {
        wakeUpListeners.add(listener);
    }

    public void removeWakeUpListener(IWakeUp.IWakeUpListener listener) {
        if (wakeUpListeners.contains(listener)) {
            wakeUpListeners.remove(listener);
        }
    }
}
