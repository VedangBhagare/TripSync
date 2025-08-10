package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.adapter.PlacesAutoCompleteAdapter;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ActivityAddItineraryBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Arrays;
import java.util.Locale;

public class AddItineraryActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityAddItineraryBinding binding;
    private AppDatabase db;
    private int userId;

    private PlacesClient placesClient;
    private int destinationCount = 0;

    private GoogleMap previewMap;
    private View currentMapContainer;

    private final ActivityResultLauncher<Intent> mapPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String addr = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    if (currentMapContainer != null) {
                        AutoCompleteTextView auto = currentMapContainer.findViewById(R.id.autoAddress);
                        auto.setText(addr);
                        movePreviewMarker(new LatLng(lat, lng));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        userId = getSharedPreferences("TripSyncPrefs", MODE_PRIVATE).getInt("user_id", -1);

        binding.topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            if (!Places.isInitialized() && apiKey != null && !apiKey.isEmpty()) {
                Places.initialize(getApplicationContext(), apiKey);
            }
        } catch (Exception ignored) {}
        placesClient = Places.createClient(this);

        binding.btnAddDestination.setOnClickListener(this);
        binding.btnConfirmSave.setOnClickListener(this);
        updateAddButtonLabel();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.btnAddDestination.getId()) {
            addDestinationCard(null);
        } else if (v.getId() == binding.btnConfirmSave.getId()) {
            saveItinerary();
        }
    }

    private void updateAddButtonLabel() {
        binding.btnAddDestination.setText("Add destination " + (destinationCount + 1));
    }

    private void addDestinationCard(@Nullable String address) {
        destinationCount++;
        updateAddButtonLabel();

        View card = getLayoutInflater().inflate(R.layout.destination_card, binding.destinationContainer, false);

        TextView title = card.findViewById(R.id.txtTitle);
        title.setText("Destination " + destinationCount);

        ImageButton btnClose = card.findViewById(R.id.btnClose);
        AutoCompleteTextView auto = card.findViewById(R.id.autoAddress);
        TextInputEditText note = card.findViewById(R.id.editNote);
        MaterialButton dateBtn = card.findViewById(R.id.btnSelectDate);
        MaterialButton timeBtn = card.findViewById(R.id.btnSelectTime);
        MaterialButton btnUseMap = card.findViewById(R.id.btnUseMap);
        MaterialButton btnClear = card.findViewById(R.id.btnClear);

        if (address != null) auto.setText(address);

        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this, placesClient);
        auto.setAdapter(adapter);
        auto.setOnItemClickListener((parent, view, position, id) -> {
            String placeId = adapter.getPrediction(position).getPlaceId();
            FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                    placeId, Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS)
            );
            placesClient.fetchPlace(req).addOnSuccessListener(resp -> {
                Place p = resp.getPlace();
                auto.setText(p.getAddress());
                if (p.getLatLng() != null) {
                    movePreviewMarker(p.getLatLng());
                }
            });
        });

        btnUseMap.setOnClickListener(v -> {
            currentMapContainer = card;
            Intent i = new Intent(this, MapPickerActivity.class);
            mapPicker.launch(i);
        });

        btnClear.setOnClickListener(v -> auto.setText(""));

        btnClose.setOnClickListener(v -> {
            binding.destinationContainer.removeView(card);
            destinationCount = Math.max(0, destinationCount - 1);
            renumberCards();
            updateAddButtonLabel();
        });

        dateBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .build();
            picker.addOnPositiveButtonClickListener(sel -> dateBtn.setText(picker.getHeaderText()));
            picker.show(getSupportFragmentManager(), "DATE");
        });

        timeBtn.setOnClickListener(v -> {
            MaterialTimePicker tp = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12).setMinute(0)
                    .setTitleText(getString(R.string.select_time))
                    .build();
            tp.addOnPositiveButtonClickListener(v1 ->
                    timeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute())));
            tp.show(getSupportFragmentManager(), "TIME");
        });

        binding.destinationContainer.addView(card);
    }

    private void renumberCards() {
        for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
            View c = binding.destinationContainer.getChildAt(i);
            TextView t = c.findViewById(R.id.txtTitle);
            t.setText("Destination " + (i + 1));
        }
    }

    private void movePreviewMarker(LatLng latLng) {
        if (previewMap == null) {
            SupportMapFragment frag = SupportMapFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.mapPreviewContainer, frag)
                    .commit();
            frag.getMapAsync(map -> {
                previewMap = map;
                previewMap.addMarker(new MarkerOptions().position(latLng).title("Selected"));
                previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            });
        } else {
            previewMap.clear();
            previewMap.addMarker(new MarkerOptions().position(latLng).title("Selected"));
            previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
    }

    private void saveItinerary() {
        String tripName = binding.editTripName.getText() == null ? "" :
                binding.editTripName.getText().toString().trim();

        if (tripName.isEmpty()) {
            Toast.makeText(this, getString(R.string.trip_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId <= 0) {
            Toast.makeText(this, "Invalid user. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        Itinerary it = new Itinerary();
        it.tripName = tripName;
        it.userId = userId;
        long itId = db.itineraryDao().insertItinerary(it);

        for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
            View card = binding.destinationContainer.getChildAt(i);

            String address = ((AutoCompleteTextView) card.findViewById(R.id.autoAddress))
                    .getText().toString().trim();
            String note = textOf((TextInputEditText) card.findViewById(R.id.editNote));
            String date = ((MaterialButton) card.findViewById(R.id.btnSelectDate)).getText().toString().trim();
            String time = ((MaterialButton) card.findViewById(R.id.btnSelectTime)).getText().toString().trim();

            if (address.isEmpty() && note.isEmpty() && date.isEmpty() && time.isEmpty()) continue;

            Destination d = new Destination();
            d.itineraryId = (int) itId;
            d.address = address;
            d.note = note;
            d.date = date;
            d.time = time;
            db.destinationDao().insertDestination(d);
        }

        Toast.makeText(this, getString(R.string.itinerary_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
}
