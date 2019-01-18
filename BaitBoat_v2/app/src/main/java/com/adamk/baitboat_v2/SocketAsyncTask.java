package com.adamk.baitboat_v2;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

// Helper class that runs async background tasks, when executed it will send commands to a
// server program running on the network.
public class SocketAsyncTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = "SocketAsyncTask";

    /**
     * Overriden method from AsyncTask class. There the TCPClient object is created.
     *
     * @param params String input parameter that takes an ip address and port
     * @return
     */
    @Override
    protected Void doInBackground(String... params) {
        String serverIP = params[0];
        int serverPort = Integer.valueOf(params[1]);
        String command = params[2];

        try {
            Log.e(TAG, "Connecting...");

            InetAddress inetAddress = InetAddress.getByName(serverIP);

            // Create a socket to make the connection with the server
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(inetAddress, serverPort), 5000);

            // Send command via the output stream
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.writeBytes(command);
            dataOutputStream.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Connection failed");
        }
        return null;
    }

}
