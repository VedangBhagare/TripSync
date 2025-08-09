package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.auth.SessionManager;
import com.example.tripsync_phone_app.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String username = getSharedPreferences("TripSyncPrefs", MODE_PRIVATE)
                .getString("username", "User");
        binding.welcomeText.setText("Welcome, " + username);

        binding.fabAdd.setOnClickListener(this);

        // NOTE: id is bottomNav (not bottomNavigation)
        binding.bottomNav.setOnItemSelectedListener(this::onBottomItemSelected);
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    private boolean onBottomItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            return true; // already here
        } else if (id == R.id.nav_itineraries) {
            int userId = new SessionManager(this).getUserId();
            Intent i = new Intent(this, ItinerariesActivity.class);
            i.putExtra("user_id", userId);
            startActivity(i);
            // keep Home highlighted when returning
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.fabAdd.getId()) {
            startActivity(new Intent(this, AddItineraryActivity.class));
        }
    }
}
