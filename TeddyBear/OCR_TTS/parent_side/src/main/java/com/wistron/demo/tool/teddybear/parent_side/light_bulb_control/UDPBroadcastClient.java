package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by aaron on 17-2-10.
 */

public class UDPBroadcastClient {
    private static final String TAG = LightControllerClient.TAG;
    private static final int PORT = 8111;
    private static final String GET_IP_REQUEST = "GET_IP_REQUEST";
    private MulticastSocket mSocket;
    private onDataReceiveListener mOnDataReceiveListener;
    private boolean receiveFlag = true;

    public UDPBroadcastClient() {
        try {
            mSocket = new MulticastSocket(PORT);
            new Thread(new ReceiveUDPThread()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendUDPBroadcast() {
        new Thread(new SendUDPThread()).start();
    }

    public void release() {
        receiveFlag = false;
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    private class SendUDPThread implements Runnable {

        @Override
        public void run() {
            DatagramPacket datagramPacket = null;
            byte data[] = GET_IP_REQUEST.getBytes();
            InetAddress address = null;
            try {
                mSocket.setTimeToLive(4);
                address = InetAddress.getByName("224.0.0.1");
                //这个地方可以输出判断该地址是不是广播类型的地址
                if (address.isMulticastAddress()) {
                    datagramPacket = new DatagramPacket(data, data.length, address,
                            PORT);
                    mSocket.send(datagramPacket);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveUDPThread implements Runnable {

        @Override
        public void run() {
            byte buf[] = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, 1024);
            while (receiveFlag) {
                try {
                    mSocket.receive(dp);
                    if (mOnDataReceiveListener != null) {
                        mOnDataReceiveListener.onDataReceive(new String(buf, 0, dp.getLength()));
                    }
                    Log.i(TAG, "receive data : " + new String(buf, 0, dp.getLength()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setOnDataReceiveListener(onDataReceiveListener listener) {
        mOnDataReceiveListener = listener;
    }

    public interface onDataReceiveListener {
        //Receive server IP
        void onDataReceive(String data);
    }
}
