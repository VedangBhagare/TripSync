package com.example.tripsync_phone_app.activities;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.adapter.PlacesAutoCompleteAdapter;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ActivityAddItineraryBinding;
import com.example.tripsync_phone_app.notify.ReminderScheduler; // <-- NEW
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.example.tripsync_phone_app.notify.ReminderScheduler;

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
                        auto.setText(addr, false);
                        auto.dismissDropDown();
                        auto.clearFocus();
                        hideKeyboard(auto);
                        movePreviewMarker(new LatLng(lat, lng));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Make window resize when keyboard shows
        getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE);
        setupImeAwareBottomNav();   // hide bottom bar on IME & pad scroll

        // Toolbar back -> Home
        MaterialToolbar bar = findViewById(R.id.topAppBar);
        if (bar != null) bar.setNavigationOnClickListener(v -> navigateHome());

        // System back -> Home
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { navigateHome(); }
        });

        // Bottom nav destinations
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) { navigateHome(); return true; }
                if (id == R.id.nav_itineraries) {
                    startActivity(new Intent(this, ItinerariesActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, UserProfileActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }

        // DB & user
        db = AppDatabase.getInstance(this);
        userId = getSharedPreferences("TripSyncPrefs", MODE_PRIVATE).getInt("user_id", -1);

        // Places init
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            if (!Places.isInitialized() && apiKey != null && !apiKey.isEmpty()) {
                Places.initialize(getApplicationContext(), apiKey);
            }
        } catch (Exception ignored) { }
        placesClient = Places.createClient(this);

        // UI actions
        binding.btnAddDestination.setOnClickListener(this);
        binding.btnConfirmSave.setOnClickListener(this);
        updateAddButtonLabel();
    }

    /** IME-aware bottom nav + padding so fields never hide behind keyboard or nav bar. */
    private void setupImeAwareBottomNav() {
        View root = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());

            // Hide bottom nav while keyboard shown
            binding.bottomNav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);

            // Add bottom padding to scroll content
            int navH = binding.bottomNav.getHeight();
            int bottomPad = imeVisible ? imeInsets.bottom : (navH + dp(16));
            if (binding.scrollContent.getPaddingBottom() != bottomPad) {
                binding.scrollContent.setPadding(
                        binding.scrollContent.getPaddingLeft(),
                        binding.scrollContent.getPaddingTop(),
                        binding.scrollContent.getPaddingRight(),
                        bottomPad
                );
            }
            return insets;
        });

        // initial padding when IME not visible yet
        binding.scrollContent.post(() -> {
            int navH = binding.bottomNav.getHeight();
            binding.scrollContent.setPadding(
                    binding.scrollContent.getPaddingLeft(),
                    binding.scrollContent.getPaddingTop(),
                    binding.scrollContent.getPaddingRight(),
                    navH + dp(16)
            );
        });
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void navigateHome() {
        Intent i = new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
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
        binding.btnAddDestination.setText(getString(R.string.add_destination_n, destinationCount + 1));
    }

    private void addDestinationCard(@Nullable String address) {
        destinationCount++;
        updateAddButtonLabel();

        View card = getLayoutInflater().inflate(R.layout.destination_card, binding.destinationContainer, false);

        TextView title = card.findViewById(R.id.txtTitle);
        title.setText(getString(R.string.destination_n, destinationCount));

        ImageButton btnClose = card.findViewById(R.id.btnClose);
        AutoCompleteTextView auto = card.findViewById(R.id.autoAddress);
        TextInputEditText note = card.findViewById(R.id.editNote);
        MaterialButton dateBtn = card.findViewById(R.id.btnSelectDate);
        MaterialButton timeBtn = card.findViewById(R.id.btnSelectTime);
        MaterialButton btnUseMap = card.findViewById(R.id.btnUseMap);
        MaterialButton btnClear = card.findViewById(R.id.btnClear);

        if (address != null) auto.setText(address, false);

        // Places dropdown
        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this, placesClient);
        auto.setAdapter(adapter);
        auto.setThreshold(1);

        final boolean[] suppress = { false };
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppress[0] && auto.hasFocus() && !auto.isPerformingCompletion() && s.length() >= 1) {
                    auto.showDropDown();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        auto.addTextChangedListener(watcher);
        auto.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) auto.dismissDropDown(); });

        auto.setOnItemClickListener((parent, view, position, id) -> {
            String placeId = adapter.getPrediction(position).getPlaceId();
            FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                    placeId, Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS)
            );
            placesClient.fetchPlace(req).addOnSuccessListener(resp -> {
                Place p = resp.getPlace();
                suppress[0] = true;
                auto.setText(p.getAddress(), false);
                auto.dismissDropDown();
                auto.clearFocus();
                hideKeyboard(auto);
                auto.postDelayed(() -> suppress[0] = false, 150);

                if (p.getLatLng() != null) movePreviewMarker(new LatLng(p.getLatLng().latitude, p.getLatLng().longitude));
            });
        });

        btnUseMap.setOnClickListener(v -> {
            currentMapContainer = card;
            mapPicker.launch(new Intent(this, MapPickerActivity.class));
        });

        btnClear.setOnClickListener(v -> {
            auto.setText("", false);
            showKeyboard(auto);
        });

        btnClose.setOnClickListener(v -> {
            auto.removeTextChangedListener(watcher);
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
            t.setText(getString(R.string.destination_n, i + 1));
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
                // Non-interactive so it doesn't fight with scroll
                previewMap.getUiSettings().setAllGesturesEnabled(false);
                previewMap.getUiSettings().setZoomControlsEnabled(false);
                previewMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.selected)));
                previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            });
            findViewById(R.id.mapPreviewCard).setVisibility(View.VISIBLE);
        } else {
            previewMap.clear();
            previewMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.selected)));
            previewMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }
    }

    private void saveItinerary() {
        String tripName = binding.editTripName.getText() == null
                ? "" : binding.editTripName.getText().toString().trim();

        if (tripName.isEmpty()) {
            Toast.makeText(this, getString(R.string.trip_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId <= 0) {
            Toast.makeText(this, R.string.invalid_user, Toast.LENGTH_SHORT).show();
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

            long rowId = db.destinationDao().insertDestination(d); // <-- CHANGED (capture id)

            // === NEW: schedule a reminder 1 hour before ===

        }

        ReminderScheduler.scheduleForItinerary(this, (int) itId);

        Toast.makeText(this, R.string.itinerary_saved, Toast.LENGTH_SHORT).show();
        com.example.tripsync_phone_app.notify.ReminderScheduler.scheduleForItinerary(
                this, (int) itId
        );
        finish();
    }

    private static String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            v.post(() -> {
                v.requestFocus();
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            });
        }
    }
}
