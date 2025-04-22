package com.example.proyecto1;

import android.content.Context;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MyMapEventsReceiver implements org.osmdroid.events.MapEventsReceiver {

    private final Context context;
    private final MapView map;
    private final MarkerManager markerManager; // interfaz personalizada que paso desde MapActivity
    private Marker pressedPointMarker = null;

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

        // limpiar marcadores de lugares anteriores
        markerManager.clearAllPlaceMarkers();

        // eliminar el marcador anterior del punto pulsado
        if (pressedPointMarker != null) {
            map.getOverlays().remove(pressedPointMarker);
            pressedPointMarker = null;
        }

        // cargar lugares desde ese punto
        markerManager.loadPlacesFromPoint(p);

        // crear nuevo marcador en el punto pulsado
        pressedPointMarker = new Marker(map);
        pressedPointMarker.setPosition(p);
        pressedPointMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        pressedPointMarker.setTitle(context.getString(R.string.selected_point));
        pressedPointMarker.setIcon(AppUtils.getMarkerColor(context, ""));
        map.getOverlays().add(pressedPointMarker);
        return true;
    }
}
