package com.example.jon.fitnessped;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class statspage extends Fragment {

    View rootview;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        rootview = inflater.inflate(R.layout.fragment_statspage, container, false);
        GraphView graph = (GraphView) rootview.findViewById(R.id.graph);
        graph.addSeries(series);
        return rootview;

    }

    LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
            new DataPoint(0, 1),
            new DataPoint(1, 5),
            new DataPoint(2, 3),
            new DataPoint(3, 2),
            new DataPoint(4, 6)
    });

}
