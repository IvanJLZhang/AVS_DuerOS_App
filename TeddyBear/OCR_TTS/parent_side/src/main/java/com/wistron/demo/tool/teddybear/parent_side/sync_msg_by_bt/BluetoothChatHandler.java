/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wistron.demo.tool.teddybear.parent_side.sync_msg_by_bt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatHandler {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-13ae-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-1010-ac64-0800200c9a66");

    public static final int MODE_SERVER = 0;
    public static final int MODE_CLIENT = 1;

    private int mCurrentMode;

    // Member fields
    // Server: acceptThread->ConnectedThread     Client: ConnectThread -> ConnectedThread
    private final BluetoothAdapter mAdapter;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private static final int MSG_STATE_CONNECTED = 0;
    private static final int MSG_STATE_CONNECT_FAIL = 1;
    private static final int MSG_STATE_DISCONNECT = 2;
    private static final int MSG_READ_DATA = 3;

    private ArrayList<ConnectedThread> mBTClientList;

    private OnWisBluetoothChatStateChangedListener mBTStateChangedListener;
    private OnWisBluetoothChatDataChangedListener mBTDataChangedListener;

    private boolean isStop = false;

    private boolean isConnected = false;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param mode    test mode: client or server
     */
    public BluetoothChatHandler(Context context, int mode) {
        this(context, BluetoothAdapter.getDefaultAdapter(), mode);
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param adapter The Bluetooth adapter
     * @param mode    test mode. client or server
     */
    public BluetoothChatHandler(Context context, BluetoothAdapter adapter, int mode) {
        mAdapter = adapter;
        if (mAdapter != null && !mAdapter.isEnabled()) {
            mAdapter.enable();
        }
        mCurrentMode = mode;
        if (mCurrentMode == MODE_SERVER) {
            mBTClientList = new ArrayList<>();
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mAdapter;
    }

    // just for Client call
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     **/
    public synchronized void startServer() {
        if (D) Log.d(TAG, "start");

        if (mBTClientList != null) {
            mBTClientList.clear();
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        if (mBTStateChangedListener != null) {
            mBTStateChangedListener.onStateIsListening();
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
        if (mInsecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(false);
            mSecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device with insecure connection
     *
     * @param device device  The BluetoothDevice to connectisse
     */
    public synchronized void connect(BluetoothDevice device) {
        connect(device, false);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mBTStateChangedListener != null) {
            mBTStateChangedListener.onStateIsConnecting();
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        if (D) Log.d(TAG, "connected, Socket Type:" + socketType);
        Log.i(TAG, "RemoteDeviceAddress: " + device.getAddress() + " , Name: " + device.getName());

        if (mCurrentMode == MODE_SERVER) {
            ConnectedThread clientThread = new ConnectedThread(socket, socketType);
            mBTClientList.add(clientThread);

            Message msg = mHandler.obtainMessage();
            msg.what = MSG_STATE_CONNECTED;
            msg.obj = device.getName();
            msg.sendToTarget();

            clientThread.start();
        } else {
            // Cancel the thread that completed the connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            // Cancel any thread currently running a connection
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(socket, socketType);

            Message msg = mHandler.obtainMessage();
            msg.what = MSG_STATE_CONNECTED;
            msg.obj = device.getName();
            msg.sendToTarget();

            mConnectedThread.start();
        }

    }

    /**
     * For Server use, to get the client count that connected to server
     *
     * @return Return the count of client which connected to server
     */
    public int getClientCount() {
        if (mBTClientList != null) {
            return mBTClientList.size();
        } else {
            return 0;
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        isStop = true;
        isConnected = false;

        if (mCurrentMode == MODE_SERVER) {
            if (mSecureAcceptThread != null) {
                mSecureAcceptThread.cancel();
                mSecureAcceptThread = null;
            }
            if (mInsecureAcceptThread != null) {
                mInsecureAcceptThread.cancel();
                mInsecureAcceptThread = null;
            }
            for (ConnectedThread client : mBTClientList) {
                client.cancel();
                client = null;
            }
        } else {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
        }

        if (mBTStateChangedListener != null) {
            mBTStateChangedListener.onStateIsIdle();
        }
    }

    /**
     * send content to server from client
     *
     * @param content send content
     */
    public synchronized void write(String content) {
        write("", content);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param macAddress destination MAC address
     * @param content    send content
     */
    public synchronized void write(String macAddress, String content) {
        // Create temporary object
        ConnectedThread r = null;
        if (mCurrentMode == MODE_SERVER) {
            for (ConnectedThread client : mBTClientList) {
                if (client.isMatchMacAddress(macAddress)) {
                    r = client;
                    break;
                }
            }
        } else {
            // Synchronize a copy of the ConnectedThread
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        if (r != null && content != null) {
            String tempContent = "M" + content.getBytes().length + ":" + content;
            r.write(tempContent.getBytes());
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            // Listen to the server socket if we're not connected
            BluetoothSocket socket = null;
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + " accept() failed", e);
                    if (!isStop) {
                        BluetoothChatHandler.this.startServer();
                    }
                    break;
                }
                connected(socket, socket.getRemoteDevice(), mSocketType);
                Log.i(TAG, "wait other client connect!");
            }
            if (D) Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D) Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG, "connect error: " + e.getMessage());
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                mHandler.sendEmptyMessage(MSG_STATE_CONNECT_FAIL);
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatHandler.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private String mRemoteDeviceAddress, mRemoteDeviceName;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();

                mRemoteDeviceAddress = mmSocket.getRemoteDevice().getAddress();
                mRemoteDeviceName = mmSocket.getRemoteDevice().getName();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_READ_DATA;
                    msg.obj = mRemoteDeviceAddress + "" + new String(buffer, 0, bytes);
                    msg.sendToTarget();
                } catch (IOException e) {
                    Log.i(TAG, "disconnected!");
                    e.printStackTrace();

                    Message msg = mHandler.obtainMessage();
                    msg.what = MSG_STATE_DISCONNECT;
                    msg.obj = mRemoteDeviceAddress + mRemoteDeviceName;
                    msg.sendToTarget();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public boolean isMatchMacAddress(String address) {
            return address.equalsIgnoreCase(getMacAddress());
        }

        public String getMacAddress() {
            return mmSocket.getRemoteDevice().getAddress();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            String value = "";
            switch (msg.what) {
                case MSG_STATE_CONNECTED:
                    value = "";
                    if (msg.obj != null) {
                        value = msg.obj.toString();
                    }
                    isConnected = true;
                    mBTStateChangedListener.onStateIsConnected(value);
                    break;
                case MSG_STATE_CONNECT_FAIL:
                    isConnected = false;
                    mBTStateChangedListener.onStateIsConnectFail();
                    break;
                case MSG_STATE_DISCONNECT:
                    String remoteDeviceAddress = "", remoteDeviceName = "";
                    if (msg.obj != null) {
                        String remoteDevice = msg.obj.toString();
                        remoteDeviceAddress = remoteDevice.substring(0, 17);
                        remoteDeviceName = remoteDevice.substring(17);
                    }
                    if (mCurrentMode == MODE_SERVER) {
                        for (ConnectedThread client : mBTClientList) {
                            if (client.getMacAddress().equalsIgnoreCase(remoteDeviceAddress)) {
                                mBTClientList.remove(client);
                                client.cancel();
                                client = null;

                                System.gc();
                                Log.i(TAG, "client remove!");
                                break;
                            }
                        }
                    }

                    isConnected = false;
                    mBTStateChangedListener.onStateIsDisconnect(remoteDeviceName);
                    break;
                case MSG_READ_DATA:
                    String content = (String) msg.obj;
                    String fromAddress = content.substring(0, 17);
                    String info = content.substring(17);
                    mBTDataChangedListener.onReadMessage(fromAddress, info);
                    break;
                default:
                    break;
            }
        }

    };

    /**
     * Register a callback to be invoked when connect status changed
     *
     * @param listener The callback that will run
     */
    public void setOnWisBluetoothChatStateChangedListener(OnWisBluetoothChatStateChangedListener listener) {
        mBTStateChangedListener = listener;
    }

    /**
     * @author dragon
     *         Interface definition for a callback to be invoked when connect status changed
     */
    public abstract interface OnWisBluetoothChatStateChangedListener {
        /**
         * Idle status
         */
        public abstract void onStateIsIdle();

        /**
         * connect to Server (only valid on Client side)
         */
        public abstract void onStateIsConnecting();

        /**
         * connect another device
         *
         * @param remoteName the Bluetooth name
         */
        public abstract void onStateIsConnected(String remoteName);

        /**
         * fail to connect the Server(only valid on Client side)
         */
        public abstract void onStateIsConnectFail();

        /**
         * disconnect from another device
         *
         * @param remoteName the Bluetooth name
         */
        public abstract void onStateIsDisconnect(String remoteName);

        /**
         * Listening the Client connection (only valid on Server side)
         */
        public void onStateIsListening();
    }

    /**
     * Register a callback to be invoked when data changed(read message from another device)
     *
     * @param listener The callback that will run
     */
    public void setOnWisBluetoothChatDataChangedListener(OnWisBluetoothChatDataChangedListener listener) {
        mBTDataChangedListener = listener;
    }

    /**
     * @author dragon
     *         Interface definition for a callback to be invoked when data changed
     */
    public abstract interface OnWisBluetoothChatDataChangedListener {
        /**
         * have read a message from other device
         *
         * @param from the source MAC address which send the message
         * @param msg  the message content
         */
        public abstract void onReadMessage(String from, String msg);
    }
}
