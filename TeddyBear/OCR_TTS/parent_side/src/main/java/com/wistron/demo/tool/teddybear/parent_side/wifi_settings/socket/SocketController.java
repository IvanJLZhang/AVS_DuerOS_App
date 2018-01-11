package com.wistron.demo.tool.teddybear.parent_side.wifi_settings.socket;

import android.content.Context;
import android.util.Log;

import com.wistron.demo.tool.teddybear.parent_side.wifi_settings.ui.SetupWifiActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by aaron on 16-10-20.
 */
public class SocketController {
    private static final String TAG = SetupWifiActivity.TAG;
    private static final int PORT = 6006;
    private static final String END_WORD = "}:wistron";
    private Socket mSocket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Boolean receiveFlag = true;
    private Context mContext;

    public SocketController(Context context) {
        mContext = context;
    }

    public void connect(String host) {
        new Thread(new ConnectRunnable(host)).start();
    }

    public void send(String data) {
        if (mSocket != null && mSocket.isConnected()) {
            try {
                out.write((data + END_WORD).getBytes());
            } catch (IOException e) {
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
            mSocket = new Socket();
            try {
                mSocket.bind(null);
                mSocket.connect(new InetSocketAddress(HOST, PORT), 5000);
                in = mSocket.getInputStream();
                out = mSocket.getOutputStream();
                new Thread(new ReceiveRunnable()).start();
                ((SetupWifiActivity) mContext).connectState(true);
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "connect Exception " + e.getMessage());
                ((SetupWifiActivity) mContext).socketException();
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
                    } while (!data.endsWith(END_WORD) || bytes == -1);
                    if (bytes > 0) {
                        //notify receive data
                        Log.i(TAG, "receive data " + data);
                        ((SetupWifiActivity) mContext).receiveData(data);
                        data = "";
                    } else {
                        receiveFlag = false;
                    }
                } catch (IOException e) {
                    receiveFlag = false;
                    e.printStackTrace();
                    Log.i(TAG, "receive Exception " + e.getMessage());
                    //((SetupWifiActivity) mContext).socketException();
                }
            }
        }
    }

    public interface Notify {
        void receiveData(String data);

        void socketException();

        void connectState(boolean state);
    }
}
