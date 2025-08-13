// tripsync_wear_app/src/main/java/com/example/tripsync_wear_app/ItinerariesActivity.java
package com.example.tripsync_wear_app;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tripsync_wear_app.data.DataLayerHelper;
import com.example.tripsync_wear_app.data.ItineraryStore;
import com.example.tripsync_wear_app.databinding.ActivityItinerariesBinding;
import com.example.tripsync_wear_app.model.ItineraryModel;
import com.example.tripsync_wear_app.notify.NotificationHelper;
import com.example.tripsync_wear_app.ui.ItineraryAdapter;

import java.util.ArrayList;
import java.util.List;

public class ItinerariesActivity extends ComponentActivity {

    private ActivityItinerariesBinding binding;
    private ItineraryAdapter adapter;

    private static final long AUTO_PULL_MS = 15_000;
    private final Runnable autoPull = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed()) return;
            DataLayerHelper.pull(ItinerariesActivity.this);
            binding.getRoot().postDelayed(this, AUTO_PULL_MS);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItinerariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ItineraryAdapter();
        binding.rvItineraries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvItineraries.setAdapter(adapter);

        final SharedPreferences sp = getSharedPreferences("TripSyncWear", MODE_PRIVATE);
        binding.ckNotifications.setChecked(sp.getBoolean("notify_enabled", true));
        binding.ckNotifications.setOnCheckedChangeListener((b, checked) ->
                sp.edit().putBoolean("notify_enabled", checked).apply());

        // Observe phone data; we’ll merge wear-local in onResume as well.
        ItineraryStore.itineraries().observe(this, (List<ItineraryModel> phoneList) -> {
            List<ItineraryModel> merged = new ArrayList<>();
            if (phoneList != null) merged.addAll(phoneList);
            merged.addAll(com.example.tripsync_wear_app.data.WearUserItineraries.getAll(this));
            adapter.submit(merged);

            if (sp.getBoolean("notify_enabled", true)) {
                NotificationHelper.scheduleAll(this, merged);
            }
        });

        NotificationHelper.ensureChannel(this);

        // Show cached immediately so the screen isn’t empty
        String cached = sp.getString("last_json", null);
        if (cached != null) {
            adapter.submit(com.example.tripsync_wear_app.util.JsonParser.parseItineraries(cached));
        }

        // Give the node a moment, then pull
        binding.getRoot().postDelayed(() -> DataLayerHelper.pull(this), 600);
    }

    @Override protected void onResume() {
        super.onResume();

        // Merge latest phone list (from LiveData) with wear-local items
        List<ItineraryModel> merged = new ArrayList<>();
        List<ItineraryModel> phoneNow = ItineraryStore.itineraries().getValue(); // <-- instead of current()
        if (phoneNow != null) merged.addAll(phoneNow);
        merged.addAll(com.example.tripsync_wear_app.data.WearUserItineraries.getAll(this));
        adapter.submit(merged);

        SharedPreferences sp = getSharedPreferences("TripSyncWear", MODE_PRIVATE);
        if (sp.getBoolean("notify_enabled", true)) {
            NotificationHelper.scheduleAll(this, merged);
        }

        // keep the auto-pull loop running while visible
        binding.getRoot().removeCallbacks(autoPull);
        binding.getRoot().postDelayed(autoPull, AUTO_PULL_MS);
    }

    @Override protected void onPause() {
        super.onPause();
        binding.getRoot().removeCallbacks(autoPull);
    }
}
