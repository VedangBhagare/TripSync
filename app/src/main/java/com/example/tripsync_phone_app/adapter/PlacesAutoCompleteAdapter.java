package com.example.tripsync_phone_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.tripsync_phone_app.R;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> {

    private final PlacesClient placesClient;
    private final AutocompleteSessionToken sessionToken;
    private final LayoutInflater inflater;

    private final List<AutocompletePrediction> data = new ArrayList<>();
    private RectangularBounds locationBias; // optional
    private String country;                 // optional (e.g., "CA", "US")

    public PlacesAutoCompleteAdapter(@NonNull Context ctx, @NonNull PlacesClient client) {
        super(ctx, 0);
        this.placesClient = client;
        this.sessionToken = AutocompleteSessionToken.newInstance();
        this.inflater = LayoutInflater.from(ctx);
    }

    /** Optional: bias predictions to current visible region/city bounds */
    public void setLocationBias(@Nullable LatLngBounds bounds) {
        this.locationBias = (bounds == null) ? null : RectangularBounds.newInstance(bounds);
    }

    /** Optional: restrict results to a single country ("CA", "US", …) */
    public void setCountry(@Nullable String countryCode) {
        this.country = countryCode;
    }

    public AutocompletePrediction getPrediction(int position) { return data.get(position); }

    @Override public int getCount() { return data.size(); }
    @Nullable @Override public AutocompletePrediction getItem(int position) { return data.get(position); }

    @NonNull @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = (convertView != null) ? convertView
                : inflater.inflate(R.layout.item_place_prediction, parent, false);
        AutocompletePrediction p = data.get(position);
        ((TextView) v.findViewById(R.id.primary)).setText(p.getPrimaryText(null));
        ((TextView) v.findViewById(R.id.secondary)).setText(p.getSecondaryText(null));
        return v;
    }

    @NonNull @Override
    public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults fr = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    fr.values = new ArrayList<>();
                    fr.count = 0;
                    return fr;
                }

                FindAutocompletePredictionsRequest.Builder b =
                        FindAutocompletePredictionsRequest.builder()
                                .setQuery(constraint.toString())
                                .setSessionToken(sessionToken);

                if (locationBias != null) b.setLocationBias(locationBias);
                if (country != null && !country.isEmpty()) {
                    b.setCountries(Collections.singletonList(country));
                }

                try {
                    FindAutocompletePredictionsResponse resp = Tasks.await(
                            placesClient.findAutocompletePredictions(b.build()),
                            3, TimeUnit.SECONDS
                    );
                    List<AutocompletePrediction> list = resp.getAutocompletePredictions();
                    fr.values = (list == null) ? new ArrayList<>() : list;
                    fr.count = (list == null) ? 0 : list.size();
                } catch (Exception e) {
                    fr.values = new ArrayList<>();
                    fr.count = 0;
                }
                return fr;
            }

            @Override @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                data.clear();
                if (results != null && results.values != null) {
                    data.addAll((List<AutocompletePrediction>) results.values);
                }
                notifyDataSetChanged();
            }

            @Override public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof AutocompletePrediction) {
                    return ((AutocompletePrediction) resultValue).getFullText(null);
                }
                return super.convertResultToString(resultValue);
            }
        };
    }
}
