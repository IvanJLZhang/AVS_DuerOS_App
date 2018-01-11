package com.wistron.demo.tool.teddybear.light_control.socket;

import com.wistron.demo.tool.teddybear.light_control.util.JSONUtil;
import com.wistron.demo.tool.teddybear.light_control.util.Light;

import org.json.JSONObject;

import java.util.List;

/**
 * Created by aaron on 17-2-16.
 */

public class LightControllerServer implements SocketServer.onDataReceiveListener {

    public static final String TAG = "Light";
    private SocketServer socketServer;
    private UDPBroadcastServer udpBroadcastServer;
    private onDataReceiveListener onDataReceiveListener;

    public LightControllerServer() {
        socketServer = new SocketServer();
        socketServer.setOnDataReceiveListener(this);
        udpBroadcastServer = new UDPBroadcastServer();
    }

    public void sendLightsList(List<Light> lights) {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.setCommand(jsonObject, Light.CMD_GET_LIGHTS);
        JSONUtil.setLightList(jsonObject, lights);
        socketServer.send(jsonObject.toString());
    }

    /**
     * send string to parent for AVS
     *
     * @param content
     */
    public void sendString(String content) {
        socketServer.send(content);
    }

    @Override
    public void onDataReceive(String data) {
        if (onDataReceiveListener != null) {
            onDataReceiveListener.onDataReceive(data);
        }
    }

    public void setOnDataReceiveListener(onDataReceiveListener listener) {
        onDataReceiveListener = listener;
    }

    public interface onDataReceiveListener {
        void onDataReceive(String data);
    }

}
