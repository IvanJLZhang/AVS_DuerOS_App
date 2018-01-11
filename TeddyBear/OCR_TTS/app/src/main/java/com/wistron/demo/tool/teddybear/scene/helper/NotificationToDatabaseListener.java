package com.wistron.demo.tool.teddybear.scene.helper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.table.EmailTable;

import java.util.Date;

/**
 * Created by king on 16-9-1.
 */

public class NotificationToDatabaseListener extends NotificationListenerService {
    private static final String TAG = "NotificationToDatabase";

    private final String NOTIFICATION_KEY_APPLICATIONINFO = "android.rebuild.applicationInfo";  // ApplicationInfo
    private final String NOTIFICATION_KEY_TEMPLATE = "android.template"; // Notification.Style
    private final String NOTIFICATION_KEY_TITLE = "android.title";  // Sender
    private final String NOTIFICATION_KEY_SUBTEXT = "android.subText";  // Recipient
    private final String NOTIFICATION_KEY_TEXT = "android.text";  // Subject
    private final String NOTIFICATION_KEY_BIGTEXT = "android.bigText";  // Content

    private Handler removeNotification = new Handler() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                cancelAllNotifications();
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            //cancelNotification((String) msg.obj);
            //Log.i(TAG, "remove the msg: "+msg.obj);
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        Log.i(TAG, "onNotificationPosted.......");

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        Log.i(TAG, "------------------------>>>>>>>>>---------------------------");
        Log.i(TAG, "" + sbn.getGroupKey() + ", " + sbn.getKey() + ", " + sbn.getTag() + ", " + sbn.getId() + ", " + (new Date(sbn.getPostTime())));
        Log.i(TAG, "bundle = start [");
        for (String key : extras.keySet()) {
            Log.i(TAG, "key = " + key + ", value = " + extras.get(key));
        }
        Log.i(TAG, "] end");

        if (extras.containsKey(NOTIFICATION_KEY_APPLICATIONINFO)) {
            ApplicationInfo applicationInfo = (ApplicationInfo) extras.get(NOTIFICATION_KEY_APPLICATIONINFO);
            String template = extras.getString(NOTIFICATION_KEY_TEMPLATE);
            Log.i(TAG, "packageName = " + applicationInfo.packageName);
            if ((applicationInfo.packageName.equals("com.android.email")  // E-mail
                    || applicationInfo.packageName.equals("com.google.android.gm")) // G-mail
                    && (template.equals("android.app.Notification$BigTextStyle") || template.equals("android.app.Notification$BigPictureStyle"))) {
                EmailTable mail = new EmailTable();
                mail.setSender(extras.getString(NOTIFICATION_KEY_TITLE).toLowerCase());
                mail.setRecipient(extras.getString(NOTIFICATION_KEY_SUBTEXT).toLowerCase());
                mail.setSubject(("" + extras.get(NOTIFICATION_KEY_TEXT)).toLowerCase());
                String content = "" + extras.get(NOTIFICATION_KEY_BIGTEXT);
                mail.setContent(content.substring(content.indexOf("\n") + 1));
                mail.setReceiveDate(sbn.getPostTime());
                mail.setNotificationID(sbn.getId());
                mail.saveIfNotExist("notificationid = ?", String.valueOf(sbn.getId()));
            }
        }

        removeNotification.removeMessages(0);
        Message message = new Message();
        message.what = 0;
        message.obj = sbn.getKey();
        removeNotification.sendMessageDelayed(message, 30 * 1000);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
