package com.example.tripsync_phone_app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.databinding.ActivityEditItineraryBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.List;
import java.util.Locale;

public class EditItineraryActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityEditItineraryBinding binding;
    private AppDatabase db;
    private int itineraryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        loadItinerary();
    }

    private void loadItinerary() {
        Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
        if (it == null) { finish(); return; }
        binding.editTripName.setText(it.tripName);

        List<Destination> dests = db.destinationDao().getDestinationsForItinerary(itineraryId);
        for (Destination d : dests) {
            addCardPrefilled(d.address, d.note, d.date, d.time);
        }
    }

    private void addCardPrefilled(String address, String note, String date, String time) {
        View card = getLayoutInflater().inflate(R.layout.destination_card, binding.destinationContainer, false);

        AutoCompleteTextView autoAddress = card.findViewById(R.id.autoAddress);
        TextInputEditText noteField = card.findViewById(R.id.editNote);
        MaterialButton dateBtn = card.findViewById(R.id.btnSelectDate);
        MaterialButton timeBtn = card.findViewById(R.id.btnSelectTime);

        if (address != null) autoAddress.setText(address);
        if (note != null) noteField.setText(note);
        if (date != null && !date.isEmpty()) dateBtn.setText(date);
        if (time != null && !time.isEmpty()) timeBtn.setText(time);

        dateBtn.setOnClickListener(v -> showDatePicker(dateBtn));
        timeBtn.setOnClickListener(v -> showTimePicker(timeBtn));

        binding.destinationContainer.addView(card);
    }

    private void showDatePicker(MaterialButton targetBtn) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .build();
        picker.addOnPositiveButtonClickListener(sel -> targetBtn.setText(picker.getHeaderText()));
        picker.show(getSupportFragmentManager(), "DATE_EDIT");
    }

    private void showTimePicker(MaterialButton targetBtn) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12).setMinute(0)
                .setTitleText(getString(R.string.select_time))
                .build();
        picker.addOnPositiveButtonClickListener(v ->
                targetBtn.setText(String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute())));
        picker.show(getSupportFragmentManager(), "TIME_EDIT");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.fabAddDestination.getId()) {
            addCardPrefilled(null, null, null, null);
        } else if (v.getId() == binding.btnSaveChanges.getId()) {
            saveChanges();
        }
    }

    private void saveChanges() {
        String tripName = binding.editTripName.getText() != null ? binding.editTripName.getText().toString().trim() : "";
        if (tripName.isEmpty()) {
            Toast.makeText(this, getString(R.string.trip_name_required), Toast.LENGTH_SHORT).show();
            return;
        }

        Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
        if (it == null) { finish(); return; }
        it.tripName = tripName;
        db.itineraryDao().updateItinerary(it);

        // Replace destinations
        db.destinationDao().deleteByItineraryId(itineraryId);

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
            db.destinationDao().insertDestination(d);
        }

        Toast.makeText(this, getString(R.string.itinerary_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String textOf(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
    private static String textOf(MaterialButton b) {
        CharSequence t = b != null ? b.getText() : null; return t != null ? t.toString().trim() : "";
    }
    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }
}
