package com.example.tp13;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MainActivity for TP13: Campus Guide with Geolocation.
 * Demonstrates Google Maps SDK integration, Sidi Amar Campus locations,
 * location permissions, user tracking, distance calculation, polyline drawing,
 * and external navigation intent.
 */
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnInfoWindowClickListener {

    // Sidi Amar Campus default center coordinates (Université Badji Mokhtar - Annaba)
    private static final double CAMPUS_LAT = 36.8175;
    private static final double CAMPUS_LNG = 7.7050;
    private static final float DEFAULT_ZOOM = 16.0f;

    // UI Components
    private RecyclerView rvLocations;
    private FloatingActionButton btnRecenter;
    private MaterialCardView cardPermissionBanner;
    private MaterialCardView cardDestinationInfo;
    private Button btnRequestPermission;
    private ImageView imgSelectedIcon;
    private TextView tvSelectedName;
    private TextView tvSelectedDesc;
    private TextView tvDistance;
    private LinearLayout layoutActions;
    private Button btnDrawRoute;
    private Button btnNavigate;

    // Data & Adapters
    private List<CampusLocation> locationList;
    private LocationAdapter locationAdapter;
    private CampusLocation selectedLocation;

    // Google Maps & Location
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentUserLocation;
    private Polyline currentPolyline;
    private boolean isRouteDrawn = false;

    // Mapping markers to location objects
    private final Map<String, Marker> locationMarkerMap = new HashMap<>();
    private final Map<Marker, CampusLocation> markerLocationMap = new HashMap<>();

    // Runtime Permission Launcher
    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                    // Permission Granted
                    cardPermissionBanner.setVisibility(View.GONE);
                    enableUserLocation();
                    startLocationUpdates();
                    Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission Denied
                    cardPermissionBanner.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Permission denied. Location tracking disabled.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // System Bars Inset handling for comfortable top spacing
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI Elements
        initViews();

        // Setup Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Load Sidi Amar Campus Location Data
        locationList = CampusLocation.getSidiAmarLocations();

        // Setup RecyclerView Location List
        setupRecyclerView();

        // Initialize SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup Click Listeners
        setupClickListeners();

        // Check and Request Location Permissions
        checkLocationPermissions();
    }

    /**
     * Bind view references from layout XML.
     */
    private void initViews() {
        rvLocations = findViewById(R.id.rvLocations);
        btnRecenter = findViewById(R.id.btnRecenter);
        cardPermissionBanner = findViewById(R.id.cardPermissionBanner);
        cardDestinationInfo = findViewById(R.id.cardDestinationInfo);
        btnRequestPermission = findViewById(R.id.btnRequestPermission);
        imgSelectedIcon = findViewById(R.id.imgSelectedIcon);
        tvSelectedName = findViewById(R.id.tvSelectedName);
        tvSelectedDesc = findViewById(R.id.tvSelectedDesc);
        tvDistance = findViewById(R.id.tvDistance);
        layoutActions = findViewById(R.id.layoutActions);
        btnDrawRoute = findViewById(R.id.btnDrawRoute);
        btnNavigate = findViewById(R.id.btnNavigate);
    }

    /**
     * Configure horizontal RecyclerView for campus location cards.
     */
    private void setupRecyclerView() {
        rvLocations.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        locationAdapter = new LocationAdapter(locationList, (location, position) -> selectCampusLocation(location, true));
        rvLocations.setAdapter(locationAdapter);
    }

    /**
     * Set up UI action button listeners.
     */
    private void setupClickListeners() {
        // Recenter button
        btnRecenter.setOnClickListener(v -> recenterUserLocation());

        // Permission banner button
        btnRequestPermission.setOnClickListener(v -> checkLocationPermissions());

        // Toggle Polyline Route
        btnDrawRoute.setOnClickListener(v -> toggleRoutePolyline());

        // External Navigation App Intent
        btnNavigate.setOnClickListener(v -> launchExternalNavigation());
    }

    /**
     * Google Maps SDK ready callback.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Configure map settings
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Center map on Sidi Amar Campus
        LatLng sidiAmar = new LatLng(CAMPUS_LAT, CAMPUS_LNG);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sidiAmar, DEFAULT_ZOOM));

        // Add campus markers
        addCampusMarkers();

        // Listen for marker and InfoWindow clicks
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);

        // Enable user location on map if permission granted
        if (hasLocationPermission()) {
            enableUserLocation();
            startLocationUpdates();
        }
    }

    /**
     * Add markers for all important Sidi Amar Campus locations.
     */
    private void addCampusMarkers() {
        if (googleMap == null || locationList == null) return;

        locationMarkerMap.clear();
        markerLocationMap.clear();

        for (CampusLocation location : locationList) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location.getLatLng())
                    .title(location.getName())
                    .snippet(location.getDescription())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

            Marker marker = googleMap.addMarker(markerOptions);
            if (marker != null) {
                locationMarkerMap.put(location.getId(), marker);
                markerLocationMap.put(marker, location);
            }
        }
    }

    /**
     * Handle marker click on Google Map.
     */
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        CampusLocation location = markerLocationMap.get(marker);
        if (location != null) {
            selectCampusLocation(location, false);
            locationAdapter.setSelectedLocation(location);

            // Scroll horizontal list to selected location
            int position = locationList.indexOf(location);
            if (position >= 0) {
                rvLocations.smoothScrollToPosition(position);
            }
        }
        return false; // Return false to allow default InfoWindow behavior
    }

    /**
     * Handle InfoWindow click on Google Map.
     */
    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {
        CampusLocation location = markerLocationMap.get(marker);
        if (location != null) {
            Toast.makeText(this, "Selected: " + location.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Select a location: animate camera, open InfoWindow, and update info card.
     */
    private void selectCampusLocation(CampusLocation location, boolean animateCamera) {
        this.selectedLocation = location;

        // Update Info Card Views
        imgSelectedIcon.setImageResource(location.getIconResId());
        tvSelectedName.setText(location.getName());
        tvSelectedDesc.setText(location.getDescription());
        tvSelectedDesc.setVisibility(View.VISIBLE);
        layoutActions.setVisibility(View.VISIBLE);

        // Animate Camera to Location & Show InfoWindow
        if (googleMap != null) {
            if (animateCamera) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location.getLatLng(), 17.0f));
            }
            Marker marker = locationMarkerMap.get(location.getId());
            if (marker != null) {
                marker.showInfoWindow();
            }
        }

        // Calculate and update distance
        updateDistanceDisplay();

        // Refresh polyline route if currently drawn
        if (isRouteDrawn) {
            drawRoutePolyline();
        }
    }

    /**
     * Calculate and display distance between user location and selected destination.
     */
    private void updateDistanceDisplay() {
        if (selectedLocation == null) {
            tvDistance.setVisibility(View.GONE);
            return;
        }

        if (currentUserLocation == null) {
            tvDistance.setVisibility(View.VISIBLE);
            tvDistance.setText(getString(R.string.location_unavailable));
            return;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                currentUserLocation.getLatitude(), currentUserLocation.getLongitude(),
                selectedLocation.getLatitude(), selectedLocation.getLongitude(),
                results
        );

        float distanceInMeters = results[0];
        String formattedDistance;

        if (distanceInMeters < 1000) {
            formattedDistance = String.format(Locale.US, "%.0f m", distanceInMeters);
        } else {
            formattedDistance = String.format(Locale.US, "%.2f km", distanceInMeters / 1000.0f);
        }

        tvDistance.setVisibility(View.VISIBLE);
        tvDistance.setText(getString(R.string.distance_format, formattedDistance));
    }

    /**
     * Recenter map on user's current location.
     */
    private void recenterUserLocation() {
        if (!hasLocationPermission()) {
            checkLocationPermissions();
            return;
        }

        // Check if GPS is enabled on device
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            Toast.makeText(this, getString(R.string.gps_disabled_warning), Toast.LENGTH_LONG).show();
            return;
        }

        if (currentUserLocation != null && googleMap != null) {
            LatLng userLatLng = new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 17.0f));
            Toast.makeText(this, "Centered on current location", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show();
            // Fetch last known location as immediate fallback
            tryFetchLastLocation();
        }
    }

    /**
     * Toggle route line (Polyline) on map.
     */
    private void toggleRoutePolyline() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Select a destination first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUserLocation == null) {
            Toast.makeText(this, "User location unavailable. Enable GPS to draw route.", Toast.LENGTH_LONG).show();
            return;
        }

        if (isRouteDrawn) {
            clearRoutePolyline();
        } else {
            drawRoutePolyline();
        }
    }

    /**
     * Draw Polyline connecting user location to selected destination.
     */
    private void drawRoutePolyline() {
        if (googleMap == null || currentUserLocation == null || selectedLocation == null) return;

        if (currentPolyline != null) {
            currentPolyline.remove();
        }

        LatLng userLatLng = new LatLng(currentUserLocation.getLatitude(), currentUserLocation.getLongitude());
        LatLng destLatLng = selectedLocation.getLatLng();

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(userLatLng, destLatLng)
                .width(10f)
                .color(ContextCompat.getColor(this, R.color.polyline_color))
                .geodesic(true);

        currentPolyline = googleMap.addPolyline(polylineOptions);
        isRouteDrawn = true;
        btnDrawRoute.setText(getString(R.string.btn_clear_line));
    }

    /**
     * Clear route Polyline from map.
     */
    private void clearRoutePolyline() {
        if (currentPolyline != null) {
            currentPolyline.remove();
            currentPolyline = null;
        }
        isRouteDrawn = false;
        btnDrawRoute.setText(getString(R.string.btn_draw_line));
    }

    /**
     * Launch external navigation app (Google Maps intent).
     */
    private void launchExternalNavigation() {
        if (selectedLocation == null) {
            Toast.makeText(this, "Select a destination first", Toast.LENGTH_SHORT).show();
            return;
        }

        double destLat = selectedLocation.getLatitude();
        double destLng = selectedLocation.getLongitude();
        String name = selectedLocation.getName();

        // Create Google Maps navigation intent
        String navUriStr = String.format(Locale.US, "google.navigation:q=%f,%f", destLat, destLng);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(navUriStr));
        intent.setPackage("com.google.android.apps.maps");

        if (intent.resolveActivity(getPackageManager()) == null) {
            // Fallback geo: intent if Google Maps app is not installed
            String geoUriStr = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                    destLat, destLng, destLat, destLng, Uri.encode(name));
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUriStr));
        }

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch navigation application", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Safely enable Google Maps My Location layer.
     */
    private void enableUserLocation() {
        if (googleMap != null && hasLocationPermission()) {
            try {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false); // We use custom FAB
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Start continuous location updates using FusedLocationProviderClient.
     */
    private void startLocationUpdates() {
        if (!hasLocationPermission()) return;

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    currentUserLocation = locationResult.getLastLocation();
                    updateDistanceDisplay();
                    if (isRouteDrawn) {
                        drawRoutePolyline();
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            tryFetchLastLocation();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetch last known location for immediate response before regular updates arrive.
     */
    private void tryFetchLastLocation() {
        if (!hasLocationPermission()) return;
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentUserLocation = location;
                    updateDistanceDisplay();
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if location permissions are granted.
     */
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request runtime permissions if not granted.
     */
    private void checkLocationPermissions() {
        if (hasLocationPermission()) {
            cardPermissionBanner.setVisibility(View.GONE);
            enableUserLocation();
            startLocationUpdates();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }
}