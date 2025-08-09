package com.example.tripsync_phone_app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.tripsync_phone_app.R;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private static final int RC_AUTOCOMPLETE = 2001;

    private GoogleMap mMap;
    private Marker currentMarker;
    private FusedLocationProviderClient fused;

    private final ActivityResultLauncher<String> finePermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) enableMyLocationAndCenter();
                else Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fused = LocationServices.getFusedLocationProviderClient(this);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        bar.setNavigationOnClickListener(v -> onBackPressed());
        bar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                openPlacesSearch();
                return true;
            }
            return false;
        });

        // Initialize Places with API key from AndroidManifest meta-data
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey, Locale.getDefault());
            }
        } catch (Exception ignored) {}

        // Load map
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();

        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;
            setupMap();
        });
    }

    private void setupMap() {
        // Tap to drop marker + return result
        mMap.setOnMapClickListener(latLng -> {
            addOrMoveMarker(latLng);
            String address = reverseGeocode(latLng);
            returnResult(address, latLng);
        });

        // My-location button recenters
        mMap.setOnMyLocationButtonClickListener(() -> {
            centerOnDevice(false);
            return false;
        });

        // Default camera while we wait
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 4f));

        enableMyLocationAndCenter();
    }

    private void enableMyLocationAndCenter() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finePermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        mMap.setMyLocationEnabled(true);
        centerOnDevice(true);
    }

    private void centerOnDevice(boolean animate) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fused.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                if (animate) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
                }
            } // else keep default camera (GPS may still be warming up)
        });
    }

    private void openPlacesSearch() {
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY,
                Arrays.asList(
                        com.google.android.libraries.places.api.model.Place.Field.ID,
                        com.google.android.libraries.places.api.model.Place.Field.NAME,
                        com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
                        com.google.android.libraries.places.api.model.Place.Field.ADDRESS
                )
        ).build(this);
        startActivityForResult(intent, RC_AUTOCOMPLETE);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK && data != null) {
                com.google.android.libraries.places.api.model.Place place = Autocomplete.getPlaceFromIntent(data);
                LatLng latLng = place.getLatLng();
                if (latLng != null) {
                    addOrMoveMarker(latLng);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                    String address = place.getAddress() != null ? place.getAddress() : reverseGeocode(latLng);
                    returnResult(address, latLng);
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Toast.makeText(this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addOrMoveMarker(LatLng latLng) {
        if (currentMarker == null) {
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            currentMarker.setPosition(latLng);
        }
    }

    private String reverseGeocode(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (list != null && !list.isEmpty()) {
                return list.get(0).getAddressLine(0);
            }
        } catch (IOException ignored) {}
        return latLng.latitude + ", " + latLng.longitude;
    }

    private void returnResult(String address, LatLng latLng) {
        Intent result = new Intent();
        result.putExtra("address", address);
        result.putExtra("lat", latLng.latitude);
        result.putExtra("lng", latLng.longitude);
        setResult(RESULT_OK, result);
        finish();
    }
}
