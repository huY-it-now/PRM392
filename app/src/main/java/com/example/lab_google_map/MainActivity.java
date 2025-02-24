package com.example.lab_google_map;

import androidx.appcompat.widget.SearchView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.graphics.Color;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    private GoogleMap myMap;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    SearchView searchView;
    Button btnExit;
    LocationCallback locationCallback;
    private List<LatLng> measurePoints = new ArrayList<>();
    private Polyline measureLine;
    private boolean isMeasuring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        searchView = findViewById(R.id.search_view);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        FloatingActionButton gpsButton = findViewById(R.id.gpsButton);
        gpsButton.setImageResource(R.drawable.ic_gps);
        gpsButton.setOnClickListener(v -> moveCameraToCurrentLocation());

        FloatingActionButton zoomInButton = findViewById(R.id.zoomInButton);
        FloatingActionButton zoomOutButton = findViewById(R.id.zoomOutButton);
        
        zoomInButton.setImageResource(android.R.drawable.ic_menu_add);
        zoomOutButton.setImageResource(R.drawable.ic_zoom_out);

        zoomInButton.setOnClickListener(v -> {
            if (myMap != null) {
                myMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        zoomOutButton.setOnClickListener(v -> {
            if (myMap != null) {
                myMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        FloatingActionButton measureButton = findViewById(R.id.measureButton);
        measureButton.setImageResource(R.drawable.ic_measure);
        measureButton.setOnClickListener(v -> toggleMeasuring());

        getLastLocation();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (myMap != null) {
                    myMap.clear();
                    myMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                    myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveCameraToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (myMap != null) {
                    myMap.clear();
                    myMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                }
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchLocation(String locationName) {
        Geocoder geocoder = new Geocoder(MainActivity.this);
        List<Address> addressList = null;
        try {
            addressList = geocoder.getFromLocationName(locationName, 1);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Error retrieving location", Toast.LENGTH_SHORT).show();
        }

        if (addressList != null && !addressList.isEmpty()) {
            Address selectedAddress = addressList.get(0);
            LatLng selectedLocation = new LatLng(selectedAddress.getLatitude(), selectedAddress.getLongitude());
            putRedMarkerAndMoveCamera(selectedLocation, selectedAddress.getAddressLine(0));
        } else {
            Toast.makeText(MainActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void putRedMarkerAndMoveCamera(LatLng latLng, String title) {
        myMap.clear();
        myMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        myMap.animateCamera(CameraUpdateFactory.newLatLng(latLng), 1000, null);
    }

    private void startLocationSharing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        myMap.setMyLocationEnabled(true);
        myMap.getUiSettings().setMyLocationButtonEnabled(false);

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null && myMap != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    float bearing = location.getBearing();

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(currentLatLng)
                            .zoom(25)
                            .tilt(65)
                            .bearing(bearing)
                            .build();
                    myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
                }
            }
        };

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, new Handler().getLooper());
    }

    private void stopLocationSharing() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(false);
        }

        if (myMap != null && currentLocation != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                    .zoom(15)
                    .tilt(0)
                    .bearing(0)
                    .build();
            myMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000, null);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
        myMap.getUiSettings().setCompassEnabled(true);
        myMap.getUiSettings().setZoomControlsEnabled(false);
        myMap.getUiSettings().setZoomGesturesEnabled(true);

        myMap.setOnMapClickListener(latLng -> {
            if (isMeasuring) {
                handleMeasureClick(latLng);
            } else {
                putRedMarkerAndMoveCamera(latLng, "Selected Location");
            }
        });

        getLastLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mapNormal) {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if (id == R.id.mapHybird) {
            myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        if (id == R.id.mapSattelite) {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (id == R.id.mapTerrain) {
            myMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleMeasuring() {
        isMeasuring = !isMeasuring;
        if (!isMeasuring) {
            measurePoints.clear();
            if (measureLine != null) {
                measureLine.remove();
            }
        }
        Toast.makeText(this, 
            isMeasuring ? "Measuring mode activated" : "Measuring mode deactivated", 
            Toast.LENGTH_SHORT).show();
    }

    private void handleMeasureClick(LatLng point) {
        measurePoints.add(point);
        
        myMap.addMarker(new MarkerOptions()
            .position(point)
            .title("Point " + measurePoints.size())
            .icon(BitmapDescriptorFactory.defaultMarker(
                measurePoints.size() == 1 ? BitmapDescriptorFactory.HUE_GREEN : BitmapDescriptorFactory.HUE_RED)));
        
        if (measurePoints.size() == 2) {
            if (measureLine != null) {
                measureLine.remove();
            }
            
            measureLine = myMap.addPolyline(new PolylineOptions()
                .add(measurePoints.get(0), measurePoints.get(1))
                .width(5)
                .color(Color.BLUE));
            
            float[] results = new float[1];
            Location.distanceBetween(
                measurePoints.get(0).latitude, measurePoints.get(0).longitude,
                measurePoints.get(1).latitude, measurePoints.get(1).longitude,
                results);
            
            float distance = results[0];
            String distanceText;
            if (distance >= 1000) {
                distanceText = String.format("Distance: %.2f km", distance/1000);
            } else {
                distanceText = String.format("Distance: %.0f m", distance);
            }
            Toast.makeText(this, distanceText, Toast.LENGTH_LONG).show();
            
            new Handler().postDelayed(() -> {
                measurePoints.clear();
                myMap.clear();
                if (measureLine != null) {
                    measureLine.remove();
                }
            }, 3000);
        }
    }
}