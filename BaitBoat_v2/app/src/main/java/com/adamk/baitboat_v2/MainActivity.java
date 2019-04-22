package com.adamk.baitboat_v2;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.Manifest;

import android.content.pm.PackageManager;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.InputType;
import android.text.method.DigitsKeyListener;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity {
    // Class variables used when connection to the raspberry pi
    private static String serverIP = "";
    private static final String SERVER_PORT = "21570";

    // OpenStreetMap variables
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay = null;

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

        setupMap(context);

        // Sliding menu bar with a fab that toggles show/hide
        setupMenuBar();

        // Fab that'll center map around device gps location
        setupPositionFAB();

        setupJoyStick();

        // Dialog for ip-address input
        showConnectDialog();
    }

    /**
     * Creates a joystick view and then connect a onMove listener
     * that listens to the joystick's input and starts a async task
     * with the command as a parameter.
     */
    private void setupJoyStick() {
        JoyStickView joyStickView = findViewById(R.id.joy);
        joyStickView.setOnMoveListener(new JoyStickView.OnMoveListener() {
            @Override
            public void onMove(double angle, float strength) {
                boolean right = angle >= 0 && angle < 45 || angle <= 360 && angle > 315;
                boolean ahead = 45 <= angle && angle < 135;
                boolean left = angle >= 135 && angle < 225;
                boolean reverse = angle >= 225 && angle <= 315;

                if (right) startTask("Right");
                if (ahead) startTask("Ahead");
                if (left) startTask("Left");
                if (reverse) startTask("Reverse");
            }
        });
    }

    /**
     * Floating action button that centers the map
     * around the device gps position.
     */
    private void setupPositionFAB() {
        // FAB to center map around device position
        FloatingActionButton followButton = (FloatingActionButton) findViewById(R.id.fab_centerMap);
        followButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLocationOverlay.enableFollowLocation();
            }
        });
    }

    /**
     * Sliding menu bar that can be shown/hidden.
     */
    private void setupMenuBar() {
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
    }

    /**
     * Setup the map widget, using osmdroid.
     * https://github.com/osmdroid/osmdroid
     *
     * @param context Application context
     */
    private void setupMap(Context context) {
        // Set up the mapView
        mapView = (MapView) findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Default mapView position
        IMapController mapController = mapView.getController();
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
        CompassOverlay mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mapView);
        mCompassOverlay.enableCompass();

        // Add overlays to the mapView
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(mCompassOverlay);
    }

    /**
     * Start an async task that will send a command
     * using TCP Socket.
     *
     * @param cmd Command to send to the server.
     */
    private void startTask(String cmd) {
        // The task will handle the output stream
        SocketAsyncTask task = new SocketAsyncTask();
        task.execute(serverIP, SERVER_PORT, cmd);
    }

    /**
     * Prompt the user for an ip-address.
     */
    private void showConnectDialog() {
        final EditText et_ipAddress = new EditText(this);
        et_ipAddress.setText(serverIP);
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
                        serverIP = et_ipAddress.getText().toString();
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
