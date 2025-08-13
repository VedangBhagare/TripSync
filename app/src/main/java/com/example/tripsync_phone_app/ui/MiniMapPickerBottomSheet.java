package com.example.tripsync_phone_app.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.adapter.PlacesAutoCompleteAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MiniMapPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnPlaceChosen {
        void onChosen(String address, double lat, double lng);
    }

    private final PlacesClient placesClient;
    private final LatLng initial;
    private final OnPlaceChosen callback;

    private GoogleMap map;
    private LatLng selected;

    public MiniMapPickerBottomSheet(PlacesClient pc, @Nullable LatLng start, OnPlaceChosen cb) {
        this.placesClient = pc;
        this.initial = start;
        this.callback = cb;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        View root = inflater.inflate(R.layout.activity_mini_map_picker_bottom_sheet, parent, false);

        AutoCompleteTextView search = root.findViewById(R.id.autoSearch);
        MaterialButton btnDone = root.findViewById(R.id.btnDone);

        // Uber-like suggestions in the sheet
        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(requireContext(), placesClient);
        search.setAdapter(adapter);
        search.setOnItemClickListener((p, v, pos, id) -> {
            String placeId = adapter.getPrediction(pos).getPlaceId();
            FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                    placeId,
                    Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS)
            );
            placesClient.fetchPlace(req)
                    .addOnSuccessListener(response -> {
                        Place place = response.getPlace();
                        if (place.getAddress() != null) {
                            search.setText(place.getAddress());
                            search.setSelection(search.getText().length());
                        }
                        if (place.getLatLng() != null) {
                            selected = place.getLatLng();   // remember
                            movePin(place.getLatLng());      // show on mini map
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        // Embed the small map
        SupportMapFragment mf = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        if (mf == null) {
            mf = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapFragment, mf)
                    .commitNow();
        }

        mf.getMapAsync(googleMap -> {
            map = googleMap;

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                map.setMyLocationEnabled(true);
            }

            LatLng start = (initial != null) ? initial : new LatLng(20.5937, 78.9629);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, (initial != null) ? 15f : 5f));

            // Tap anywhere on the mini map to pick
            map.setOnMapClickListener(this::movePin);
        });

        btnDone.setOnClickListener(v -> {
            if (selected == null) {
                Toast.makeText(requireContext(), R.string.select_on_map, Toast.LENGTH_SHORT).show();
                return;
            }
            String addr = reverseGeocode(requireContext(), selected);
            callback.onChosen(addr, selected.latitude, selected.longitude);
            dismiss();
        });

        return root;
    }

    private void movePin(LatLng latLng) {
        selected = latLng;
        if (map == null) return;
        map.clear();
        map.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.selected_location)));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
    }

    private static String reverseGeocode(Context ctx, LatLng ll) {
        try {
            Geocoder g = new Geocoder(ctx, Locale.getDefault());
            List<Address> list = g.getFromLocation(ll.latitude, ll.longitude, 1);
            if (list != null && !list.isEmpty()) {
                return list.get(0).getAddressLine(0);
            }
        } catch (IOException ignored) {}
        return ll.latitude + ", " + ll.longitude;
    }
}
