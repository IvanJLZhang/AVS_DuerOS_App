package com.wistron.demo.tool.teddybear.parent_side.light_bulb_control;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by aaron on 17-2-13.
 */

public class SocketClient {

    private static final String TAG = LightControllerClient.TAG;
    private static final int PORT = 8222;
    private static final String END_WORD = "}:wistron";
    private Socket mSocket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Boolean receiveFlag = true;
    private onConnectListener connectListener;

    public SocketClient() {
        mSocket = new Socket();
    }

    public void connect(String host) {
        if (!mSocket.isConnected()) {
            new Thread(new ConnectRunnable(host)).start();
        }
    }

    public void send(String data) {
        if (mSocket != null && mSocket.isConnected()) {
            try {
                out.write((data + END_WORD).getBytes());
            } catch (IOException e) {
                Log.i(TAG, "send exception " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void release() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mSocket != null) {
            if (mSocket.isConnected()) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mSocket = null;
        }
    }

    private class ConnectRunnable implements Runnable {
        String HOST;

        public ConnectRunnable(String host) {
            HOST = host;
        }

        @Override
        public void run() {
            try {
                mSocket.bind(null);
                mSocket.connect(new InetSocketAddress(HOST, PORT), 5000);
                in = mSocket.getInputStream();
                out = mSocket.getOutputStream();
                new Thread(new ReceiveRunnable()).start();
                if (connectListener != null && mSocket.isConnected()) {
                    connectListener.onConnectSuccess();
                }
            } catch (IOException e) {
                if (connectListener != null) {
                    connectListener.onConnectFail();
                }
                e.printStackTrace();
            }
        }
    }

    private class ReceiveRunnable implements Runnable {

        public ReceiveRunnable() {
            receiveFlag = true;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            String data = "";
            while (receiveFlag) {
                try {
                    do {
                        bytes = in.read(buffer, 0, buffer.length);
                        if (bytes > 0) {
                            data += new String(buffer, 0, bytes);
                        }
                    } while (!data.endsWith(END_WORD));
                    if (bytes > 0) {
                        //notify receive data
                        Log.i(TAG, "receive data " + data);
                        if (connectListener != null) {
                            connectListener.onDataReceived(data);
                        }
                        data = "";
                    } else {
                        receiveFlag = false;
                    }
                } catch (IOException e) {
                    receiveFlag = false;
                    e.printStackTrace();
                    Log.i(TAG, "receive Exception " + e.getMessage());
                }
            }
        }
    }

    public void setOnConnectListener(onConnectListener listener) {
        connectListener = listener;
    }

    public interface onConnectListener {
        void onConnectSuccess();

        void onConnectFail();

        void onDataReceived(String data);
    }
}
