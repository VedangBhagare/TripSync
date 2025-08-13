package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.adapter.ItineraryAdapter;
import com.example.tripsync_phone_app.auth.SessionManager;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.DestinationDao;
import com.example.tripsync_phone_app.database.Itinerary;
import com.example.tripsync_phone_app.database.ItineraryDao;
import com.example.tripsync_phone_app.databinding.ActivityItinerariesBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

        // Bottom nav
        BottomNavigationView bottomNav = binding.bottomNav;
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            if (id == R.id.nav_itineraries) return true;
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        binding.recyclerItineraries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItineraryAdapter(items, new ItineraryAdapter.OnItineraryClick() {
            @Override public void onView(Itinerary it) {
                Intent i = new Intent(ItinerariesActivity.this, ViewItineraryActivity.class);
                i.putExtra("itinerary_id", it.id);
                startActivity(i);
            }
            @Override public void onEdit(Itinerary it) {
                Intent i = new Intent(ItinerariesActivity.this, EditItineraryActivity.class);
                i.putExtra("itinerary_id", it.id);
                startActivity(i);
            }
            @Override public void onDelete(Itinerary it) {
                destinationDao.deleteByItineraryId(it.id);
                itineraryDao.deleteItinerary(it);
                loadData();
            }
        });
        binding.recyclerItineraries.setAdapter(adapter);
    }

    @Override protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        items.clear();
        items.addAll(itineraryDao.getItinerariesByUser(userId));
        adapter.notifyDataSetChanged();
    }
}
