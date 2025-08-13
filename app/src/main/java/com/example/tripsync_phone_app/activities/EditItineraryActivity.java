package com.example.tripsync_phone_app.activities;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;

import com.example.tripsync_phone_app.notify.ReminderScheduler;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.tripsync_phone_app.databinding.ActivityEditItineraryBinding;
import com.example.tripsync_phone_app.notify.ReminderScheduler; // <-- NEW
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditItineraryActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityEditItineraryBinding binding;
    private AppDatabase db;
    private int itineraryId;

    private PlacesClient placesClient;
    private GoogleMap previewMap, overlayMap;
    private boolean previewLoadedOnce = false, overlayLoadedOnce = false;

    private View currentMapContainer;
    private int destinationCount = 0;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Map<View, LatLng> cardLatLng = new HashMap<>();

    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable pendingRender;
    private String lastPointsSignature = "";

    private final ActivityResultLauncher<Intent> mapPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String addr = result.getData().getStringExtra("address");
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    if (currentMapContainer != null) {
                        AutoCompleteTextView auto = currentMapContainer.findViewById(R.id.autoAddress);
                        if (auto != null) {
                            auto.setText(addr, false);
                            auto.dismissDropDown();
                            auto.clearFocus();
                            hideKeyboard(auto);
                        }
                        setLatLngForCard(currentMapContainer, new LatLng(lat, lng));
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // ➜ Make the window resize and pad the scroll view when the IME shows
        getWindow().setSoftInputMode(SOFT_INPUT_ADJUST_RESIZE);
        setupImeAwareScroll();

        db = AppDatabase.getInstance(this);
        itineraryId = getIntent().getIntExtra("itinerary_id", -1);
        if (itineraryId <= 0) {
            Toast.makeText(this, "Invalid itinerary.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.topAppBar.setNavigationOnClickListener(v -> onBackPressed());
        binding.fabAddDestination.setOnClickListener(this);
        binding.btnSaveChanges.setOnClickListener(this);
        binding.btnExploreMap.setOnClickListener(v -> showOverlayMap(true));
        binding.overlayToolbar.setNavigationOnClickListener(v -> showOverlayMap(false));

        // Places init
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = ai.metaData.getString("com.google.android.geo.API_KEY", "");
            if (!Places.isInitialized() && apiKey != null && !apiKey.isEmpty()) {
                Places.initialize(getApplicationContext(), apiKey);
            }
        } catch (Exception ignored) {}
        placesClient = Places.createClient(this);

        // Preview map (static)
        SupportMapFragment prev = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapPreviewContainer);
        if (prev != null) {
            prev.getMapAsync(map -> {
                previewMap = map;
                previewMap.getUiSettings().setAllGesturesEnabled(false);
                previewMap.getUiSettings().setZoomControlsEnabled(false);
                previewMap.setOnMapLoadedCallback(() -> previewLoadedOnce = true);
            });
        }

        // Overlay map (interactive)
        SupportMapFragment overlay = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.overlayMapContainer);
        if (overlay != null) {
            overlay.getMapAsync(map -> {
                overlayMap = map;
                overlayMap.getUiSettings().setAllGesturesEnabled(true);
                overlayMap.getUiSettings().setZoomControlsEnabled(true);
                overlayMap.setOnMapLoadedCallback(() -> overlayLoadedOnce = true);
            });
        }

        loadItinerary();
    }

    // --- IME padding so fields don't hide behind the keyboard ---
    private void setupImeAwareScroll() {
        View root = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPad = imeVisible ? ime.bottom : dp(16);

            if (binding.scrollContainer.getPaddingBottom() != bottomPad) {
                binding.scrollContainer.setPadding(
                        binding.scrollContainer.getPaddingLeft(),
                        binding.scrollContainer.getPaddingTop(),
                        binding.scrollContainer.getPaddingRight(),
                        bottomPad
                );
            }
            return insets;
        });

        // initial padding (when IME not visible yet)
        binding.scrollContainer.post(() ->
                binding.scrollContainer.setPadding(
                        binding.scrollContainer.getPaddingLeft(),
                        binding.scrollContainer.getPaddingTop(),
                        binding.scrollContainer.getPaddingRight(),
                        dp(16)
                ));
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    @Override protected void onDestroy() { super.onDestroy(); io.shutdown(); }

    private void showOverlayMap(boolean show) {
        binding.overlayMapRoot.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.fabAddDestination.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.btnSaveChanges.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) updatePreviewMarkers();
    }

    // ---------- Load / Save ----------
    private void loadItinerary() {
        io.execute(() -> {
            final Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
            final List<Destination> dests = db.destinationDao().getDestinationsForItinerary(itineraryId);

            runOnUiThread(() -> {
                if (it == null) { finish(); return; }
                binding.editTripName.setText(it.tripName);

                destinationCount = 0;
                binding.destinationContainer.removeAllViews();
                cardLatLng.clear();
                lastPointsSignature = "";

                if (dests != null && !dests.isEmpty()) {
                    for (Destination d : dests) addCardPrefilled(d.address, d.note, d.date, d.time);
                    geocodeAllExistingAddresses();
                } else {
                    addCardPrefilled(null, null, null, null);
                    updatePreviewMarkers();
                }
            });
        });
    }

    private void saveChanges() {
        final String tripName = binding.editTripName.getText() != null
                ? binding.editTripName.getText().toString().trim() : "";
        if (tripName.isEmpty()) {
            Toast.makeText(this, getString(R.string.trip_name_required), Toast.LENGTH_SHORT).show();
            return;
        }

        io.execute(() -> {
            Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
            if (it == null) { runOnUiThread(this::finish); return; }
            it.tripName = tripName;
            db.itineraryDao().updateItinerary(it);

            // NEW: cancel previously scheduled reminders for this itinerary
            ReminderScheduler.cancelAllForItinerary(this, itineraryId);

            db.destinationDao().deleteByItineraryId(itineraryId);
            ReminderScheduler.cancelAllForItinerary(this, itineraryId);

            for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
                View card = binding.destinationContainer.getChildAt(i);

                AutoCompleteTextView auto = card.findViewById(R.id.autoAddress);
                String address = auto != null && auto.getText() != null ? auto.getText().toString().trim() : "";
                String note = textOf((TextInputEditText) card.findViewById(R.id.editNote));
                String date = textOf((MaterialButton) card.findViewById(R.id.btnSelectDate));
                String time = textOf((MaterialButton) card.findViewById(R.id.btnSelectTime));

                if (isEmpty(address) && isEmpty(note) && isEmpty(date) && isEmpty(time)) continue;

                Destination d = new Destination();
                d.itineraryId = itineraryId;
                d.address = address;
                d.note = note;
                d.date = date;
                d.time = time;

                long rowId = db.destinationDao().insertDestination(d); // capture id

                // NEW: schedule reminder 1 hour before

            }

            ReminderScheduler.scheduleForItinerary(this, itineraryId);

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.itinerary_saved, Toast.LENGTH_SHORT).show();
                // NEW
                com.example.tripsync_phone_app.notify.ReminderScheduler.scheduleForItinerary(this, itineraryId);
                loadItinerary(); // stay here
            });

        });
    }

    // ---------- Card creation & wiring ----------
    private void addCardPrefilled(@Nullable String address, @Nullable String note,
                                  @Nullable String date, @Nullable String time) {

        destinationCount++;
        View card = getLayoutInflater().inflate(R.layout.destination_card, binding.destinationContainer, false);

        TextView title = card.findViewById(R.id.txtTitle);
        if (title != null) title.setText(getString(R.string.destination_n, destinationCount));

        ImageButton btnClose = card.findViewById(R.id.btnClose);
        AutoCompleteTextView autoAddress = card.findViewById(R.id.autoAddress);
        TextInputEditText noteField = card.findViewById(R.id.editNote);
        MaterialButton dateBtn = card.findViewById(R.id.btnSelectDate);
        MaterialButton timeBtn = card.findViewById(R.id.btnSelectTime);
        MaterialButton btnUseMap = card.findViewById(R.id.btnUseMap);
        MaterialButton btnClear  = card.findViewById(R.id.btnClear);

        if (address != null) autoAddress.setText(address, false);
        if (note != null)    noteField.setText(note);
        if (date != null && !date.isEmpty()) dateBtn.setText(date);
        if (time != null && !time.isEmpty()) timeBtn.setText(time);

        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this, placesClient);
        autoAddress.setAdapter(adapter);
        autoAddress.setThreshold(1);

        final boolean[] suppress = { false };
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!suppress[0] && autoAddress.hasFocus() && !autoAddress.isPerformingCompletion() && s.length() >= 1) {
                    autoAddress.showDropDown();
                }
            }
            @Override public void afterTextChanged(Editable s) {
                if (s == null || s.toString().trim().isEmpty()) setLatLngForCard(card, null);
            }
        };
        autoAddress.addTextChangedListener(watcher);
        autoAddress.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) autoAddress.dismissDropDown(); });

        autoAddress.setOnItemClickListener((parent, view, position, id) -> {
            String placeId = adapter.getPrediction(position).getPlaceId();
            FetchPlaceRequest req = FetchPlaceRequest.newInstance(
                    placeId, Arrays.asList(Place.Field.LAT_LNG, Place.Field.ADDRESS)
            );
            placesClient.fetchPlace(req).addOnSuccessListener(resp -> {
                Place p = resp.getPlace();
                suppress[0] = true;
                autoAddress.setText(p.getAddress(), false);
                autoAddress.dismissDropDown();
                autoAddress.clearFocus();
                hideKeyboard(autoAddress);
                autoAddress.postDelayed(() -> suppress[0] = false, 150);
                if (p.getLatLng() != null) setLatLngForCard(card, p.getLatLng());
            });
        });

        dateBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date)).build();
            picker.addOnPositiveButtonClickListener(sel -> {
                dateBtn.setText(picker.getHeaderText());
                updatePreviewMarkers();
            });
            picker.show(getSupportFragmentManager(), "DATE_EDIT");
        });

        timeBtn.setOnClickListener(v -> {
            MaterialTimePicker tp = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(12).setMinute(0)
                    .setTitleText(getString(R.string.select_time))
                    .build();
            tp.addOnPositiveButtonClickListener(v1 -> {
                timeBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", tp.getHour(), tp.getMinute()));
                updatePreviewMarkers();
            });
            tp.show(getSupportFragmentManager(), "TIME_EDIT");
        });

        if (btnUseMap != null) {
            btnUseMap.setOnClickListener(v -> {
                currentMapContainer = card;
                mapPicker.launch(new Intent(this, MapPickerActivity.class));
            });
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                autoAddress.setText("", false);
                autoAddress.requestFocus();
                showKeyboard(autoAddress);
                setLatLngForCard(card, null);
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                autoAddress.removeTextChangedListener(watcher);
                binding.destinationContainer.removeView(card);
                destinationCount = Math.max(0, destinationCount - 1);
                renumberCards();
                cardLatLng.remove(card);
                updatePreviewMarkers();
            });
        }

        binding.destinationContainer.addView(card);
    }

    private void renumberCards() {
        for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
            View c = binding.destinationContainer.getChildAt(i);
            TextView t = c.findViewById(R.id.txtTitle);
            if (t != null) t.setText(getString(R.string.destination_n, i + 1));
        }
    }

    // ---------- Map rendering (debounced + deduped) ----------
    private void setLatLngForCard(View card, @Nullable LatLng latLng) {
        if (latLng == null) cardLatLng.remove(card);
        else cardLatLng.put(card, latLng);
        updatePreviewMarkers();
    }

    private void geocodeAllExistingAddresses() {
        io.execute(() -> {
            List<Runnable> applyList = new ArrayList<>();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
                    View card = binding.destinationContainer.getChildAt(i);
                    AutoCompleteTextView a = card.findViewById(R.id.autoAddress);
                    if (a == null || a.getText() == null) continue;
                    String addr = a.getText().toString().trim();
                    if (addr.isEmpty() || cardLatLng.containsKey(card)) continue;

                    List<Address> list = geocoder.getFromLocationName(addr, 1);
                    if (list != null && !list.isEmpty()) {
                        final LatLng ll = new LatLng(list.get(0).getLatitude(), list.get(0).getLongitude());
                        applyList.add(() -> setLatLngForCard(card, ll));
                    }
                }
            } catch (IOException ignored) {}

            runOnUiThread(() -> {
                for (Runnable r : applyList) r.run();
                updatePreviewMarkers();
            });
        });
    }

    private void hideMapCard() {
        if (binding.mapPreviewCard.getVisibility() != View.GONE) binding.mapPreviewCard.setVisibility(View.GONE);
        if (previewMap != null) previewMap.clear();
    }

    private void updatePreviewMarkers() {
        class Item { final LatLng ll; final long sort; Item(LatLng l, long s){ ll=l; sort=s; } }
        List<Item> items = new ArrayList<>();

        for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
            View card = binding.destinationContainer.getChildAt(i);
            LatLng ll = cardLatLng.get(card);
            if (ll == null) continue;
            String dateStr = textOf((MaterialButton) card.findViewById(R.id.btnSelectDate));
            String timeStr = textOf((MaterialButton) card.findViewById(R.id.btnSelectTime));
            long sortKey = computeEpoch(dateStr, timeStr);
            items.add(new Item(ll, sortKey));
        }

        if (items.isEmpty()) {
            hideMapCard();
            lastPointsSignature = "";
            return;
        }

        Collections.sort(items, Comparator.comparingLong(i -> i.sort));
        if (binding.mapPreviewCard.getVisibility() != View.VISIBLE) binding.mapPreviewCard.setVisibility(View.VISIBLE);

        StringBuilder sig = new StringBuilder();
        for (Item it : items) sig.append(String.format(Locale.US, "%.5f,%.5f;", it.ll.latitude, it.ll.longitude));
        String newSig = sig.toString();
        if (newSig.equals(lastPointsSignature)) return;
        lastPointsSignature = newSig;

        if (pendingRender != null) main.removeCallbacks(pendingRender);
        final ArrayList<LatLng> points = new ArrayList<>();
        for (Item it : items) points.add(it.ll);

        pendingRender = () -> renderMarkers(points);
        main.postDelayed(pendingRender, 250);
    }

    private void renderMarkers(List<LatLng> points) {
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        // Preview map
        if (previewMap != null) {
            previewMap.clear();
            char label = 'A';
            for (LatLng p : points) {
                previewMap.addMarker(new MarkerOptions().position(p).title(String.valueOf(label++)));
                bounds.include(p);
            }
            Runnable cam = () -> {
                try {
                    previewMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 80));
                } catch (IllegalStateException ignore) {
                    previewMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
                }
            };
            if (previewLoadedOnce) cam.run();
            else previewMap.setOnMapLoadedCallback(() -> { previewLoadedOnce = true; cam.run(); });
        }

        // Overlay map
        if (overlayMap != null) {
            overlayMap.clear();
            LatLngBounds.Builder bounds2 = new LatLngBounds.Builder();
            char label2 = 'A';
            for (LatLng p : points) {
                overlayMap.addMarker(new MarkerOptions().position(p).title(String.valueOf(label2++)));
                bounds2.include(p);
            }
            Runnable cam2 = () -> {
                try {
                    overlayMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds2.build(), 80));
                } catch (IllegalStateException ignore) {
                    overlayMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), 15f));
                }
            };
            if (overlayLoadedOnce) cam2.run();
            else overlayMap.setOnMapLoadedCallback(() -> { overlayLoadedOnce = true; cam2.run(); });
        }
    }

    // ---------- Utils ----------
    private long computeEpoch(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || dateStr.startsWith("Select")) return Long.MAX_VALUE;
        SimpleDateFormat df1 = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat df2 = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        java.util.Calendar cal = java.util.Calendar.getInstance();
        try {
            try { cal.setTime(df1.parse(dateStr)); }
            catch (ParseException ex) { cal.setTime(df2.parse(dateStr)); }
            int hour = 0, minute = 0;
            if (timeStr != null && !timeStr.trim().isEmpty() && !timeStr.startsWith("Select")) {
                try {
                    String[] parts = timeStr.split(":");
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                } catch (Exception ignored) {}
            }
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
            cal.set(java.util.Calendar.MINUTE, minute);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.fabAddDestination.getId()) {
            addCardPrefilled(null, null, null, null);
            updatePreviewMarkers();
        } else if (v.getId() == binding.btnSaveChanges.getId()) {
            saveChanges();
        }
    }

    private static String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
    private static String textOf(MaterialButton b) {
        CharSequence t = b != null ? b.getText() : null; return t != null ? t.toString().trim() : "";
    }
    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) v.post(() -> { v.requestFocus(); imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT); });
    }
}
