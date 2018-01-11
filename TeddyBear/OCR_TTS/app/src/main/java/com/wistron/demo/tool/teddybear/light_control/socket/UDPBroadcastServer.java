package com.wistron.demo.tool.teddybear.light_control.socket;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.wistron.demo.tool.teddybear.light_control.LightControllerService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by aaron on 17-2-10.
 */

public class UDPBroadcastServer {
    public static final String TAG = LightControllerServer.TAG;
    private static final int PORT = 8111;
    private static final String multicastHost = "224.0.0.1";
    private static final String GET_IP_REQUEST = "GET_IP_REQUEST";
    private MulticastSocket mSocket;
    private InetAddress receiveAddress;

    public UDPBroadcastServer() {
        try {
            mSocket = new MulticastSocket(PORT);
            receiveAddress = InetAddress.getByName(multicastHost);
            mSocket.joinGroup(receiveAddress);
            new Thread(new ReceiveUDPThread()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReceiveUDPThread implements Runnable {

        @Override
        public void run() {
            byte buf[] = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, 1024);
            while (true) {
                try {
                    mSocket.receive(dp);
                    buf = getLocalIpAddress().getBytes();
                    dp.setData(buf);
                    mSocket.send(dp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getLocalIpAddress() {
        WifiManager wifiManager = (WifiManager) LightControllerService.context.getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();

        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
}
