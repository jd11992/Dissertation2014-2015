package com.example.jon.fitnessped;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public class Bluetooth {
    private static final String TAG = "bluetooth";
    private static final boolean D = true;

    private static final String NAME = "Arduino";

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private final BluetoothAdapter BAdapter;
    private final Handler handlers;
    private AcceptThread acceptThread;
    private ConnectThread CThread;
    private ConnectedThread connectedThread;
    private int State;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote device

    public Bluetooth(Context context, Handler handler) {
        BAdapter = BluetoothAdapter.getDefaultAdapter();
        if (D) for (BluetoothDevice bd : BAdapter.getBondedDevices())
            Log.d(TAG, "Bounded device " + bd);
        State = STATE_NONE;
        handlers = handler;
    }

    private synchronized void setState(int state) {
        if (D)
            Log.d(TAG, "setState() " + State + " -> " + state);
        State = state;

        // Give the new state to the Handler so the UI Activity can update
        handlers.obtainMessage(MESSAGE_STATE_CHANGE, state, -1)
                .sendToTarget();
    }

    public synchronized int getState() {
        return State;
    }

    public synchronized void start() {
        if (D)
            Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (CThread != null) {
            CThread.cancel();
            CThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    private synchronized void connect(BluetoothDevice device) {
        if (D)
            Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (State == STATE_CONNECTING) {
            if (CThread != null) {
                CThread.cancel();
                CThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        CThread = new ConnectThread(device);
        connectedThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device, final String socketType) {
        if (D)
            Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (CThread != null) {
            CThread.cancel();
            CThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket, socketType);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = handlers.obtainMessage(MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString("Connected", device.getName());
        msg.setData(bundle);
        handlers.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (D)
            Log.d(TAG, "stop");

        if (CThread != null) {
            CThread.cancel();
            CThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    private void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (State != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = handlers.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Unable to connect device");
        msg.setData(bundle);
        handlers.sendMessage(msg);

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = handlers.obtainMessage(MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString("Toast", "Device connection was lost");
        msg.setData(bundle);
        handlers.sendMessage(msg);

        // Start the service over to restart listening mode
        Bluetooth.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = BAdapter
                        .listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D)
                Log.d(TAG, "Socket Type: " + mSocketType
                        + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (State != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType
                            + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (Bluetooth.this) {
                        switch (State) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D)
                Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);

        }

        public void cancel() {
            if (D)
                Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType
                        + "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            BAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                Log.e(TAG,"Unable to connect socket ",e);
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType
                            + " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (Bluetooth.this) {
               CThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType
                        + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
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
                    Log.d(TAG, "message bytes " + bytes);
                    Log.d(TAG, "message string bytes " + String.valueOf(bytes));
                    Log.d(TAG, "message buffer " + new String(buffer));
                    // Send the obtained bytes to the UI Activity
                    handlers.obtainMessage(MESSAGE_READ, bytes,
                            -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    Bluetooth.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer
         *            The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                handlers.obtainMessage(MESSAGE_WRITE, -1, -1,
                        buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (this.getState() != Bluetooth.STATE_CONNECTED) {
            Log.w(TAG, "bluetooth is not connected");
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            char EOT = (char)3 ;
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = (message + EOT).getBytes();
            this.write(send);
        }
    }

    public void connectDevice(String deviceName) {
        // Get the device MAC address
        String address = null;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for(BluetoothDevice d: adapter.getBondedDevices()){
            if (d.getName().equals(deviceName)) address = d.getAddress();
        }

        try {
            BluetoothDevice device = adapter.getRemoteDevice(address); // Get the BluetoothDevice object
            this.connect(device); // Attempt to connect to the device
        } catch (Exception e){
            Log.e("Unable to connect to device address "+ address,e.getMessage());
        }
    }

}