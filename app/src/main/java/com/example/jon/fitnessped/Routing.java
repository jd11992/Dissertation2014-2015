package com.example.jon.fitnessped;

import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Arrays;

//import com.google.maps.android.SphericalUtil;

/**
 * Created by Jon on 25/04/2015.
 */
public class Routing extends map implements GoogleMap.OnMarkerDragListener {
    private TextView distancetext;
    private Marker markerA;
    private Marker markerB;
    private Polyline polyline;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_map;
    }

    @Override
    protected void startMap() {
        distancetext = (TextView) rootview.findViewById(R.id.DistanceView);

        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-33.8256, 151.2395), 10));
        getMap().setOnMarkerDragListener(this);

        markerA = getMap().addMarker(new MarkerOptions().position(new LatLng(-33.9046, 151.155)).draggable(true));
        markerB = getMap().addMarker(new MarkerOptions().position(new LatLng(-33.8291, 151.248)).draggable(true));
        polyline = getMap().addPolyline(new PolylineOptions().geodesic(true));


        showDistance();
    }

    private void showDistance() {
        // double distance = getActivity().SphericalUtil.computeDistanceBetween(markerA.getPosition(), markerB.getPosition());
        // distancetext.setText("The markers are " + formatNumber(distance) + " apart.");
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
}
