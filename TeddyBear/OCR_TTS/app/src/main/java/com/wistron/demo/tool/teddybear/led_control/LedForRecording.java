package com.wistron.demo.tool.teddybear.led_control;

import android.content.Context;
import android.os.AsyncTask;

import com.wistron.demo.tool.teddybear.ocr_tts.helper.WisShellCommandHelper;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;
import com.wistron.demo.tool.teddybear.scene.ssr_helper.SurroundSoundRecording;

import java.io.File;

/**
 * Created by king on 16-9-14.
 */

public class LedForRecording {
    // Pando Led light
    private static final String PANDO_BLUE_LED_PATH = "/sys/class/leds/lamp:b/brightness";
    private static final String PANDO_RED_LED_PATH = "/sys/class/leds/lamp:r/brightness";
    private static final String PANDO_GREEN_LED_PATH = "/sys/class/leds/lamp:g/brightness";

    public static void recordingStart(Context context) {
        LedController.lightLed(LedController.COLORS.BLUE);
        SurroundSoundRecording.getInstance(context).startTracking();

        // just for pando
        if (new File(PANDO_GREEN_LED_PATH).exists()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void[] params) {
                    WisShellCommandHelper mShellCommandHelper = new WisShellCommandHelper();
                    mShellCommandHelper.exec("echo 255 > " + PANDO_GREEN_LED_PATH);
                    return null;
                }
            }.executeOnExecutor(SceneCommonHelper.mCachedThreadPool);
        }
    }

    public static void recordingStop(Context context) {
        SurroundSoundRecording.getInstance(context).stopTracking();
        LedController.closeLed(context);

        // just for pando
        if (new File(PANDO_GREEN_LED_PATH).exists()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void[] params) {
                    WisShellCommandHelper mShellCommandHelper = new WisShellCommandHelper();
                    mShellCommandHelper.exec("echo 0 > " + PANDO_GREEN_LED_PATH);
                    return null;
                }
            }.executeOnExecutor(SceneCommonHelper.mCachedThreadPool);
        }
    }

    public static void turnOffPandoBlueLight() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void[] params) {
                WisShellCommandHelper mShellCommandHelper = new WisShellCommandHelper();
                mShellCommandHelper.exec("echo 0 > " + PANDO_BLUE_LED_PATH);
                return null;
            }
        }.executeOnExecutor(SceneCommonHelper.mCachedThreadPool);
    }
}
