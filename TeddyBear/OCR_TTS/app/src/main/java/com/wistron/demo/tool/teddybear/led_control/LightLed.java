package com.wistron.demo.tool.teddybear.led_control;

import com.wistron.demo.tool.teddybear.ocr_tts.helper.WisShellCommandHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by aaron on 16-8-10.
 */
public class LightLed {

    private Command mCmd;
    private ExecutorService leftExec, rightExec;
    /**
     * Color array. The brightness for led. 0 is close.
     * Three led(Red led, Green led, Blue led) compose to one big led in Pando.
     */
    private final int Red[] = {0, 255, 0, 0, 255, 160, 255, 255};
    private final int Green[] = {0, 0, 255, 0, 255, 32, 192, 165};
    private final int Blue[] = {0, 0, 0, 255, 0, 240, 203, 0};
    /**
     * These flags used to control thread on/off
     */
    private static boolean isLightModeLeft = false;
    private static boolean isLightModeRight = false;
    private static boolean isLightDegreeModeLeft = false;
    private static boolean isLightDegreeModeRight = false;
    private static boolean isLightFlashModeLeft = false;
    private static boolean isLightFlashModeRight = false;
    private static boolean isLightRotateMode = false;
    /**
     * Used to control background led only turn once, if repeated calls light degree.
     */
    private static boolean isFirstLightDegreeLeft = true;
    private static boolean isFirstLightDegreeRight = true;
    private static int mCurrentLightPos = -1;
    private static int mLastLightPos = -1;
    private static int mRotateNumbers = 6;

    private WisShellCommandHelper wisShell;
    private static final String STOP_TURN_OFF = "stop_turn_off.sh";
    private static final String VOLUME = "volume.sh";
    public static String SHELL_PATH = "/data/data/com.wistron.demo.tool.teddybear/files";

    private boolean isClosingLed = false;

    public LightLed() {
        wisShell = new WisShellCommandHelper();
        mCmd = new Command();
        /**
         * Use SingleThreadExecutor to control led light mode change.
         */
        leftExec = Executors.newSingleThreadExecutor();
        rightExec = Executors.newSingleThreadExecutor();
    }

    //Use shell
    public void closeAll() {
        setFlagFalse();
        isClosingLed = true;
        leftExec.execute(new CloseAllThread());
    }

    //use shell
    public void lightShell() {
        setFlagFalse();
        leftExec.execute(new LightLedShell());
    }

    public void lightLed(int color) {
        setFlagFalse();
        setFirstDegreeFlag();
        leftExec.execute(new LeftLightThread(color));
        rightExec.execute(new RightLightThread(color));
    }

    public void lightDegreeLed(int color, int highColor, int degree) {
        setFlagFalse();
        //If degree = 360, array will out of length.
        if (degree > 359) {
            degree = 359;
        }
        leftExec.execute(new LeftLightDegreeLed(color, highColor, degree));
        rightExec.execute(new RightLightDegreeLed(color, highColor, degree));
    }

    public void lightFlashLed(int color, int flashColor) {
        setFlagFalse();
        setFirstDegreeFlag();
        leftExec.execute(new LeftLightFlashLed(color, flashColor));
        rightExec.execute(new RightLightFlashLed(color, flashColor));
    }

    public void lightRotateLed(int color) {
        setFlagFalse();
        setFirstDegreeFlag();
        leftExec.execute(new LeftLightRotateLed(color));
    }

    /**
     * Light led thread. Control left part led.
     */
    private class LeftLightThread implements Runnable {

        private int red = 255;
        private int green = 255;
        private int blue = 255;

