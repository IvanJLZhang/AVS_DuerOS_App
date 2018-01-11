package com.wistron.demo.tool.teddybear.scene.lamp_bulb;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.R;
import com.wistron.demo.tool.teddybear.scene.helper.SceneCommonHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by king on 17-2-28.
 */

public class YeeLightLampBulb extends LampBulbBase {
    private static final String TAG = "YeeLight";

    public static final String KEY_LOCATION = "Location";
    public static final String KEY_ID = "id";
    public static final String KEY_MODEL = "model";
    public static final String KEY_FW_VERSION = "fw_ver";
    public static final String KEY_POWER = "power";
    public static final String KEY_BRIGHT = "bright";
    public static final String KEY_COLOR_MODE = "color_mode";
    public static final String KEY_CT = "ct";
    public static final String KEY_RGB = "rgb";
    public static final String KEY_HUE = "hue";
    public static final String KEY_SATURATION = "sat";
    public static final String KEY_NAME = "name";

    private static final int MSG_SHOWLOG = 0;
    private static final int MSG_FOUND_DEVICE = 1;
    private static final int MSG_DISCOVER_FINISH = 2;
    private static final int MSG_STOP_SEARCH = 3;
    private static final int MSG_START_SEARCH = 4;
    private static final int MSG_CONNECT_SUCCESS = 10;
    private static final int MSG_CONNECT_FAILURE = 11;

    private static final String UDP_HOST = "239.255.255.250";
    private static final int UDP_PORT = 1982;
    private static final String message = "M-SEARCH * HTTP/1.1\r\n" +
            "HOST:239.255.255.250:1982\r\n" +
            "MAN:\"ssdp:discover\"\r\n" +
            "ST:wifi_bulb\r\n";//用于发送的字符串
    private DatagramSocket mDSocket;
    private boolean mSearching = false;
    private WifiManager.MulticastLock multicastLock;
    private Thread mSearchThread = null;

    private List<HashMap<String, String>> mDeviceList = new ArrayList<HashMap<String, String>>();
    private int mYeeLightIndex = -1;

    public YeeLightLampBulb(final Context mContext) {
        super(mContext);
        mMainHandler = new Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "get a message: " + msg.what);
                switch (msg.what) {
                    case MSG_FOUND_DEVICE:

                        break;
                    case MSG_SHOWLOG:
                        Toast.makeText(mContext, "" + msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "startYeeLight:: MSG_SHOWLOG --> " + msg.obj.toString());
                        break;
                    case MSG_STOP_SEARCH:
                        mSearching = false;
                        if (mDSocket != null) {
                            mDSocket.close();
                            mDSocket = null;
                        }
                        mSearchThread.interrupt();
                        break;
                    case MSG_START_SEARCH:
                        if (null != mListener) {
                            mListener.searchStart();
                        }
                        break;
                    case MSG_DISCOVER_FINISH:
                        Log.i(TAG, "startYeeLight:: MSG_DISCOVER_FINISH size = " + mDeviceList.size());

                        if (null != mListener) {
                            mListener.searchEnd();
                        }
                        break;
                    case MSG_CONNECT_FAILURE:

                        break;
                    case MSG_CONNECT_SUCCESS:
                        if (mBlubsAction == BlubsAction.TURN_ON_LIGHT) { // Open light
                            write(parseSwitch(true));
                        } else if (mBlubsAction == BlubsAction.TURN_OFF_LIGHT) {  // Close light
                            write(parseSwitch(false));
                        }

                        // Debug
                        /*Log.i(TAG, "connect success: to change light "+isDebugValue);
                        write(parseSwitch(isDebugValue));
                        isDebugValue = !isDebugValue;*/
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public List<HashMap<String, String>> getYeelightList() {
        return mDeviceList;
    }

    @Override
    public void startSearch() {
        WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wm.createMulticastLock("test");
        multicastLock.acquire();

        mSearchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mDSocket = new DatagramSocket();
                    DatagramPacket dpSend = new DatagramPacket(message.getBytes(),
                            message.getBytes().length, InetAddress.getByName(UDP_HOST),
                            UDP_PORT);
                    mDSocket.send(dpSend);
                    mMainHandler.sendEmptyMessageDelayed(MSG_STOP_SEARCH, 2000);
                    while (mSearching) {
                        byte[] buf = new byte[1024];
                        DatagramPacket dpRecv = new DatagramPacket(buf, buf.length);
                        mDSocket.receive(dpRecv);
                        byte[] bytes = dpRecv.getData();
                        StringBuffer buffer = new StringBuffer();
                        for (int i = 0; i < dpRecv.getLength(); i++) {
                            // parse /r
                            if (bytes[i] == 13) {
                                continue;
                            }
                            buffer.append((char) bytes[i]);
                        }
                        Log.d("socket", "got message:" + buffer.toString());
                        if (!buffer.toString().contains("yeelight")) {
                            mMainHandler.obtainMessage(MSG_SHOWLOG, "收到一条消息,不是Yeelight灯泡").sendToTarget();
                            return;
                        }
                        String[] infos = buffer.toString().split("\n");
                        HashMap<String, String> bulbInfo = new HashMap<String, String>();
                        for (String str : infos) {
                            int index = str.indexOf(":");
                            if (index == -1) {
                                continue;
                            }
                            String title = str.substring(0, index);
                            String value = str.substring(index + 1);
                            bulbInfo.put(title, value);
                        }
                        if (!hasAdd(bulbInfo)) {
                            mDeviceList.add(bulbInfo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mMainHandler.sendEmptyMessage(MSG_DISCOVER_FINISH);
                }
            }
        });
        if (!mSearching) {
            mMainHandler.sendEmptyMessage(MSG_START_SEARCH);
            mSearching = true;
            mSearchThread.start();
        }
    }

