package com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts;

import com.wistron.demo.tool.teddybear.dcs.devicemodule.alerts.message.Alert;
import com.wistron.demo.tool.teddybear.dcs.util.DateFormatterUtil;

import org.litepal.util.LogUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 个的闹铃/提醒计时器
 * Created by ivanjlzhang on 17-9-21.
 */

public class AlertScheduler extends Timer {
    private static final String TAG = "AlertScheduler";
    private final Alert alert;
    private final AlertHandler handler;
    private boolean active = false;

    public AlertScheduler(final Alert alert, final AlertHandler handler) {
        super();
        String scheduledTimeStr = alert.getScheduledTime();
        if (scheduledTimeStr != null && scheduledTimeStr.length() > 0) {
            try {
                Date date = DateFormatterUtil.toDate(alert.getScheduledTime());
                long scheduledTime = date.getTime();
                long delay = scheduledTime - System.currentTimeMillis();
                LogUtil.d(TAG, "alert-delay start:" + delay);
                if (delay > 0) {
                    this.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // 时间到了，开始闹铃
                            setActive(true);
                            handler.startAlert(alert.getToken());
                        }
                    }, delay);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }
        this.alert = alert;
        this.handler = handler;
    }

    public synchronized boolean isActive() {
        return active;
    }

    public synchronized void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (isActive()) {
            handler.stopAlert(alert.getToken());
            setActive(false);
        }
    }

    public Alert getAlert() {
        return alert;
    }
}
