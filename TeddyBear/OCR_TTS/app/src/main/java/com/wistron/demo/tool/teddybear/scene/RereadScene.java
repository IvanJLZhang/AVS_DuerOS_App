package com.wistron.demo.tool.teddybear.scene;

import android.content.Context;
import android.os.Handler;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.RereadAudioRecordHelper;
import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;

/**
 * Created by king on 16-5-12.
 */
public class RereadScene extends SceneBase {
    private RereadAudioRecordHelper mAudioRecord;

    public RereadScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    @Override
    public void simulate() {
        super.simulate();
        /*Intent reReadSceneIntent = new Intent(context, RereadActivity.class);
        context.startActivity(reReadSceneIntent);*/
        updateLog(context.getString(R.string.reread_status_tips));
    }

    public void startRecord() {
        if (mAudioRecord == null) {
            mAudioRecord = new RereadAudioRecordHelper(context, mMainHandler);
            mAudioRecord.initial();
        }
        mAudioRecord.startRecord("reread.wav");
    }

    public void stopRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.stopRecord();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mAudioRecord != null) {
            mAudioRecord.exit();
        }
    }
}
