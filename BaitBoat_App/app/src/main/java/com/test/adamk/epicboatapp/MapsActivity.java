package com.test.adamk.epicboatapp;

import android.app.AlertDialog;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jackandphantom.joystickview.JoyStickView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    // IP address to the Raspberry PI
    public static String ipAddress;
    public static int port;

    private GoogleMap mMap;

    private EditText et_ipAddress;
    private EditText et_port;

    private Button connect;

    private ToggleButton button1;
    private ToggleButton button2;
    private ToggleButton button3;

    private Pattern pattern;
    private Handler handler;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Matcher matcher;
    private Socket socket;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pattern = Pattern.compile(IPADDRESS_PATTERN);
        handler = new Handler();
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Joystick
        JoyStickView joyStickView = findViewById(R.id.joy);
        joyStickView.setOnMoveListener(new JoyStickView.OnMoveListener() {
            @Override
            public void onMove(double angle, float strength) {

            }
        });

        // Define buttons
        connect = findViewById(R.id.connect);
        button1 = findViewById(R.id.button1);
        button2 = findViewById(R.id.button2);
        button3 = findViewById(R.id.button3);

        // Define editTexts
        et_ipAddress = findViewById(R.id.et_ipAddress);
        et_port = findViewById(R.id.et_port);

        initButtonClickListeners();
    }

    private void initButtonClickListeners() {
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connect.getText().toString().equalsIgnoreCase("Connect")) {
                    try {
                        ipAddress = et_ipAddress.getText().toString();
                        if (!checkIP(ipAddress))
                            throw new UnknownHostException(port + "is not a valid IP address");
                        port = Integer.parseInt(et_port.getText().toString());
                        if (port > 65535 && port < 0)
                            throw new UnknownHostException(port + "is not a valid port number ");
                        Client client = new Client(ipAddress, port);
                        client.start();


                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {
                    connect.setText("Connect");
                    toggleButtonsState(false);
                    closeConnection();
                }
            }
        });


        button1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    lightOn(1);
                } else {
                    lightOff(1);
                }
            }
        });
        button2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    lightOn(2);
                } else {
                    lightOff(2);
                }
            }
        });
        button3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    lightOn(3);
                } else {
                    lightOff(3);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeConnection();
    }

    ////////////////////// light related methods /////////////
    void lightOn(int lednum) {
        try {
            out.writeObject(lednum + "1");
            out.flush();
            out.writeObject("end");
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    void lightOff(int lednum) {
        try {
            out.writeObject(lednum + "0");
            out.flush();
            out.writeObject("end");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean checkIP(final String ip) {
        matcher = pattern.matcher(ip);
        return matcher.matches();
    }


    private void closeConnection() {
        try {
            out.writeObject("close");
            out.close();
            in.close();
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }//end of closeConnection

    void toggleButtonsState(boolean state) {
        button1.setChecked(state);
        button2.setChecked(state);
        button3.setChecked(state);
    }

    //////////////switches related methods ///////////////////
    void checkSwitchStatus() {
        if (button1.isChecked()) {
            lightOn(1);
        } else {
            lightOff(1);
        }
        if (button2.isChecked()) {
            lightOn(2);
        } else {
            lightOff(2);
        }
        if (button3.isChecked()) {
            lightOn(3);
        } else {
            lightOff(3);
        }
    }




    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng pos = new LatLng(58, 11.5);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));


    }

    /////////////// client thread ////////////////////////////
    private class Client extends Thread {
        private String ipaddress;
        private int portnum;

        public Client(String ipaddress, int portnum) {
            this.ipaddress = ipaddress;
            this.portnum = portnum;
        }

        @Override
        public void run() {
            super.run();
            connectToServer(ipaddress, portnum);

        }


        public void connectToServer(String ip, int port) {

            try {
                socket = new Socket(InetAddress.getByName(ip), port);
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                for (int i = 0; i < 1; i++) {
                    System.out.println((String) in.readObject() + "\n");
                }
                checkSwitchStatus();
                handler.post(new Runnable() {
                    public void run() {
                        connect.setText("Close");
                        toggleButtonsState(true);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(new Runnable() {
                    public void run() {

                    }
                });
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

        }

    }//end of client class
}
