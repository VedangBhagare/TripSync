package com.example.tripsync_phone_app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface DestinationDao {

    @Insert
    long insertDestination(Destination destination);

    @Query("SELECT * FROM destinations WHERE itinerary_id = :itineraryId")
    List<Destination> getDestinationsForItinerary(int itineraryId);

    @Query("DELETE FROM destinations WHERE itinerary_id = :itineraryId")
    int deleteByItineraryId(int itineraryId);

    @Delete
    int deleteDestination(Destination destination);
}
