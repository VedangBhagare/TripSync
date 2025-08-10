package com.example.tripsync_phone_app.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class PlacesAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private final PlacesClient placesClient;
    private final List<AutocompletePrediction> predictions = new ArrayList<>();
    private AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

    public PlacesAutoCompleteAdapter(@NonNull Context ctx, PlacesClient client) {
        super(ctx, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        this.placesClient = client;
    }

    public AutocompletePrediction getPrediction(int position) {
        return predictions.get(position);
    }

    @Override public int getCount() { return predictions.size(); }

    @Override public String getItem(int position) {
        return predictions.get(position).getFullText(null).toString();
    }

    @Override public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults res = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    predictions.clear();
                    res.values = predictions;
                    res.count = 0;
                    return res;
                }

                FindAutocompletePredictionsRequest req = FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(token)
                        .setQuery(constraint.toString())
                        .build();

                try {
                    predictions.clear();
                    predictions.addAll(placesClient.findAutocompletePredictions(req).getResult().getAutocompletePredictions());
                } catch (Exception ignored) {}

                res.values = predictions;
                res.count = predictions.size();
                return res;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };
    }
}
