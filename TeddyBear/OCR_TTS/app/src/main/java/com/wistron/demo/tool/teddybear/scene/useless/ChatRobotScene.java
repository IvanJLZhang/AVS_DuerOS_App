package com.wistron.demo.tool.teddybear.scene.useless;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.wistron.demo.tool.teddybear.scene.luis_scene.SceneBase;

/**
 * Created by king on 16-4-27.
 */
public class ChatRobotScene extends SceneBase {

    public ChatRobotScene(Context context, Handler mMainHandler) {
        super(context, mMainHandler);
    }

    @Override
    public void simulate() {
        super.simulate();
        Intent chatRobotIntent = new Intent(context, AssistantActivity.class);
        context.startActivity(chatRobotIntent);
    }
}
