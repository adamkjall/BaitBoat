package com.adamk.baitboat_v2;

import android.content.Context;
import android.graphics.Point;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.Manifest;

import android.content.pm.PackageManager;
import android.os.AsyncTask;

import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.widget.EditText;

import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.github.rubensousa.floatingtoolbar.FloatingToolbarMenuBuilder;
import com.jackandphantom.joystickview.JoyStickView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;


import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class MainActivity extends AppCompatActivity {
    // Class variables used when connection to the raspberry pi
    private static String wifiModuleIp = "";
    private static String CMD = "0";
    private static int wifiModulePort = 0;

    // OpenStreetMap variables
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay = null;
    private CompassOverlay mCompassOverlay;
    private IMapController mapController;

    // The sliding menu
    private FloatingToolbar floatingToolbar;

    // Views
    private EditText tv_ipAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLocationPermissions();

        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        setContentView(R.layout.activity_main);

        tv_ipAddress = (EditText) findViewById(R.id.ipAddress);

        // Set up the mapView
        mapView = (MapView) findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Default mapView position
        mapController = mapView.getController();
        mapController.setZoom(19.0);
        GeoPoint startPoint = new GeoPoint(57.70639, 11.93827);
        mapController.setCenter(startPoint);

        // Set up the gps provider
        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(context);
        gpsMyLocationProvider.setLocationUpdateMinDistance(10);
        gpsMyLocationProvider.setLocationUpdateMinTime(1000);

        // My location overlay enables the map to follow your position
        myLocationOverlay = new MyLocationNewOverlay(gpsMyLocationProvider, mapView);
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(false);

        // Compass overlay
        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mapView);
        mCompassOverlay.enableCompass();

        // Find device width and height, and calculate it's height/width in pixel density
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;
        float dpHeight = outMetrics.heightPixels / density;
        float dpWidth = outMetrics.widthPixels / density;

        // Relocate the compass overlay to the bottom right corner
        mCompassOverlay.setCompassCenter(dpWidth * 0.94f, dpHeight * 0.83f);

        // Add overlays to the mapView
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(mCompassOverlay);

        // Toolbar menu with drawer feature
        floatingToolbar = findViewById(R.id.floatingToolbar);
        floatingToolbar.setMenu(new FloatingToolbarMenuBuilder(context)
                .addItem(R.id.action_unread, R.drawable.ic_markunread_black_24dp, "Mark unread")
                .addItem(R.id.action_copy, R.drawable.ic_content_copy_black_24dp, "Copy")
                .addItem(R.id.action_google, R.drawable.ic_google_plus_box, "Google+")
                .addItem(R.id.action_facebook, R.drawable.ic_facebook_box, "Facebook")
                .addItem(R.id.action_twitter, R.drawable.ic_twitter_box, "Twitter")
                .build());

        // FAB to show the floating toolbar
        FloatingActionButton fab = findViewById(R.id.fab);
        floatingToolbar.attachFab(fab);

        // Joystick
        JoyStickView joyStickView = findViewById(R.id.joy);
        joyStickView.setOnMoveListener(new JoyStickView.OnMoveListener() {
            @Override
            public void onMove(double angle, float strength) {

                boolean right = angle >= 0 && angle < 45 || angle <= 360 && angle > 315;
                boolean ahead = 45 <= angle && angle < 135;
                boolean left = angle >= 135 && angle < 225;
                boolean reverse = angle >= 225 && angle <= 315;

                if (right) {
                    startTask("Right");
                }
                if (ahead) {
                    startTask("Ahead");
                }
                if (left) {
                    startTask("Left");
                }
                if (reverse) {
                    startTask("Reverse");
                }
            }
        });

        showSettingsDialog();

    }

    private void startTask(String cmd) {
        getIPandPort();
        CMD = cmd;
        Socket_AsyncTask task = new Socket_AsyncTask();
        task.execute();
    }

    public void getIPandPort() {
        String iPandPort = tv_ipAddress.getText().toString();
        String temp[] = iPandPort.split(":");
        wifiModuleIp = temp[0];
        wifiModulePort = Integer.valueOf(temp[1]);
    }

    private void showSettingsDialog() {
        FragmentManager fm = getSupportFragmentManager();
        SettingsDialogFragment settingsDialog = SettingsDialogFragment.newInstance("Connect to Baitboat");
        settingsDialog.show(fm, "fragment_settings");
    }

    public class Socket_AsyncTask extends AsyncTask<Void, Void, Void> {
        Socket socket;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                InetAddress inetAddress = InetAddress.getByName(MainActivity.wifiModuleIp);
                socket = new java.net.Socket(inetAddress, MainActivity.wifiModulePort);
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeBytes(CMD);
                dataOutputStream.close();
                socket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Prompts the user for location permissions
     */
    private void checkLocationPermissions() {
        // check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 1);
        }
    }


    /**
     * Runs when app is resumed
     */
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        myLocationOverlay.enableMyLocation();
        mapView.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    /**
     * Runs when activity is paused
     */
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        myLocationOverlay.disableMyLocation();
        mapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
}
