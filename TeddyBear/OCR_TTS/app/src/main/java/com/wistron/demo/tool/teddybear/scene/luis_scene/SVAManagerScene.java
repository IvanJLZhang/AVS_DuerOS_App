package com.wistron.demo.tool.teddybear.scene.luis_scene;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.SceneActivity;
import com.wistron.demo.tool.teddybear.scene.helper.LuisHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONArray;
import org.json.JSONException;

public class SVAManagerScene extends SceneBase {

    public SVAManagerScene(Context context, Handler mMainHandler, String sceneAction, JSONArray params) {
        super(context, mMainHandler, sceneAction, params);
    }

    @Override
    public void simulate() {
        SceneCommonHelper.openLED();
        super.simulate();
        try {
            String entityType = sceneParams.getJSONObject(0).getString(LuisHelper.TAG_TYPE);
            Log.v("SVA", "in SVA Scene. entityType = " + entityType);
            if (!TextUtils.isEmpty(entityType)) {
                switch (entityType) {
                    case LuisHelper.ENTITIES_TYPE_SVA_CREATE:
                        createNewSM();
                        break;
                    case LuisHelper.ENTITIES_TYPE_SVA_DELETE:
                        deleteThisSM();
                        break;
                    default:
                        toSpeak("I do not understand in this scene.", false);
                        break;
                }
            } else {
                toSpeak("I can not figure out the meaning. Please try again.", false);
                stop();
            }
        } catch (JSONException e) {
            e.printStackTrace();
            toSpeak("I can not figure out the meaning. Please try again.", false);
            stop();
        }
    }

    @Override
    public void stop() {
        Log.v("SVA", "stop...");
        SceneCommonHelper.closeLED();
        super.stop();
    }

    private void createNewSM() {
        mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_SVA_VERIFY_RECORD_TASK));
//        mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_SVA_GET_USER_NAME));
    }

    private void deleteThisSM() {
        mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_SVA_DELETE_THIS_SM));
    }

    @Override
    public void updateSttResult(String result) {
        super.updateSttResult(result);
//        JSONObject toJason;
//        try {
//            toJason = new JSONObject(result);
//            String getName = toJason.getString(LuisHelper.TAG_QUERY);
//            mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_SVA_GET_USER_NAME, getName));
        mMainHandler.sendMessage(mMainHandler.obtainMessage(SceneActivity.MSG_SVA_GET_USER_NAME, result));
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }
}
