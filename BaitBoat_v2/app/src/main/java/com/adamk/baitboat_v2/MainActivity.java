package com.adamk.baitboat_v2;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.Manifest;

import android.content.pm.PackageManager;
import android.os.AsyncTask;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputType;
import android.text.method.DigitsKeyListener;

import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.jackandphantom.joystickview.JoyStickView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

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
    private static String serverIP = "";
    private static String CMD = "0";
    private static int serverPort = 21567;

    // OpenStreetMap variables
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay = null;
    private CompassOverlay mCompassOverlay;
    private IMapController mapController;

    // The sliding menu
    private FloatingToolbar floatingToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLocationPermissions();

        final Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        // Set up the mapView
        mapView = (MapView) findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Default mapView position
        mapController = mapView.getController();
        mapController.setZoom(19.0);

        // Instantiate the gps provider
        GpsMyLocationProvider gpsMyLocationProvider = new GpsMyLocationProvider(context);

        // My location overlay enables the map to follow your position
        myLocationOverlay = new MyLocationNewOverlay(gpsMyLocationProvider, mapView);
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(false);

        // Change standard position icon
        Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.marker_cluster);
        //myLocationOverlay.setPersonIcon(icon);
        myLocationOverlay.setDirectionArrow(icon, icon);

        // Compass overlay
        mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mapView);
        mCompassOverlay.enableCompass();

        // Find device width and height, and calculate it's height/width in pixel density
        //Display display = getWindowManager().getDefaultDisplay();
        //DisplayMetrics outMetrics = new DisplayMetrics();
        //display.getMetrics(outMetrics);
        //float density = getResources().getDisplayMetrics().density;
        //float dpHeight = outMetrics.heightPixels / density;
        //float dpWidth = outMetrics.widthPixels / density;

        // Relocate the compass overlay to the bottom right corner
        //mCompassOverlay.setCompassCenter(dpWidth * 0.94f, dpHeight * 0.83f);

        // Add overlays to the mapView
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(mCompassOverlay);

        // Toolbar menu with drawer feature
        floatingToolbar = findViewById(R.id.floatingToolbar);
        floatingToolbar.enableAutoHide(false);
        floatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {
                int itemID = item.getItemId();
                switch (itemID) {
                    case R.id.action_close_menu:
                        floatingToolbar.hide();
                        break;
                    case R.id.action_settings:
                        showConnectDialog();
                        break;
                    case R.id.action_google:
                        break;
                    case R.id.action_copy:
                        break;
                    case R.id.action_unread:
                        break;
                }
            }

            @Override
            public void onItemLongClick(MenuItem item) {
            }
        });

        // FAB to show the floating toolbar
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_menu);
        floatingToolbar.attachFab(fab);

        // FAB to center map around device position
        FloatingActionButton followButton = (FloatingActionButton) findViewById(R.id.fab_centerMap);
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLocationOverlay.enableFollowLocation();
                mapController.setZoom(19.0);
            }
        });

        // Joystick
        JoyStickView joyStickView = findViewById(R.id.joy);
        joyStickView.setOnMoveListener(new JoyStickView.OnMoveListener() {
            @Override
            public void onMove(double angle, float strength) {
                boolean right = angle >= 0 && angle < 45 || angle <= 360 && angle > 315;
                boolean ahead = 45 <= angle && angle < 135;
                boolean left = angle >= 135 && angle < 225;
                boolean reverse = angle >= 225 && angle <= 315;
                boolean stop = strength == 0;


                if (right) startTask("Right");
                if (ahead) startTask("Ahead");
                if (left) startTask("Left");
                if (reverse) startTask("Reverse");

            }
        });

        showConnectDialog();
    }

    private void startTask(String cmd) {
        CMD = cmd;
        Socket_AsyncTask task = new Socket_AsyncTask();
        task.execute();
    }

    private void showConnectDialog() {
        final EditText et_ipAddress = new EditText(this);
        et_ipAddress.setHint("Enter IP address here");
        et_ipAddress.setInputType(InputType.TYPE_CLASS_NUMBER);
        et_ipAddress.setKeyListener(DigitsKeyListener.getInstance(".0123456789"));
        et_ipAddress.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Connect to Raspberry Pi")
                .setView(et_ipAddress)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        serverIP = String.valueOf(et_ipAddress.getText());
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
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


    // Helper class that runs async background tasks, when executed it will send commands to a
    // server program running on the network.
    public class Socket_AsyncTask extends AsyncTask<Void, Void, Void> {
        Socket socket;

        @Override
        protected Void doInBackground(Void... params) {
            try {
                InetAddress inetAddress = InetAddress.getByName(MainActivity.serverIP);
                socket = new java.net.Socket(inetAddress, MainActivity.serverPort);
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
}
