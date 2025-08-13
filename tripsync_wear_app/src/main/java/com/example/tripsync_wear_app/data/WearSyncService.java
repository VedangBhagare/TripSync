package com.example.tripsync_wear_app.data;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tripsync_wear_app.model.ItineraryModel;
import com.example.tripsync_wear_app.notify.NotificationHelper;
import com.example.tripsync_wear_app.util.JsonParser;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class WearSyncService extends WearableListenerService {
    private static final String TAG = "WearSyncService";
    private static final String PATH_PUSH_JSON = "/tripsync/itineraries_json";

    @Override public void onCreate() {
                super.onCreate();
                // If we have something cached, push it to the UI right away.
                        String cached = getSharedPreferences("TripSyncWear", MODE_PRIVATE)
                                .getString("last_json", null);
                if (cached != null) {
                        ItineraryStore.post(JsonParser.parseItineraries(cached));
                    }
            }

    @Override public void onMessageReceived(@NonNull MessageEvent event) {
        final String path = event.getPath();
        Log.d(TAG, "onMessageReceived path=" + path);
        // inside onMessageReceived, right after String json = ...
        Log.d(TAG, "received /tripsync/itineraries_json, bytes=" + event.getData().length);


        if (!PATH_PUSH_JSON.equals(path)) {
            super.onMessageReceived(event);
            return;
        }

        try {
            String json = new String(event.getData(), StandardCharsets.UTF_8);
            Log.d(TAG, "json bytes=" + event.getData().length);
            getSharedPreferences("TripSyncWear", MODE_PRIVATE)
                    .edit().putString("last_json", json).apply();

            // Parse → publish to UI → schedule reminders (if enabled)
            List<ItineraryModel> list = JsonParser.parseItineraries(json);
            ItineraryStore.post(list);

            SharedPreferences sp = getSharedPreferences("TripSyncWear", MODE_PRIVATE);
            boolean enabled = sp.getBoolean("notify_enabled", true);
            if (enabled) {
                NotificationHelper.scheduleAll(this, list);
            } else {
                Log.d(TAG, "reminders disabled by pref");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle itineraries JSON", e);
        }
    }
}
