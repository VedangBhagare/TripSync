package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tripsync_phone_app.adapter.ItineraryAdapter;
import com.example.tripsync_phone_app.auth.SessionManager;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.DestinationDao;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.database.ItineraryDao;
import com.example.tripsync_phone_app.databinding.ActivityItinerariesBinding;

import java.util.ArrayList;
import java.util.List;

public class ItinerariesActivity extends AppCompatActivity {

    private ActivityItinerariesBinding binding;
    private AppDatabase db;
    private ItineraryDao itineraryDao;
    private DestinationDao destinationDao;
    private int userId;
    private final List<Itinerary> items = new ArrayList<>();
    private ItineraryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityItinerariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        db = AppDatabase.getInstance(this);
        itineraryDao = db.itineraryDao();
        destinationDao = db.destinationDao();

        userId = new SessionManager(this).getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.recyclerItineraries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItineraryAdapter(items,
                it -> { // onEdit
                    Intent i = new Intent(this, EditItineraryActivity.class);
                    i.putExtra("itinerary_id", it.id);
                    startActivity(i);
                },
                it -> { // onDelete
                    // delete children first (if no FK cascade)
                    destinationDao.deleteByItineraryId(it.id);
                    itineraryDao.deleteItinerary(it);
                    loadData();
                });
        binding.recyclerItineraries.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        items.clear();
        items.addAll(itineraryDao.getItinerariesByUser(userId));
        adapter.notifyDataSetChanged();
    }
}
