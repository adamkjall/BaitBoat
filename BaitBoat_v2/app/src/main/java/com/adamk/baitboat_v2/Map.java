package com.adamk.baitboat_v2;

import android.content.Context;

import android.location.LocationManager;
import android.util.AttributeSet;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Map.Entry;
import java.util.HashMap;
import java.util.List;


/**
 * Custom MapView class that uses osmdroid
 */
public class Map extends MapView implements MapEventsReceiver {
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private Context context;


    /**
     * The constructor that is called when the Map in activity_main.xml is created
     *
     * @param context, the application context
     * @param attrs,   the attribute set
     */
    public Map(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        setTileSource(TileSourceFactory.MAPNIK);
        mapController = getController();
        init();
        setBuiltInZoomControls(true);
        setMultiTouchControls(true);
    }

    /**
     * Initializes the map's starting position, draws a person on the map and follows that person
     * when the gps location is changed
     */
    private void init() {
        // configures map starting position
        //mapController.setZoom(15);
        GeoPoint startPoint = new GeoPoint(57.699775, 11.979478); // gbg coordinates
        mapController.setCenter(startPoint);

        // configures location overlay (the "person" icon that shows on the map)
        GpsMyLocationProvider gpsProvider = new GpsMyLocationProvider(context);
        gpsProvider.addLocationSource(LocationManager.GPS_PROVIDER);
        final MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(gpsProvider, this);
        locationOverlay.enableMyLocation(); // draw person on map

        locationOverlay.enableFollowLocation(); // move map when location changes
        this.getOverlayManager().add(locationOverlay);
        this.locationOverlay = locationOverlay;

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(context, this);
        this.getOverlays().add(0, mapEventsOverlay);
    }




    /**
     * @return The mapController
     */
    public IMapController getMapController() {
        return mapController;
    }

    /**
     * @return The location overlay
     */
    public MyLocationNewOverlay getLocationOverlay() {
        return locationOverlay;
    }


    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
}
