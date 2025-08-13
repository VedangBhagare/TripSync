package com.example.tripsync_wear_app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.tripsync_wear_app.model.DestinationModel;
import com.example.tripsync_wear_app.model.ItineraryModel;
import com.example.tripsync_wear_app.util.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight local store for user-created trips on Wear only.
 * Persists a JSON array in SharedPreferences and can convert to models.
 */
public final class WearUserItineraries {
    private static final String SP = "TripSyncWearUser";
    private static final String KEY = "user_trips_json";

    private WearUserItineraries() {}

    public static void add(Context ctx, ItineraryModel it) {
        try {
            JSONArray arr = new JSONArray(getRaw(ctx));
            arr.put(toJson(it));
            ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                    .edit().putString(KEY, arr.toString()).apply();
        } catch (JSONException e) {
            // Start fresh if parsing fails
            JSONArray arr = new JSONArray();
            arr.put(toJson(it));
            ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                    .edit().putString(KEY, arr.toString()).apply();
        }
    }

    public static List<ItineraryModel> getAll(Context ctx) {
        String raw = getRaw(ctx);
        if (raw.isEmpty() || raw.equals("[]")) return new ArrayList<>();
        return JsonParser.parseItineraries(raw);
    }

    private static String getRaw(Context ctx) {
        return ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
    }

    // --- helpers ----
    private static JSONObject toJson(ItineraryModel it) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", it.id);
            o.put("tripName", it.tripName);

            JSONArray dests = new JSONArray();
            for (DestinationModel d : it.destinations) {
                JSONObject dj = new JSONObject();
                dj.put("address", d.address);
                dj.put("note", d.note);
                dj.put("date", d.date);
                dj.put("time", d.time);
                dests.put(dj);
            }
            o.put("destinations", dests);
            return o;
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
