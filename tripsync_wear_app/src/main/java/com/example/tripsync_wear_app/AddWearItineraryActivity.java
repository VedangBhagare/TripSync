// tripsync_wear_app/src/main/java/com/example/tripsync_wear_app/AddWearItineraryActivity.java
package com.example.tripsync_wear_app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;

import com.example.tripsync_wear_app.databinding.ActivityAddWearItineraryBinding;
import com.example.tripsync_wear_app.model.DestinationModel;
import com.example.tripsync_wear_app.model.ItineraryModel;
import com.example.tripsync_wear_app.notify.NotificationHelper;
import com.example.tripsync_wear_app.util.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddWearItineraryActivity extends ComponentActivity implements View.OnClickListener {

    private ActivityAddWearItineraryBinding binding;

    private static final int RC_SPEECH_TRIP = 201;
    private static final int RC_SPEECH_NOTE = 202;

    private final Calendar cal = Calendar.getInstance();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddWearItineraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnPickDate.setOnClickListener(this);
        binding.btnPickTime.setOnClickListener(this);
        binding.btnVoiceTrip.setOnClickListener(this);
        binding.btnVoiceNote.setOnClickListener(this);
        binding.btnSave.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(v -> finish());

        // Initialize date/time display
        updateDateButton();
        updateTimeButton();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == binding.btnPickDate.getId()) {
            DatePickerDialog dp = new DatePickerDialog(
                    this, (DatePicker view, int y, int m, int d) -> {
                cal.set(Calendar.YEAR, y);
                cal.set(Calendar.MONTH, m);
                cal.set(Calendar.DAY_OF_MONTH, d);
                updateDateButton();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dp.show();

        } else if (id == binding.btnPickTime.getId()) {
            TimePickerDialog tp = new TimePickerDialog(
                    this, (TimePicker view, int h, int min) -> {
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                updateTimeButton();
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            tp.show();

        } else if (id == binding.btnVoiceTrip.getId()) {
            startVoice(RC_SPEECH_TRIP);

        } else if (id == binding.btnVoiceNote.getId()) {
            startVoice(RC_SPEECH_NOTE);

        } else if (id == binding.btnSave.getId()) {
            onSave();
        }
    }

    private void startVoice(int req) {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        try { startActivityForResult(i, req); }
        catch (Exception e) { Toast.makeText(this, "Voice input unavailable", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (res == null || res.isEmpty()) return;

        String text = res.get(0);
        if (requestCode == RC_SPEECH_TRIP) binding.editTripName.setText(text);
        else if (requestCode == RC_SPEECH_NOTE) binding.editNote.setText(text);
    }

    private void updateDateButton() {
        SimpleDateFormat df = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        binding.btnPickDate.setText(df.format(cal.getTime()));
    }

    private void updateTimeButton() {
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        binding.btnPickTime.setText(tf.format(cal.getTime()));
    }

    private void onSave() {
        String trip = safe(binding.editTripName.getText());
        String note = safe(binding.editNote.getText());
        String dateStr = binding.btnPickDate.getText().toString();
        String timeStr = binding.btnPickTime.getText().toString();

        if (trip.isEmpty()) {
            Toast.makeText(this, "Trip name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build a single Wear-only itinerary (id from time so it’s unique)
        ItineraryModel it = new ItineraryModel();
        it.id = (int) (System.currentTimeMillis() / 1000L) * -1;
        it.tripName = trip;

        DestinationModel d = new DestinationModel();
        d.address = "Trip reminder";   // no address in Wear quick add
        d.note = note;
        d.date = dateStr; // must be "MMM d, yyyy"
        d.time = timeStr; // must be "HH:mm"
        it.destinations = new java.util.ArrayList<>();
        it.destinations.add(d);

        // 1) Persist to SharedPreferences (append to a JSON array)
        SharedPreferences sp = getSharedPreferences("TripSyncWear", MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(sp.getString("manual_itins", "[]"));

            JSONObject itJson = new JSONObject();
            itJson.put("id", it.id);
            itJson.put("tripName", it.tripName);

            JSONArray dests = new JSONArray();
            JSONObject dj = new JSONObject();
            dj.put("address", d.address);
            dj.put("note", d.note);
            dj.put("date", d.date);
            dj.put("time", d.time);
            dests.put(dj);

            itJson.put("destinations", dests);
            arr.put(itJson);

            sp.edit().putString("manual_itins", arr.toString()).apply();

            // 2) Also refresh the cached “last_json” shown in ItinerariesActivity by merging
            //    manual_itins + whatever the phone last sent.
            List<com.example.tripsync_wear_app.model.ItineraryModel> merged = new ArrayList<>();

            String cached = sp.getString("last_json", null);
            if (cached != null) merged.addAll(JsonParser.parseItineraries(cached));
            merged.addAll(JsonParser.parseItineraries(arr.toString()));

            // Re-save the merged result back into last_json so the list shows it
            // (purely local—doesn’t affect phone sync)
            JSONArray mergedJson = new JSONArray();
            for (ItineraryModel mi : merged) {
                JSONObject mj = new JSONObject();
                mj.put("id", mi.id);
                mj.put("tripName", mi.tripName);
                JSONArray mjDests = new JSONArray();
                for (DestinationModel md : mi.destinations) {
                    JSONObject mdj = new JSONObject();
                    mdj.put("address", md.address);
                    mdj.put("note", md.note);
                    mdj.put("date", md.date);
                    mdj.put("time", md.time);
                    mjDests.put(mdj);
                }
                mj.put("destinations", mjDests);
                mergedJson.put(mj);
            }
            sp.edit().putString("last_json", mergedJson.toString()).apply();

        } catch (Exception ignore) { /* harmless: we’ll still schedule below */ }

        // 3) Schedule the reminder (reuse your existing helper)
        List<ItineraryModel> single = new ArrayList<>();
        single.add(it);
        com.example.tripsync_wear_app.data.WearUserItineraries.add(this, it);
        com.example.tripsync_wear_app.notify.NotificationHelper.scheduleAll(this,
                java.util.Collections.singletonList(it));

        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