        public LeftLightThread(int color) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isLightModeLeft = true;
            for (int i = 0; i < 6 && isLightModeLeft; i++) {
                lightingLed(red, green, blue, i);
            }
        }
    }

    /**
     * Light led thread. Control right part led.
     */
    private class RightLightThread implements Runnable {

        private int red = 255;
        private int green = 255;
        private int blue = 255;

        public RightLightThread(int color) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isLightModeRight = true;
            for (int i = 11; i > 5 && isLightModeRight; i--) {
                lightingLed(red, green, blue, i);
            }
        }
    }

    /**
     * The thread control left part led light in given degree.
     */
    private class LeftLightDegreeLed implements Runnable {

        private int red = 100;
        private int green = 100;
        private int blue = 100;
        private int redHigh = 255;
        private int greenHigh = 255;
        private int blueHigh = 255;
        private int mDegree = 0;

        public LeftLightDegreeLed(int color, int highColor, int degree) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
            redHigh = Red[highColor];
            greenHigh = Green[highColor];
            blueHigh = Blue[highColor];
            mDegree = degree;
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isLightDegreeModeLeft = true;
            if (isFirstLightDegreeLeft) {
                isFirstLightDegreeLeft = false;
                for (int i = 0; i < 6 && isLightDegreeModeLeft; i++) {
                    lightingLed(red, green, blue, i);
                }
            }
            if (isLightDegreeModeLeft) {
                if (mLastLightPos != mDegree / 30) {
                    if (mLastLightPos >= 0 && mLastLightPos <= 5) {
                        lightingDegreeLed(red, green, blue, mLastLightPos);
                    }
                    if (mDegree >= 0 && mDegree < 180) {
                        lightingDegreeHighLed(redHigh, greenHigh, blueHigh, mDegree);
                    }
                }
            }
        }
    }

    /**
     * The thread control right part led light in given degree.
     */
    private class RightLightDegreeLed implements Runnable {

        private int red = 100;
        private int green = 100;
        private int blue = 100;
        private int redHigh = 255;
        private int greenHigh = 255;
        private int blueHigh = 255;
        private int mDegree = 0;

        public RightLightDegreeLed(int color, int highColor, int degree) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
            redHigh = Red[highColor];
            greenHigh = Green[highColor];
            blueHigh = Blue[highColor];
            mDegree = degree;
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            isLightDegreeModeRight = true;
            if (isFirstLightDegreeRight) {
                isFirstLightDegreeRight = false;
                for (int i = 11; i > 5 && isLightDegreeModeRight; i--) {
                    lightingLed(red, green, blue, i);
                }
            }
            if (isLightDegreeModeRight) {
                if (mLastLightPos != mDegree / 30) {
                    if (mLastLightPos >= 6 && mLastLightPos <= 11) {
                        lightingDegreeLed(red, green, blue, mLastLightPos);
                    }
                    if (mDegree >= 180 && mDegree <= 360) {
                        lightingDegreeHighLed(redHigh, greenHigh, blueHigh, mDegree);
                    }
                }
            }
        }
    }

    /**
     * The thread control left part led flashing.
     */
    private class LeftLightFlashLed implements Runnable {

        private int red = 100;
        private int green = 100;
        private int blue = 100;
        private int redHigh = 255;
        private int greenHigh = 255;
        private int blueHigh = 255;

        public LeftLightFlashLed(int color, int highColor) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
            redHigh = Red[highColor];
            greenHigh = Green[highColor];
            blueHigh = Blue[highColor];
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isLightFlashModeLeft = true;
            while (isLightFlashModeLeft) {
                for (int i = 0; i < 6 && isLightFlashModeLeft; i++) {
                    mCmd.controlRedLED(red, Command.LED[i]);
                    mCmd.controlGreenLED(green, Command.LED[i]);
                    mCmd.controlBlueLED(blue, Command.LED[i]);
                }
                for (int i = 0; i < 6 && isLightFlashModeLeft; i++) {
                    mCmd.controlRedLED(redHigh, Command.LED[i]);
                    mCmd.controlGreenLED(greenHigh, Command.LED[i]);
                    mCmd.controlBlueLED(blueHigh, Command.LED[i]);
                }
            }
        }
    }

    /**
     * The thread control right part led flashing.
     */
    private class RightLightFlashLed implements Runnable {

        private int red = 100;
        private int green = 100;
        private int blue = 100;
        private int redHigh = 255;
        private int greenHigh = 255;
        private int blueHigh = 255;

        public RightLightFlashLed(int color, int highColor) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
            redHigh = Red[highColor];
            greenHigh = Green[highColor];
            blueHigh = Blue[highColor];
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isLightFlashModeRight = true;
            while (isLightFlashModeRight) {
                for (int i = 11; i > 5 && isLightFlashModeRight; i--) {
                    mCmd.controlRedLED(red, Command.LED[i]);
                    mCmd.controlGreenLED(green, Command.LED[i]);
                    mCmd.controlBlueLED(blue, Command.LED[i]);
                }
                for (int i = 11; i > 5 && isLightFlashModeRight; i--) {
                    mCmd.controlRedLED(redHigh, Command.LED[i]);
                    mCmd.controlGreenLED(greenHigh, Command.LED[i]);
                    mCmd.controlBlueLED(blueHigh, Command.LED[i]);
                }
            }
        }
    }

    /**
     * Use one thread to control led rotating.
     */
    private class LeftLightRotateLed implements Runnable {

        private int red = 100;
        private int green = 100;
        private int blue = 100;

        public LeftLightRotateLed(int color) {
            red = Red[color];
            green = Green[color];
            blue = Blue[color];
        }

        @Override
        public void run() {
            // wait last flag clear.
            while (isClosingLed) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isLightRotateMode = true;
            rotating(red, green, blue);
        }
    }

    /**
     * Use .sh file to close all led.
     */
    private class CloseAllThread implements Runnable {

        @Override
        public void run() {
            wisShell.exec("." + SHELL_PATH + "/" + STOP_TURN_OFF);
            isClosingLed = false;
        }
    }

    private class LightLedShell implements Runnable {

        @Override
        public void run() {
            wisShell.exec("." + SHELL_PATH + "/" + VOLUME);
        }
    }

    private void rotating(int red, int green, int blue) {
        for (int i = 0; i < mRotateNumbers && isLightRotateMode; i++) {
            mCmd.controlRedLED(red, Command.LED[i]);
            mCmd.controlGreenLED(green, Command.LED[i]);
            mCmd.controlBlueLED(blue, Command.LED[i]);
        }
        int j = mRotateNumbers;
        while (isLightRotateMode) {
            if (j > 11) {
                j = 0;
            }
            if (j - mRotateNumbers >= 0) {
                mCmd.controlRedLED(0, Command.LED[j - mRotateNumbers]);
                mCmd.controlGreenLED(0, Command.LED[j - mRotateNumbers]);
                mCmd.controlBlueLED(0, Command.LED[j - mRotateNumbers]);
            } else {
                mCmd.controlRedLED(0, Command.LED[j + (12 - mRotateNumbers)]);
                mCmd.controlGreenLED(0, Command.LED[j + (12 - mRotateNumbers)]);
                mCmd.controlBlueLED(0, Command.LED[j + (12 - mRotateNumbers)]);
            }
            mCmd.controlRedLED(red, Command.LED[j]);
            mCmd.controlGreenLED(green, Command.LED[j]);
            mCmd.controlBlueLED(blue, Command.LED[j]);
            j++;
        }
    }

    private void lightingDegreeHighLed(int redHigh, int greenHigh, int blueHigh, int degree) {
        mCurrentLightPos = degree / 30;
        mCmd.controlRedLED(redHigh, Command.LED[mCurrentLightPos]);
        mCmd.controlGreenLED(greenHigh, Command.LED[mCurrentLightPos]);
        mCmd.controlBlueLED(blueHigh, Command.LED[mCurrentLightPos]);
        mLastLightPos = mCurrentLightPos;
    }

    private void lightingDegreeLed(int red, int green, int blue, int lastPosition) {
        if (lastPosition != -1) {
            mCmd.controlRedLED(red, Command.LED[mLastLightPos]);
            mCmd.controlGreenLED(green, Command.LED[mLastLightPos]);
            mCmd.controlBlueLED(blue, Command.LED[mLastLightPos]);
        }
    }

    private void lightingLed(int red, int green, int blue, int position) {
        mCmd.controlRedLED(red, Command.LED[position]);
        mCmd.controlGreenLED(green, Command.LED[position]);
        mCmd.controlBlueLED(blue, Command.LED[position]);
    }

    private void setFlagFalse() {
        isLightModeLeft = false;
        isLightModeRight = false;
        isLightDegreeModeLeft = false;
        isLightDegreeModeRight = false;
        isLightFlashModeLeft = false;
        isLightFlashModeRight = false;
        isLightRotateMode = false;
    }

    private void setFirstDegreeFlag() {
        isFirstLightDegreeLeft = true;
        isFirstLightDegreeRight = true;
    }

    private int currentDegreeIndex = -1;
    private Object tempObject = new Object();

    public void turnOnHighlightLed(final int degreeIndex) {
        synchronized (tempObject) {
            if (currentDegreeIndex != degreeIndex) {
                turnOffHighLightLed();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mCmd.controlRedLED(255, Command.LED[degreeIndex]);
                        currentDegreeIndex = degreeIndex;
                    }
                }).start();
            }
        }
    }

    public void turnOffHighLightLed() {
        synchronized (tempObject) {
            if (currentDegreeIndex >= 0 && currentDegreeIndex <= 11) {
                mCmd.controlRedLED(0, Command.LED[currentDegreeIndex]);
                currentDegreeIndex = -1;
            }
        }
    }
}
