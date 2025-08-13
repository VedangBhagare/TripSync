package com.example.tripsync_phone_app.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItineraryDao {
    @Insert
    long insertItinerary(Itinerary itinerary);

    @Update
    int updateItinerary(Itinerary itinerary);

    @Delete
    int deleteItinerary(Itinerary itinerary);

    @Query("SELECT * FROM itineraries WHERE user_id = :userId ORDER BY id DESC")
    List<Itinerary> getItinerariesByUser(int userId);

    @Query("SELECT * FROM itineraries WHERE id = :id LIMIT 1")
    Itinerary getItineraryById(int id);

    default List<Itinerary> getItinerariesForUser(int userId) {
        return getItinerariesByUser(userId);
    }
}
