package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.auth.SessionManager;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ActivityAddItineraryBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AddItineraryActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int RC_PICK_ON_MAP = 100;

    private ActivityAddItineraryBinding binding;
    private AppDatabase db;
    private SessionManager session;
    private int userId;
    private final List<Destination> destinations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);
        session = new SessionManager(this);

        // 1) read from session
        userId = session.getUserId();
        // 2) optional fallback from Intent (if you passed it from Home)
        int fromIntent = getIntent().getIntExtra("user_id", -1);
        if (fromIntent > 0) userId = fromIntent;

        if (userId <= 0) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding.topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        binding.fabAddDestination.setOnClickListener(this);
        binding.btnConfirmSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.fabAddDestination.getId()) {
            Intent i = new Intent(this, MapPickerActivity.class);
            startActivityForResult(i, RC_PICK_ON_MAP);
        } else if (id == binding.btnConfirmSave.getId()) {
            saveItinerary();
        }
    }

    // Blank card (if you ever need it)
    private void addDestinationCard() {
        addDestinationCardPrefilled(null);
    }

    // Prefilled card (address from map picker)
    private void addDestinationCardPrefilled(@Nullable String address) {
        View cardView = getLayoutInflater().inflate(R.layout.destination_card, binding.destinationContainer, false);

        TextInputEditText addressField = cardView.findViewById(R.id.editAddress);
        TextInputEditText noteField = cardView.findViewById(R.id.editNote);
        MaterialButton dateBtn = cardView.findViewById(R.id.btnSelectDate);
        MaterialButton timeBtn = cardView.findViewById(R.id.btnSelectTime);

        if (address != null) addressField.setText(address);

        dateBtn.setOnClickListener(v -> showDatePicker(dateBtn));
        timeBtn.setOnClickListener(v -> showTimePicker(timeBtn));

        binding.destinationContainer.addView(cardView);
    }

    private void showDatePicker(MaterialButton targetBtn) {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .build();
        datePicker.addOnPositiveButtonClickListener(selection ->
                targetBtn.setText(datePicker.getHeaderText()));
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showTimePicker(MaterialButton targetBtn) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText(getString(R.string.select_time))
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d",
                    timePicker.getHour(), timePicker.getMinute());
            targetBtn.setText(time);
        });
        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void saveItinerary() {
        String tripName = binding.editTripName.getText() != null
                ? binding.editTripName.getText().toString().trim() : "";

        if (tripName.isEmpty()) {
            Toast.makeText(this, getString(R.string.trip_name_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (userId <= 0) {
            Toast.makeText(this, "Invalid user. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Insert parent itinerary
        Itinerary itinerary = new Itinerary();
        itinerary.tripName = tripName;
        itinerary.userId = userId;
        long itineraryId = db.itineraryDao().insertItinerary(itinerary);

        // Insert each destination card
        for (int i = 0; i < binding.destinationContainer.getChildCount(); i++) {
            View card = binding.destinationContainer.getChildAt(i);

            String address = textOf((TextInputEditText) card.findViewById(R.id.editAddress));
            String note = textOf((TextInputEditText) card.findViewById(R.id.editNote));
            String date = textOf((MaterialButton) card.findViewById(R.id.btnSelectDate));
            String time = textOf((MaterialButton) card.findViewById(R.id.btnSelectTime));

            // Skip truly empty rows
            if (isEmpty(address) && isEmpty(note) && isEmpty(date) && isEmpty(time)) continue;

            Destination dest = new Destination();
            dest.itineraryId = (int) itineraryId;
            dest.address = address;
            dest.note = note;
            dest.date = date;
            dest.time = time;

            db.destinationDao().insertDestination(dest);
        }

        Toast.makeText(this, getString(R.string.itinerary_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String textOf(MaterialButton b) {
        CharSequence t = b != null ? b.getText() : null;
        return t != null ? t.toString().trim() : "";
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_ON_MAP && resultCode == RESULT_OK && data != null) {
            String address = data.getStringExtra("address");
            addDestinationCardPrefilled(address);
        }
    }
}
