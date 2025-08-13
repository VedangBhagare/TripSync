// tripsync_wear_app/src/main/java/com/example/tripsync_wear_app/util/JsonParser.java
package com.example.tripsync_wear_app.util;

import com.example.tripsync_wear_app.model.DestinationModel;
import com.example.tripsync_wear_app.model.ItineraryModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonParser {

    /** Parse the JSON sent from the phone into our Wear models. */
    public static List<ItineraryModel> parseItineraries(String json) {
        List<ItineraryModel> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject it = arr.getJSONObject(i);

                ItineraryModel itinerary = new ItineraryModel();
                itinerary.id = it.optInt("id", i + 1);
                itinerary.tripName = it.optString("tripName", "Trip");

                JSONArray dests = it.optJSONArray("destinations");
                if (dests != null) {
                    for (int d = 0; d < dests.length(); d++) {
                        JSONObject dj = dests.getJSONObject(d);

                        DestinationModel dest = new DestinationModel();
                        dest.address = dj.optString("address", "");
                        dest.note    = dj.optString("note", "");
                        dest.date    = dj.optString("date", "");  // keep same format as phone
                        dest.time    = dj.optString("time", "");

                        itinerary.destinations.add(dest);
                    }
                }

                out.add(itinerary);
            }
        } catch (Exception ignore) {
            // Return whatever we parsed so far; callers handle empty lists.
        }
        return out;
    }
}
