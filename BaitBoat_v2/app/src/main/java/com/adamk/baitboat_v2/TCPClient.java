package com.adamk.baitboat_v2;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCPClient {
    private static final String TAG = "TCPClient";

    private SendRunnable sendRunnable;
    private Socket connectionSocket;
    private Thread sendThread;

    private String dataToSend;

    private String serverIP;
    private int serverPort;

    public TCPClient() {

    }

    public void connect(String ip, int port) {
        serverIP = ip;
        serverPort = port;
        new Thread(new ConnectRunnable()).start();
    }

    public void disconnect() {
        if (sendThread != null) {
            sendThread.interrupt();
        }

        try {
            connectionSocket.close();
            Log.d(TAG, "Disconnected");
        } catch (IOException e) {
        }
    }

    public boolean isConnected() {
        return connectionSocket != null && connectionSocket.isConnected()
                && !connectionSocket.isClosed();
    }

    private void startSending() {
        sendRunnable = new SendRunnable(connectionSocket);
        sendThread = new Thread(sendRunnable);
        sendThread.start();
    }

    public void writeCommand(String cmd) {
        if (isConnected()) {
            startSending();
            sendRunnable.sendCMD(cmd);
        }
    }


    class ConnectRunnable implements Runnable {

        @Override
        public void run() {
            try {
                Log.d(TAG, "Connecting...");
                InetAddress serverAddress = InetAddress.getByName(serverIP);

                connectionSocket = new Socket();
                // Start connecting to the server with 5000ms timeout
                // This will block thread until a connecting is established
                connectionSocket.connect(new InetSocketAddress(serverAddress, serverPort), 5000);

            } catch (IOException e) {
            }
            Log.d(TAG, "Connection thread stopped");
        }
    }

    class SendRunnable implements Runnable {

        private String data;
        private DataOutputStream out;
        private boolean hasMessage = false;

        public SendRunnable(Socket server) {
            try {
                this.out = new DataOutputStream(server.getOutputStream());
            } catch (IOException e) {
            }
        }

        public void sendCMD(String cmd) {
            this.data = cmd;
            this.hasMessage = true;
        }

        @Override
        public void run() {
            Log.d(TAG, "Sending started " + this.hasMessage);
            if (this.hasMessage) {
                try {
                    this.out.writeBytes(data + "|");
                    Log.d(TAG, "Send data: " + data);
                    this.out.flush();
                } catch (IOException e) {
                    this.hasMessage = false;
                    this.data = null;
                }
            }
            Log.d(TAG, "Sending stopped");
        }
    }
}
