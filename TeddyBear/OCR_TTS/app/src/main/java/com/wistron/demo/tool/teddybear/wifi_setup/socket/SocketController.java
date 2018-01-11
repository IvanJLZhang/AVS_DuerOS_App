package com.wistron.demo.tool.teddybear.wifi_setup.socket;

import android.content.Context;
import android.util.Log;

import com.wistron.demo.tool.teddybear.wifi_setup.service.WifiDirectSetupService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by aaron on 16-10-19.
 */
public class SocketController {
    private static final String TAG = WifiDirectSetupService.TAG;
    private static final int PORT = 6006;
    private static final String END_WORD = "}:wistron";
    private ServerSocket mServerSocket = null;
    private Socket mClientSocket = null;
    private boolean acceptFlag = true;
    private boolean receiveFlag = true;
    private Context mContext;

    public SocketController(Context context) {
        mContext = context;
        new Thread(new AcceptRunnable()).start();
    }

    private class AcceptRunnable implements Runnable {

        public AcceptRunnable() {
            try {
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(PORT));
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "new ServerSocket exception " + e.getMessage());
            }
        }

        @Override
        public void run() {
            while (acceptFlag) {
                try {
                    Socket socket = mServerSocket.accept();
                    //just use one client, do not connect large than 1 client.
                    mClientSocket = socket;
                    Log.i(TAG, "accept client ");
                    new Thread(new ReceiveRunnable(socket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "accept exception " + e.getMessage());
                }
            }
        }
    }

    private class ReceiveRunnable implements Runnable {
        Socket socket = null;
        InputStream in = null;

        public ReceiveRunnable(Socket socket) {
            this.socket = socket;
            receiveFlag = true;
            try {
                in = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(TAG, "BufferedReader exception " + e.getMessage());
            }
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
                        data += new String(buffer, 0, bytes);
                    } while (!data.endsWith(END_WORD));
                    //notify receive data
                    Log.i(TAG, "receive data " + data);
                    ((WifiDirectSetupService) mContext).receiveData(data);
                    data = "";
                } catch (Exception e) {
                    receiveFlag = false;
                    e.printStackTrace();
                    Log.i(TAG, "receive Exception " + e.getMessage());
                }
            }
        }
    }

    private class SendRunnable implements Runnable {
        String data = "";
        OutputStream out = null;

        public SendRunnable(String data) {
            this.data = data;
        }

        @Override
        public void run() {
            if (mClientSocket != null && !mClientSocket.isClosed() && mClientSocket.isConnected()) {
                try {
                    out = mClientSocket.getOutputStream();
                    out.write((data + END_WORD).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "send data exception " + e.getMessage());
                }

            }
        }
    }

    public void release() {
        acceptFlag = false;
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (mClientSocket != null) {
            if (mClientSocket.isConnected()) {
                try {
                    mClientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mClientSocket = null;
        }
    }

    public void sendData(String data) {
        new Thread(new SendRunnable(data)).start();
    }

    public interface Notify {
        void receiveData(String data);
    }
}
