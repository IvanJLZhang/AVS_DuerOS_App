package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wistron.demo.tool.teddybear.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.helper.ToSpeak;

import org.json.JSONArray;

import java.text.SimpleDateFormat;

/**
 * Created by king on 16-4-11.
 */
public abstract class SceneBase {
    public static final String TAG = "King";

    protected Context context;
    protected Handler mMainHandler;
    protected String sceneAction;
    protected JSONArray sceneParams;

    private ToSpeak mToSpeak;
    protected boolean isSceneStopped;

    protected SimpleDateFormat yyyyMMddDateFormat;

    public SceneBase(Context context, Handler mMainHandler) {
        this(context, mMainHandler, null, null);
    }

    public SceneBase(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        this.context = context;
        this.mMainHandler = mMainHandler;
        mToSpeak = ToSpeak.getInstance(context);
        this.sceneAction = sceneAction;
        this.sceneParams = params;

        yyyyMMddDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public void resetSceneActionAndParams(String sceneAction, JSONArray params) {
        this.sceneAction = sceneAction;
        this.sceneParams = params;
    }

    protected void updateLog(String content) {
        Message message = mMainHandler.obtainMessage(SceneActivity.MSG_UPDATE_LOG);
        message.obj = content;
        mMainHandler.sendMessage(message);
    }

    protected void toSpeak(String content) {
        toSpeak(content, true);
    }

    protected void toSpeak(String content, boolean isAsyncToSpeak) {
        updateLog("\n" + content);
        mToSpeak.toSpeak(content, isAsyncToSpeak);
    }

    protected String getString(int stringId) {
        return SceneCommonHelper.getString(context, stringId);
    }

    protected String[] getStringArray(int stringId) {
        return SceneCommonHelper.getStringArray(context, stringId);
    }

    public void pause() {

    }

    public void resume() {

    }

    public void stop() {
        context.sendBroadcast(new Intent(CommonHelper.ACTION_KEYWORD_DETECTED));
        isSceneStopped = true;
        mToSpeak.stop();
        SceneCommonHelper.closeLED();
    }

    protected void toSpeakThenDone(String content) {
        Log.i("King", "SceneBase to read: " + content);
        toSpeak(content, false);
        SceneCommonHelper.closeLED();
    }

    public void simulate() {
        isSceneStopped = false;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void updateSttResult(String result) {
    }

}
