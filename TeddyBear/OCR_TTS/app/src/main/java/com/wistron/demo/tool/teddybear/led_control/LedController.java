package com.wistron.demo.tool.teddybear.led_control;

import android.content.Context;
import android.util.Log;

import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;


/**
 * Created by aaron on 16-8-10.
 */
public class LedController {
    private static final String TAG = "LedController";
    private static LightLed mLightLed = new LightLed();

    private LedController() {
    }

    /**
     * Light led in given color.
     *
     * @param color Indicates the color of the lighting led. See {@link LedController.COLORS}.
     */
    public static void lightLed(int color) {
        //mLightLed.lightLed(color);
        //use shell to light
        mLightLed.lightShell();
    }

    /**
     * Light led and highlight in given degree.
     *
     * @param color     Indicates the background color. See {@link LedController.COLORS}.
     * @param highColor Indicates the highlight color. See {@link LedController.COLORS}.
     * @param degree    Indicates the degree of the highlight led degree.
     */
    public static void lightDegreeLed(int color, int highColor, int degree) {
        mLightLed.lightDegreeLed(color, highColor, degree);
    }

    public static void turnOnHighlightLed(int degreeIndex) {
        mLightLed.turnOnHighlightLed(degreeIndex);
    }

    public static void turnOffHighLightLed() {
        mLightLed.turnOffHighLightLed();
    }

    /**
     * Flashing led
     *
     * @param color      Indicates the color of the flashing led background. See {@link LedController.COLORS}.
     * @param flashColor Indicates the color of the flashing led. See {@link LedController.COLORS}.
     */
    public static void lightFlashLed(int color, int flashColor) {
        mLightLed.lightFlashLed(color, flashColor);
    }

    /**
     * Rotating led.
     *
     * @param color Indicates the color of the rotating led. See {@link LedController.COLORS}.
     */
    public static void lightRotateLed(int color) {
        mLightLed.lightRotateLed(color);
    }

    /**
     * Close all led.
     */
    public static void closeLed(Context context) {
        boolean isPandoMicroMute = SceneCommonHelper.getPandoMicroMute(context);
        Log.i(TAG, "LedController:: isPandoMicroMute = " + isPandoMicroMute);
        if (isPandoMicroMute) {
            mLightLed.lightLed(COLORS.RED);
        } else {
            Log.i(TAG, "LedController:: close led");
            // mLightLed.lightLed(0);
            mLightLed.closeAll();
        }
    }


    /**
     * Indicates colors, used in {@link LightLed} color array.
     */
    public class COLORS {
        public static final int RED = 1;
        public static final int GREEN = 2;
        public static final int BLUE = 3;
        public static final int YELLOW = 4;
        public static final int PURPLE = 5;
        public static final int PINK = 6;
        public static final int ORANGE = 7;
    }
}
