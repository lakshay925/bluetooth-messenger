package com.example.bluetoothchatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatUtils {
    private Context context;
    private final Handler handler;

    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");
    private final String APP_NAME = "BluetoothMessenger";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        this.state = STATE_NONE;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }



    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }

    public void write(byte[] buffer) {
        ConnectedThread temp_Thread;
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }

            temp_Thread = connectedThread;
        }

        temp_Thread.write(buffer);
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,APP_UUID);
            } catch (IOException e) {
                Log.e("Accept____Constructor", e.toString());
            }

            serverSocket = temp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept___Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept___Close", e.toString());
                }
            }

            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept___Close", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public  ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket temp = null;
            try {
                temp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("ConnectedThread", e.toString());
            }

            socket = temp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("Connected___Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connected___Close", e.toString());
                }
                connectionFailed();
                return;
            }

            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connected___cancel", e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = socket.getInputStream();
                tempOut = socket.getOutputStream();
            } catch (IOException e) {

            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while(true) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    connectionLost();
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                //outputStream.flush();//------*********************************---------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                //inputStream.close();//----------
                //outputStream.close();//---------
                socket.close();
            } catch (IOException e) {

            }
        }
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST_KEY, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST_KEY, "Cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME_KEY, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
