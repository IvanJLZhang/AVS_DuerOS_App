package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

import android.util.Log;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by aaron on 17-2-14.
 */

public class LightControllerClient implements UDPBroadcastClient.onDataReceiveListener, SocketClient.onConnectListener {

    public static final String TAG = "Light";
    private SocketClient mSocketClient;
    private UDPBroadcastClient mBroadcast;
    private onDataReceiveListener onDataReceiveListener;
    private onConnectListener onConnectListener;

    public LightControllerClient() {
        mSocketClient = new SocketClient();
        mSocketClient.setOnConnectListener(this);
        mBroadcast = new UDPBroadcastClient();
        mBroadcast.setOnDataReceiveListener(this);
    }

    public void getLightList() {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_GET_LIGHTS);
        mSocketClient.send(jsonObject.toString());
    }

    public void powerOn(Light light) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_LIGHT_ON);
        JSONUtil.setLight(jsonObject, light);
        mSocketClient.send(jsonObject.toString());
    }

    public void powerOn(List<Light> lights) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_LIGHTS_ON);
        JSONUtil.setLightList(jsonObject, lights);
        mSocketClient.send(jsonObject.toString());
    }

    public void powerOff(Light light) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_LIGHT_OFF);
        JSONUtil.setLight(jsonObject, light);
        mSocketClient.send(jsonObject.toString());
    }

    public void powerOff(List<Light> lights) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_LIGHTS_OFF);
        JSONUtil.setLightList(jsonObject, lights);
        mSocketClient.send(jsonObject.toString());
    }

    public void connServer() {
        mBroadcast.sendUDPBroadcast();
    }

    public void release() {
        mSocketClient.release();
        mBroadcast.release();
    }

    @Override
    public void onDataReceive(String data) {
        //connect to server
        mSocketClient.connect(data);
    }

    @Override
    public void onConnectSuccess() {
        onConnectListener.onConnectSuccess();
        Log.i(TAG, "onConnectSuccess");
    }

    @Override
    public void onConnectFail() {
        onConnectListener.onConnectFail();
        Log.i(TAG, "onConnectFail");
    }

    @Override
    public void onDataReceived(String data) {
        if (onDataReceiveListener != null) {
            onDataReceiveListener.onDataReceive(data);
        }
    }

    public void setOnDataReceiveListener(onDataReceiveListener listener) {
        onDataReceiveListener = listener;
    }

    public void setOnConnectListener(onConnectListener listener) {
        onConnectListener = listener;
    }

    public interface onConnectListener {
        void onConnectSuccess();

        void onConnectFail();
    }

    public interface onDataReceiveListener {
        //Receive server data, use socket
        void onDataReceive(String data);
    }
}