    @Override
    public void turnLightState(boolean on) {
        if (mYeeLightIndex < 0) { // don't to control
            return;
        }

        mBlubsAction = on ? BlubsAction.TURN_ON_LIGHT : BlubsAction.TURN_OFF_LIGHT;
        Log.i(TAG, "turnLightState to " + on);
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
        }
        connectYeeLight();
    }

    @Override
    public void destroy() {
        cmd_run = false;
        if (mBos != null) {
            try {
                mBos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBos = null;
        }
        if (mSocket != null) {
            try {
                if (mSocket != null)
                    mSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mReader != null) {
            try {
                mReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mReader = null;
        }
    }

    private boolean hasAdd(HashMap<String, String> bulbinfo) {
        for (HashMap<String, String> info : mDeviceList) {
            Log.d(TAG, "location params = " + bulbinfo.get("Location"));
            if (info.get("Location").equals(bulbinfo.get("Location"))) {
                return true;
            }
        }
        return false;
    }

    public void setControlLightIndex(int index) {
        mYeeLightIndex = index;
    }

    // YeeLightControl
    private static final String CMD_TOGGLE = "{\"id\":%id,\"method\":\"toggle\",\"params\":[]}\r\n";
    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n";
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",500]}\r\n";
    private static final String CMD_CT = "{\"id\":%id,\"method\":\"set_ct_abx\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_HSV = "{\"id\":%id,\"method\":\"set_hsv\",\"params\":[%value, 100, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS_SCENE = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_COLOR_SCENE = "{\"id\":%id,\"method\":\"set_scene\",\"params\":[\"cf\",1,0,\"100,1,%color,1\"]}\r\n";
    private int mCmdId;
    private Socket mSocket;
    private String mBulbIP;
    private int mBulbPort;
    private BufferedOutputStream mBos;
    private BufferedReader mReader;
    private boolean cmd_run = true;

    // Debug
    private boolean isDebugValue = false;

    private void connectYeeLight() {
        HashMap<String, String> bulbInfo = mDeviceList.get(mYeeLightIndex);
        String ipinfo = bulbInfo.get(KEY_LOCATION).split("//")[1];
        mBulbIP = ipinfo.split(":")[0];
        mBulbPort = Integer.parseInt(ipinfo.split(":")[1]);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cmd_run = true;
                    mSocket = new Socket(mBulbIP, mBulbPort);
                    mSocket.setKeepAlive(true);
                    mBos = new BufferedOutputStream(mSocket.getOutputStream());
                    mMainHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS);
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (cmd_run) {
                        try {
                            String value = mReader.readLine();
                            Log.d(TAG, "value = " + value);
                            if (value != null) {
                                if (null != mListener) {
                                    mListener.updateBulbLog(value);
                                }
                                try {
                                    JSONObject toJason = new JSONObject(value);
                                    if (toJason.has("id")) {
                                        String resultContent = SceneCommonHelper.getString(mContext, R.string.luis_assistant_operate_success);
                                        if (toJason.has("error")) {
                                            JSONObject errorObject = toJason.getJSONObject("error"); // 1,3 success; 2 Fail
                                            String errorMsg = "";
                                            if (errorObject.has("message")) {
                                                errorMsg = errorObject.getString("message");
                                            }
                                            resultContent = SceneCommonHelper.getString(mContext, R.string.luis_assistant_operate_fail) + errorMsg;
                                        }
                                        if (null != mListener) {
                                            mListener.updateBulbLogAndSpeak(resultContent);
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    SceneCommonHelper.closeLED();
                                }
                            } else {
                                cmd_run = false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mMainHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                } finally {
                    if (mReader != null) {
                        try {
                            mReader.close();
                            mReader = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private String parseSwitch(boolean on) {
        String cmd;
        if (on) {
            cmd = CMD_ON.replace("%id", String.valueOf(++mCmdId));
        } else {
            cmd = CMD_OFF.replace("%id", String.valueOf(++mCmdId));
        }
        return cmd;
    }

    private String parseCTCmd(int ct) {
        return CMD_CT.replace("%id", String.valueOf(++mCmdId)).replace("%value", String.valueOf(ct + 1700));
    }

    private String parseColorCmd(int color) {
        return CMD_HSV.replace("%id", String.valueOf(++mCmdId)).replace("%value", String.valueOf(color));
    }

    private String parseBrightnessCmd(int brightness) {
        return CMD_BRIGHTNESS.replace("%id", String.valueOf(++mCmdId)).replace("%value", String.valueOf(brightness));
    }

    private void write(String cmd) {
        if (mBos != null && mSocket.isConnected()) {
            try {
                mBos.write(cmd.getBytes());
                mBos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "mBos = null or mSocket is closed");
        }
    }
}
