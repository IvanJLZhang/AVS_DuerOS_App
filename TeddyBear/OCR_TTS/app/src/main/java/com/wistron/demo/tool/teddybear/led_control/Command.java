package com.wistron.demo.tool.teddybear.led_control;

/**
 * Created by aaron on 16-8-3.
 */
public class Command {

    private final String mShellPath = "Dalvik".equals(System.getProperty("java.vm.name")) ? "/system/bin/sh" : "/bin/sh";
    /**
     * The id of the led.
     */
    public static int LED[] = {7, 8, 9, 1, 2, 3, 4, 5, 6, 10, 11, 12};

    private void exeCmd(String cmd) {
        String cmds[] = {"/system/bin/sh", "-c", cmd};
        try {
            Process mRunCmd = new ProcessBuilder(mShellPath, "-c", cmd).start();
            mRunCmd.waitFor();
            mRunCmd.destroy();
            mRunCmd = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void controlRedLED(int value, int id) {
        String cmd;
        if (id < 10) {
            cmd = "echo " + value + " > /sys/class/leds/d60" + id + ":R/brightness";
        } else {
            cmd = "echo " + value + " > /sys/class/leds/d6" + id + ":R/brightness";
        }
        exeCmd(cmd);
    }

    public void controlGreenLED(int value, int id) {
        String cmd;
        if (id < 10) {
            cmd = "echo " + value + " > /sys/class/leds/d60" + id + ":G/brightness";
        } else {
            cmd = "echo " + value + " > /sys/class/leds/d6" + id + ":G/brightness";
        }
        exeCmd(cmd);
    }

    public void controlBlueLED(int value, int id) {

        String cmd;
        if (id < 10) {
            cmd = "echo " + value + " > /sys/class/leds/d60" + id + ":B/brightness";
        } else {
            cmd = "echo " + value + " > /sys/class/leds/d6" + id + ":B/brightness";
        }
        exeCmd(cmd);
    }
}
