package com.example.tripsync_wear_app.model;

import java.util.ArrayList;
import java.util.List;

public class ItineraryModel {
    public int id;
    public String tripName;
    public List<DestinationModel> destinations = new ArrayList<>();
}
