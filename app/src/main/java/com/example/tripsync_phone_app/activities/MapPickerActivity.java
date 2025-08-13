package com.example.tripsync_phone_app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tripsync_phone_app.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.divider.MaterialDividerItemDecoration;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity {

    private GoogleMap mMap;
    private Marker currentMarker;
    private FusedLocationProviderClient fused;

    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;

    private TextInputEditText inputSearch;
    private RecyclerView suggestionsList;
    private MaterialCardView suggestionsCard;
    private TextView emptyView;
    private PlacePredictionAdapter suggestionAdapter;
    private final Handler debounce = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;

    private final ActivityResultLauncher<String> finePermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) enableMyLocationAndCenter();
                else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        fused = LocationServices.getFusedLocationProviderClient(this);

        MaterialToolbar bar = findViewById(R.id.topAppBar);
        bar.setNavigationOnClickListener(v -> finish());

        // Initialize Places
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey, Locale.getDefault());
            }
            placesClient = Places.createClient(this);
            sessionToken = AutocompleteSessionToken.newInstance();
        } catch (Exception ignored) {}

        // Map fragment inside the fixed-height card
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.map_container, mapFragment)
                .commit();
        mapFragment.getMapAsync(googleMap -> { mMap = googleMap; setupMap(); });

        // Search + suggestions UI
        inputSearch = findViewById(R.id.inputSearch);
        suggestionsCard = findViewById(R.id.suggestionsCard);
        suggestionsList = findViewById(R.id.suggestionsList);
        emptyView = findViewById(R.id.emptyView);

        suggestionsList.setLayoutManager(new LinearLayoutManager(this));
        MaterialDividerItemDecoration div =
                new MaterialDividerItemDecoration(this, LinearLayoutManager.VERTICAL);
        div.setDividerInsetStart(16);
        div.setDividerInsetEnd(16);
        suggestionsList.addItemDecoration(div);

        suggestionAdapter = new PlacePredictionAdapter(this::onPredictionClicked);
        suggestionsList.setAdapter(suggestionAdapter);

        inputSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pendingSearch != null) debounce.removeCallbacks(pendingSearch);
                pendingSearch = () -> searchPredictions(s.toString().trim());
                debounce.postDelayed(pendingSearch, 250);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        inputSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                suggestionsCard.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
            }
        });

        // My location FAB
        FloatingActionButton fab = findViewById(R.id.fabMyLocation);
        fab.setOnClickListener(v -> centerOnDevice(true));

        // Zoom buttons
        FloatingActionButton fabZoomIn  = findViewById(R.id.fabZoomIn);
        FloatingActionButton fabZoomOut = findViewById(R.id.fabZoomOut);
        fabZoomIn.setOnClickListener(v -> { if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomIn()); });
        fabZoomOut.setOnClickListener(v -> { if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomOut()); });
    }

    private void setupMap() {
        // Use our own zoom buttons
        mMap.getUiSettings().setZoomControlsEnabled(false);

        mMap.setOnMapClickListener(latLng -> {
            addOrMoveMarker(latLng);
            String address = reverseGeocode(latLng);
            returnResult(address, latLng);
        });

        mMap.setOnMyLocationButtonClickListener(() -> {
            centerOnDevice(false);
            return false;
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 4f));
        enableMyLocationAndCenter();
    }

    private void enableMyLocationAndCenter() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            finePermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        mMap.setMyLocationEnabled(true);
        centerOnDevice(true);
    }

    private void centerOnDevice(boolean animate) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fused.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng me = new LatLng(location.getLatitude(), location.getLongitude());
                if (animate) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
                else mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
            }
        });
    }

    /** Search Places predictions and show list card under search */
    private void searchPredictions(String query) {
        if (placesClient == null || query.isEmpty()) {
            suggestionsCard.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            suggestionAdapter.submit(null);
            return;
        }

        LatLngBounds bounds = (mMap != null)
                ? mMap.getProjection().getVisibleRegion().latLngBounds
                : null;

        FindAutocompletePredictionsRequest.Builder b =
                FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setSessionToken(sessionToken);

        if (bounds != null) {
            b.setLocationBias(RectangularBounds.newInstance(bounds));
            b.setOrigin(bounds.getCenter());
        }

        placesClient.findAutocompletePredictions(b.build())
                .addOnSuccessListener(response -> {
                    List<AutocompletePrediction> list = response.getAutocompletePredictions();
                    suggestionAdapter.submit(list);
                    if (list == null || list.isEmpty()) {
                        suggestionsCard.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        suggestionsCard.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    suggestionsCard.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(R.string.place_fetch_failed);
                });
    }

    private void onPredictionClicked(AutocompletePrediction p) {
        hideKeyboard();
        suggestionsCard.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        inputSearch.setText(p.getPrimaryText(null));

        FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                p.getPlaceId(),
                Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        );

        placesClient.fetchPlace(req)
                .addOnSuccessListener(resp -> {
                    Place place = resp.getPlace();
                    LatLng latLng = place.getLatLng();
                    if (latLng != null) {
                        addOrMoveMarker(latLng);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
                        String address = (place.getAddress() != null) ? place.getAddress() : reverseGeocode(latLng);
                        returnResult(address, latLng);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.place_fetch_failed, Toast.LENGTH_SHORT).show());
    }

    private void addOrMoveMarker(LatLng latLng) {
        if (currentMarker == null) {
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.selected_location))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        } else {
            currentMarker.setPosition(latLng);
        }
    }

    private String reverseGeocode(LatLng latLng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (list != null && !list.isEmpty()) return list.get(0).getAddressLine(0);
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

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
