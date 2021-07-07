package com.example.jon.fitnessped;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;


import java.util.Arrays;

public class map extends Fragment implements GoogleMap.OnMarkerDragListener {
    View rootview;
    // MapView mapview;
    private GoogleMap Gmaps;
    private TextView distancetext;
    private Marker markerA;
    private Marker markerB;
    private Polyline polyline;
    //private GPS gps;
DistanceCalculator calculator;
    Button gbutton;
    public static final String TAG = map.class.getSimpleName();
    Tracking tracking;
    private FragmentActivity myContext;

    @Override
    public void onAttach(Activity activity) {
        myContext = (FragmentActivity) activity;
        super.onAttach(activity);
    }

    protected int getLayoutId() {
        return R.layout.fragment_map;

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_map, container, false);
        // Gets the MapView from the XML layout and creates it

        //mapview = (MapView) rootview.findViewById(R.id.maps);
        //mapview.onCreate(savedInstanceState);

        // Gets to GoogleMap from the MapView and does initialization stuff
        // Gmaps = mapview.getMap();
        //    SupportMapFragment fragments = (SupportMapFragment) myContext.getSupportFragmentManager().findFragmentById(R.id.maps);
        // Gmaps = fragments.getMap();
        //Gmaps.getUiSettings().setMyLocationButtonEnabled(true);
        //Gmaps.setMyLocationEnabled(true);
        // if (Gmaps != null) {
        //   Gmaps.setMyLocationEnabled(true);
        // }
        // gbutton = (Button) rootview.findViewById(R.id.Gbutton);
        //gbutton.setOnClickListener(gStartListener);


        // Needs to call MapsInitializer before doing any CameraUpdateFactory calls
        // MapsInitializer.initialize(this.getActivity());
        //gps = new GPS(this, this);

        setUpMapIfNeeded();

        return rootview;
    }


    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        // mapview.onResume();
        // gps.connect();
    }

    private void setUpMapIfNeeded() {
        if (Gmaps != null) {
            return;
        }
        Gmaps = ((SupportMapFragment) myContext.getSupportFragmentManager().findFragmentById(R.id.maps)).getMap();
        if (Gmaps != null) {
            startMap();
        }
    }


    protected GoogleMap getMap() {
        setUpMapIfNeeded();
        return Gmaps;
    }

    protected void startMap() {
        distancetext = (TextView) rootview.findViewById(R.id.DistanceView);

        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(52.264354, -0.827526), 10));
        getMap().setOnMarkerDragListener(this);

        markerA = getMap().addMarker(new MarkerOptions().position(new LatLng(52.264354, -0.827526)).draggable(true));
        markerB = getMap().addMarker(new MarkerOptions().position(new LatLng(52.256475, -0.835278)).draggable(true));
        polyline = getMap().addPolyline(new PolylineOptions().geodesic(true));


        showDistance();
    }

    private void showDistance() {
        double distance = SphericalUtil.computeDistanceBetween(markerA.getPosition(), markerB.getPosition());
        distancetext.setText("The markers are " + formatNumber(distance) + " apart.");
    }

    private void updatePolyline() {
        polyline.setPoints(Arrays.asList(markerA.getPosition(), markerB.getPosition()));
    }

    private String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }

        return String.format("%4.3f%s", distance, unit);
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        showDistance();
        updatePolyline();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        showDistance();
        updatePolyline();
    }

    @Override
    public void onPause() {
        super.onPause();
        //mapview.onPause();
        //  gps.disconnect();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //  mapview.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        //  mapview.onLowMemory();
    }
/*
    private void getMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }
    public void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        //mMap.addMarker(new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current Location"));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title("I am here!");
        Gmaps.addMarker(options);
        Gmaps.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }
*/

}
