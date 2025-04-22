package com.example.proyecto1;

import android.Manifest;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements MyMapEventsReceiver.MarkerManager {
    private MapView map;
    private MyLocationNewOverlay locationOverlay;
    private GeoPoint defaultPoint = new GeoPoint(43.2622721, -2.9483046);
    private final List<Marker> restaurantMarkers = new ArrayList<>();
    private final List<Marker> cafeMarkers = new ArrayList<>();
    private final List<Marker> marketMarkers = new ArrayList<>();
    private NetworkChangeReceiver networkChangeReceiver;
    private boolean hasAlreadyLoadedPlaces = false;
    private boolean showRestaurants = true;
    private boolean showCafes = true;
    private boolean showMarkets = true;
    private int locationAttempts = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setLocale(this);

        // cargar/inicializar la configuración osmdroid
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.map);
        // definir el estilo del mapa
        map.setTileSource(TileSourceFactory.MAPNIK);
        // al pulsar estos botones de zoom por defecto, se dispara el longPressHelper
        map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);

        // hacer zoom en el mapa hasta 18
        map.getController().setZoom(18.0);
        map.getController().setCenter(defaultPoint);
        // limitar el zoom que puede hacer el usuario
        map.setMinZoomLevel(15.0);
        map.setMaxZoomLevel(21.0);

        checkLocationPermission();

        MyMapEventsReceiver mReceive = new MyMapEventsReceiver(this, map, this);
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(mReceive);
        map.getOverlays().add(eventsOverlay);

        ImageButton zoomInButton = findViewById(R.id.buttonZoomIn);
        ImageButton zoomOutButton = findViewById(R.id.buttonZoomOut);
        Button buttonRestaurants = findViewById(R.id.buttonRestaurants);
        Button buttonCafes = findViewById(R.id.buttonCafes);
        Button buttonMarkets = findViewById(R.id.buttonMarkets);
        Button buttonCenterLocation = findViewById(R.id.buttonCenter);
        Button buttonHelp = findViewById(R.id.buttonHelp);
        Button buttonBack = findViewById(R.id.buttonBack);

        zoomInButton.setOnClickListener(v -> map.getController().zoomIn());
        zoomOutButton.setOnClickListener(v -> map.getController().zoomOut());

        // añadir un marcador
        Marker startMarker = new Marker(map);
        startMarker.setPosition(defaultPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setTitle(getString(R.string.starting_point));
        map.getOverlays().add(startMarker);

        buttonRestaurants.setOnClickListener(v -> {
            showRestaurants = !showRestaurants;
            updateMarkers();
        });

        buttonCafes.setOnClickListener(v -> {
            showCafes = !showCafes;
            updateMarkers();
        });

        buttonMarkets.setOnClickListener(v -> {
            showMarkets = !showMarkets;
            updateMarkers();
        });

        buttonCenterLocation.setOnClickListener(v -> {
            if (locationOverlay != null && locationOverlay.getMyLocation() != null) {
                // getMyLocation devuelve la última ubicación conocida, si no null
                GeoPoint currentLocation = locationOverlay.getMyLocation();
                // centrar el mapa otra vez en la posición actual/la última posición del usuario
                map.getController().animateTo(currentLocation);
            } else {
                Toast.makeText(this, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show();
            }
        });

        buttonHelp.setOnClickListener(v -> {
            DialogFragment dialogoMapaInfo = new DialogMapInfo();
            dialogoMapaInfo.show(getSupportFragmentManager(), "etiqueta7");
        });

        buttonBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // notificar cuando haya cambios en la red (online u offline)
        networkChangeReceiver = new NetworkChangeReceiver(new NetworkChangeReceiver.NetworkListener() {
            @Override
            public void onNetworkAvailable() {
                Toast.makeText(MapActivity.this, getString(R.string.online), Toast.LENGTH_LONG).show();
                // si los puntos de interés no están ya marcados, se marcan
                if (locationOverlay != null && locationOverlay.getMyLocation() != null && !hasAlreadyLoadedPlaces) {
                    loadNearbyPlaces(locationOverlay.getMyLocation(), "restaurant");
                    loadNearbyPlaces(locationOverlay.getMyLocation(), "cafe");
                    loadNearbyPlaces(locationOverlay.getMyLocation(), "supermarket");
                    hasAlreadyLoadedPlaces = true;

                    // ampliar el área de scroll a 7km si hemos recuperado la conexión
                    BoundingBox newBox = getBoundingBox(locationOverlay.getMyLocation(), 7500);
                    map.setScrollableAreaLimitDouble(newBox);
                }
            }

            @Override
            public void onNetworkLost() {
                Toast.makeText(MapActivity.this, getString(R.string.offline), Toast.LENGTH_LONG).show();
            }
        });
        registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
        }
    }

    private void updateMarkers() {
        for (Marker m : restaurantMarkers) m.setEnabled(showRestaurants);
        for (Marker m : cafeMarkers) m.setEnabled(showCafes);
        for (Marker m : marketMarkers) m.setEnabled(showMarkets);

        Button buttonRestaurants = findViewById(R.id.buttonRestaurants);
        Button buttonCafes = findViewById(R.id.buttonCafes);
        Button buttonMarkets = findViewById(R.id.buttonMarkets);

        buttonRestaurants.setAlpha(showRestaurants ? 1.0f : 0.5f);
        buttonCafes.setAlpha(showCafes ? 1.0f : 0.5f);
        buttonMarkets.setAlpha(showMarkets ? 1.0f : 0.5f);

        map.invalidate();
    }

    private void checkLocationPermission() {
        // si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupUserLocation();
        }
        // si ya se ha solicitado el permiso anteriormente, pero el usuario lo ha rechazado
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            DialogFragment dialogoMapa = new DialogMap();
            dialogoMapa.show(getSupportFragmentManager(), "etiqueta6");
        }
        // solicitar permiso
        else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION},3);
        }
    }

    private void setupUserLocation() {
        // poner pantalla de carga
        View loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingOverlay.setVisibility(View.VISIBLE);

        // asegurar que el overlay de ubicación esté inicializado una sola vez
        if (locationOverlay == null) {
            GpsMyLocationProvider gpsProvider = new GpsMyLocationProvider(this);
            locationOverlay = new MyLocationNewOverlay(gpsProvider, map);
            map.getOverlays().add(locationOverlay);
            // obtener ubicación del usuario
            locationOverlay.enableMyLocation();
        }

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            GeoPoint location = locationOverlay.getMyLocation();
            Log.d("Location", "location: " + location);

            // usar LocationManager si osmdroid todavía no ha dado una ubicación
            if (location == null) {
                Location best = getLastBestLocation();
                Log.d("Location", "best: " + best);
                // última ubicación conocida reciente (2 minutos)
                if (best != null && System.currentTimeMillis() - best.getTime() < 2 * 60 * 1000) {
                    location = new GeoPoint(best.getLatitude(), best.getLongitude());
                }
            }

            // si tras 5 intentos no hay ubicación válida, detener y avisar al usuario
            if (location == null) {
                locationAttempts++;
                if (locationAttempts < 5) {
                    handler.postDelayed(this::setupUserLocation, 2000);
                } else {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_LONG).show();
                }
                return;
            }

            map.getController().setCenter(location);
            BoundingBox box = null;
            // si el usuario está online, se cargarán los marcadores en el mapa
            if (isOnline()) {
                loadNearbyPlaces(location, "restaurant");
                loadNearbyPlaces(location, "cafe");
                loadNearbyPlaces(location, "supermarket");
                hasAlreadyLoadedPlaces = true;
                // limitar el mapa visible al usuario a 7,5km en todas las direcciones
                box = getBoundingBox(location, 7500);
            } else {
                Toast.makeText(this, getString(R.string.offline_not_load), Toast.LENGTH_LONG).show();
                // limitar el mapa visible al usuario a 2km en todas las direcciones
                box = getBoundingBox(location, 2000);
            }

            map.setScrollableAreaLimitDouble(box);
            // quitar la pantalla de carga
            loadingOverlay.setVisibility(View.GONE);
        }, 3000); // volver a intentar en 3 segundos
    }

    private Location getLastBestLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l != null && (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy())) {
                    bestLocation = l;
                }
            }
        }
        return bestLocation;
    }

    private boolean isOnline() {
        // comprobar si el usuario está online
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private BoundingBox getBoundingBox(GeoPoint center, double rangeMeters) {
        // metros por grado
        double lonOffset = (rangeMeters / (111320f * Math.cos(Math.toRadians(defaultPoint.getLatitude()))));
        if (center != null) {
            lonOffset = (rangeMeters / (111320f * Math.cos(Math.toRadians(center.getLatitude()))));
        }
        double latOffset = (rangeMeters / 111320f);
        // aplicar el offset en todas las direcciones
        return new BoundingBox(
                center.getLatitude() + latOffset,
                center.getLongitude() + lonOffset,
                center.getLatitude() - latOffset,
                center.getLongitude() - lonOffset
        );
    }

    private void loadNearbyPlaces(GeoPoint location, String type) {
        new Thread(() -> {
            try {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                int radius = 1000; // metros

                // hacer un query a overpass API
                String query = "[out:json];node[\"amenity\"=\"" + type + "\"](around:" + radius + "," + lat + "," + lon + ");";
                if (type.equals("supermarket")) {
                    query = "[out:json];node[\"shop\"=\"supermarket\"](around:" + radius + "," + lat + "," + lon + ");";
                }

                query += "out;";
                String urlStr = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                // recoger y procesar la respuesta
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject result = new JSONObject(response.toString());
                JSONArray elements = result.getJSONArray("elements");

                runOnUiThread(() -> {
                    for (int i = 0; i < elements.length(); i++) {
                        try {
                            JSONObject elem = elements.getJSONObject(i);
                            double eLat = elem.getDouble("lat");
                            double eLon = elem.getDouble("lon");
                            JSONObject tags = elem.optJSONObject("tags");

                            String name = type;
                            if (tags != null) {
                                name = tags.optString("name", "");
                            }

                            // poner markers en los lugares devueltos
                            Marker m = new Marker(map);
                            m.setPosition(new GeoPoint(eLat, eLon));
                            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            m.setTitle(name.isEmpty() ? getString(R.string.unnamed) : name);
                            m.setIcon(AppUtils.getMarkerColor(this, type));
                            map.getOverlays().add(m);

                            if (type.equals("restaurant")) restaurantMarkers.add(m);
                            else if (type.equals("cafe")) cafeMarkers.add(m);
                            else marketMarkers.add(m);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    updateMarkers();
                    map.invalidate();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, getString(R.string.fetch_error), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 3) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupUserLocation();
            } else {
                // mostrar el punto predeterminado
                map.getController().setCenter(defaultPoint);
            }
        }
    }

    @Override
    public void loadPlacesFromPoint(GeoPoint p) {
        loadNearbyPlaces(p, "restaurant");
        loadNearbyPlaces(p, "cafe");
        loadNearbyPlaces(p, "supermarket");
    }

    @Override
    public void clearAllPlaceMarkers() {
        for (Marker m : restaurantMarkers) map.getOverlays().remove(m);
        for (Marker m : cafeMarkers) map.getOverlays().remove(m);
        for (Marker m : marketMarkers) map.getOverlays().remove(m);

        restaurantMarkers.clear();
        cafeMarkers.clear();
        marketMarkers.clear();
    }
}
