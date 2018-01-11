package com.wistron.demo.tool.teddybear.ocr_tts.helper;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * @author dragon
 *         The helper class of execute shell command
 */
public class WisShellCommandHelper {
    private Process execProcess;
    private onResultChangedListener mResultChangedListener;
    private String mShellPath;
    private boolean isExecuting;

    /**
     * The helper class of execute shell command
     */
    public WisShellCommandHelper() {
        super();
        // TODO Auto-generated constructor stub
        mShellPath = "Dalvik".equals(System.getProperty("java.vm.name")) ? "/system/bin/sh" : "/bin/sh";
    }

    /**
     * Execute shell command
     *
     * @param command shell command
     * @return Return the result of shell command.
     */
    public ArrayList<String> exec(String command) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            if (command.contains(">")) {
                execProcess = new ProcessBuilder(mShellPath, "-c", command).redirectErrorStream(true).start();
            } else {
                String[] params = command.trim().split(" ");
                execProcess = new ProcessBuilder(params).redirectErrorStream(true).start();
            }
            InputStream in = execProcess.getInputStream();
            BufferedReader mReader = new BufferedReader(new InputStreamReader(in));
            String mTemp;
            isExecuting = true;
            while (isExecuting && (mTemp = mReader.readLine()) != null) {
                //result.add(mTemp);
                if (mResultChangedListener != null) {
                    mResultChangedListener.onResultChanged(mTemp);
                }
                Log.i(getClass().getSimpleName(), mTemp);
            }
            mReader.close();
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Execute shell command which need root permission, may be it's valid only for ENG build image.
     * before you  use this method, please be sure that the file of /system/bin/su and /system/app/Superuser.apk exist.
     *
     * @param command The shell commmand.
     * @return Return the result of shell command.
     */
    public ArrayList<String> execSu(String command) {
        ArrayList<String> result = new ArrayList<String>();
        try {
            execProcess = new ProcessBuilder("su").redirectErrorStream(true).start();
            OutputStream out = execProcess.getOutputStream();
            DataOutputStream localDataOutputStream = new DataOutputStream(out);
            localDataOutputStream.writeBytes(command + "\n");
            localDataOutputStream.flush();
            localDataOutputStream.writeBytes("exit\n");
            localDataOutputStream.flush();
            execProcess.waitFor();
            InputStream in = execProcess.getInputStream();
            BufferedReader mReader = new BufferedReader(new InputStreamReader(in));
            String mTemp;
            while ((mTemp = mReader.readLine()) != null) {
                result.add(mTemp);
                if (mResultChangedListener != null) {
                    mResultChangedListener.onResultChanged(mTemp);
                }
                Log.i(getClass().getSimpleName(), mTemp);
            }
            mReader.close();
            in.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Terminates this shell process and closes any associated streams.
     */
    public void destroy() {
        if (execProcess != null) {
            execProcess.destroy();
            execProcess = null;
            isExecuting = false;
        }
    }

    /**
     * To listen the execute progress of shell command
     *
     * @param listener
     */
    public void setOnResultChangedListener(onResultChangedListener listener) {
        this.mResultChangedListener = listener;
    }

    /**
     * @author dragon
     *         To listen the shell command execute, get the current result immediately
     */
    public abstract interface onResultChangedListener {
        /**
         * This method will be invoked when shell command output line
         *
         * @param result the current line of result
         */
        public abstract void onResultChanged(String result);
    }
}
