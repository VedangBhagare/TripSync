package com.example.tripsync_wear_app.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.tripsync_wear_app.model.ItineraryModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ItineraryStore {
    private ItineraryStore() {}

    private static final MutableLiveData<List<ItineraryModel>> LIVE =
            new MutableLiveData<>(new ArrayList<>());

    public static LiveData<List<ItineraryModel>> itineraries() {
        return LIVE;
    }

    public static void post(List<ItineraryModel> src) {
        LIVE.postValue(src == null ? Collections.emptyList() : new ArrayList<>(src));
    }
}
