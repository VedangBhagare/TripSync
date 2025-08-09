package com.example.tripsync_phone_app.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "destinations")
public class Destination {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "itinerary_id")   // <— canonical column name
    public int itineraryId;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "note")
    public String note;

    @ColumnInfo(name = "date")
    public String date;

    @ColumnInfo(name = "time")
    public String time;
}
