package com.example.tripsync_phone_app.wear;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhoneSyncService extends WearableListenerService {
    private static final String TAG = "PhoneSyncService";

    // message paths (must match wear module)
    private static final String PATH_PING       = "/tripsync/ping";
    private static final String PATH_PONG       = "/tripsync/pong";
    private static final String PATH_PULL       = "/tripsync/pull";
    private static final String PATH_PUSH_JSON  = "/tripsync/itineraries_json";
    private static final String PATH_PUSH_VOICE = "/tripsync/voice_add";

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override public void onCreate() {
        super.onCreate();
        Log.i(TAG, "PhoneSyncService created");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent event) {
        final String path = event.getPath();
        final String nodeId = event.getSourceNodeId();
        Log.d(TAG, "onMessageReceived path=" + path + " from=" + nodeId);

        switch (path) {
            case PATH_PING:
                send(nodeId, PATH_PONG, new byte[0]);
                break;

            case PATH_PULL:
                io.execute(() -> {
                    try {
                        String json = buildItinerariesJson();
                        send(nodeId, PATH_PUSH_JSON, json.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        Log.e(TAG, "pull/build json failed", e);
                        send(nodeId, PATH_PUSH_JSON, "[]".getBytes(StandardCharsets.UTF_8));
                    }
                });
                break;

            case PATH_PUSH_VOICE:
                io.execute(() -> {
                    try {
                        String phrase = new String(event.getData(), StandardCharsets.UTF_8);
                        Log.d(TAG, "VOICE ADD: " + phrase);
                        // TODO: parse phrase & insert a quick itinerary if you want
                        // after insert, re-send the fresh list:
                        String json = buildItinerariesJson();
                        send(nodeId, PATH_PUSH_JSON, json.getBytes(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        Log.e(TAG, "voice handler failed", e);
                    }
                });
                break;

            default:
                super.onMessageReceived(event);
        }
    }

    private void send(String nodeId, String path, byte[] data) {
        MessageClient mc = Wearable.getMessageClient(this);
        mc.sendMessage(nodeId, path, data)
                .addOnSuccessListener(id -> Log.d(TAG, "sent " + path + " (" + data.length + "B)"))
                .addOnFailureListener(e -> Log.e(TAG, "send " + path + " failed", e));
    }

    /**
     * Builds JSON like:
     * [
     *   { "id":1, "tripName":"Paris", "destinations":[
     *       {"address":"Louvre","note":"tickets","date":"Aug 20, 2025","time":"14:30"}, ...
     *   ]},
     *   ...
     * ]
     */
    private String buildItinerariesJson() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());

            // read current user_id saved by your phone app
            SharedPreferences sp = getSharedPreferences("TripSyncPrefs", MODE_PRIVATE);
            int userId = sp.getInt("user_id", -1);

            List<Itinerary> its = db.itineraryDao().getItinerariesByUser(userId);

            // fallback sample so the watch UI isn’t empty during first run
            if (its == null || its.isEmpty()) {
                JSONArray demo = new JSONArray();
                JSONObject it = new JSONObject();
                it.put("id", 1);
                it.put("tripName", "Sample Trip");
                JSONArray dests = new JSONArray();
                JSONObject d = new JSONObject();
                d.put("address", "Central Park");
                d.put("note", "Walk + photos");
                d.put("date", "Aug 20, 2025");
                d.put("time", "14:30");
                dests.put(d);
                it.put("destinations", dests);
                demo.put(it);
                Log.d(TAG, "Sending demo JSON (no itineraries for userId=" + userId + ")");
                return demo.toString();
            }

            JSONArray arr = new JSONArray();
            for (Itinerary it : its) {
                JSONObject jo = new JSONObject();
                jo.put("id", it.id);
                jo.put("tripName", it.tripName);

                JSONArray dests = new JSONArray();
                List<Destination> list = db.destinationDao().getDestinationsForItinerary(it.id);
                for (Destination d : list) {
                    JSONObject dj = new JSONObject();
                    dj.put("address", d.address);
                    dj.put("note", d.note);
                    dj.put("date", d.date); // keep formats exactly as phone saves them
                    dj.put("time", d.time);
                    dests.put(dj);
                }
                jo.put("destinations", dests);
                arr.put(jo);
            }
            return arr.toString();

        } catch (Exception e) {
            Log.e(TAG, "buildItinerariesJson failed", e);
            return "[]";
        }
    }
}
