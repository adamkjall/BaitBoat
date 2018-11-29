package com.test.adamk.epicboatapp;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetworkService extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... strings) {
        try {
            // IP address to the Raspberry PI
            URL url = new URL("http://" + MapsActivity.ipAddress +"/?" + strings[0]);

            // Try to establish a connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            Log.d("NetworkService", "trying to connect to " + "http://" + MapsActivity.ipAddress +"/?" + strings[0]);
            // Then read the data
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                result.append(inputLine).append("\n");

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
