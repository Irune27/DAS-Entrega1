package com.example.proyecto1;

import android.content.Context;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MyMapEventsReceiver implements org.osmdroid.events.MapEventsReceiver {

    private final Context context;
    private final MapView map;
    private final MarkerManager markerManager; // interfaz personalizada que paso desde MapActivity

    public interface MarkerManager {
        void clearAllPlaceMarkers();
        void loadPlacesFromPoint(GeoPoint point);
    }

    public MyMapEventsReceiver(Context context, MapView map, MarkerManager markerManager) {
        this.context = context;
        this.map = map;
        this.markerManager = markerManager;
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // no hacemos nada con un solo toque
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        Toast.makeText(context, context.getString(R.string.loading_from_point), Toast.LENGTH_SHORT).show();

        // limpiar marcadores anteriores y cargar nuevos desde el punto pulsado
        markerManager.clearAllPlaceMarkers();
        markerManager.loadPlacesFromPoint(p);
        return true;
    }
}
