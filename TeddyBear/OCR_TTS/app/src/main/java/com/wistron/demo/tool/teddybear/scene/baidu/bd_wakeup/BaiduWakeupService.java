package com.wistron.demo.tool.teddybear.scene.baidu.bd_wakeup;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Map;

public class BaiduWakeupService extends Service {
    public BaiduWakeupService() {
    }
    BDWakeupService bdWakeupService;
    public void setBdWakeupService(BDWakeupService bdWakeupService) {
        this.bdWakeupService = bdWakeupService;
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
//        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return mBinder;
    }
    private WakeupBinder mBinder = new WakeupBinder();
    public class WakeupBinder extends Binder{
        public BaiduWakeupService getService(){return BaiduWakeupService.this;}
    }

    public void start(final Map<String, Object> params){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                bdWakeupService.start(params);
//            }
//        }).start();
    }

    public void stop(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                bdWakeupService.stop();
//            }
//        }).start();
    }

    public void release(){
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
                bdWakeupService.release();
                stopSelf();
//            }
//        }).start();
    }
}
