package com.example.tripsync_phone_app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.auth.SessionManager;
import com.example.tripsync_phone_app.databinding.ActivityHomeBinding;
import com.example.tripsync_phone_app.notify.NotificationHelper;

import com.google.android.material.navigation.NavigationBarView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity
        implements View.OnClickListener, NavigationBarView.OnItemSelectedListener {

    private ActivityHomeBinding binding;

    // --- NEW: runtime permission launcher for Android 13+ ---
    private final ActivityResultLauncher<String> requestNotifPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // Show a quick test notification so you can see it immediately
                    showTestNotification();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Top app bar
        setSupportActionBar(binding.topBar);

        String username = getSharedPreferences("TripSyncPrefs", MODE_PRIVATE)
                .getString("username", "User");
        binding.welcomeText.setText(getString(R.string.welcome_user, username));

        // Single entry to Add Itinerary
        binding.btnStartPlanning.setOnClickListener(this);

        // Bottom navigation
        binding.bottomNav.setOnItemSelectedListener(this);
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        // Top bar menu (profile only)
        binding.topBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile) {
                startActivity(new Intent(HomeActivity.this, UserProfileActivity.class));
                return true;
            }
            return false;
        });

        // --- NEW: make sure we have notification permission (Android 13+) ---
        maybeAskForNotificationPermission();
    }

    // --- NEW: Ask for POST_NOTIFICATIONS on API 33+ ---
    private void maybeAskForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                // Already granted; optionally show a one-time test
                // showTestNotification();
            }
        } else {
            // On older Android, permission isn’t required; you can test too
            // showTestNotification();
        }
    }

    // --- NEW: immediate test notification so you can see it in the phone’s notification bar ---
    private void showTestNotification() {

        // just some readable time text
        String now = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(new Date());


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            return true;
        } else if (id == R.id.nav_itineraries) {
            int userId = new SessionManager(this).getUserId();
            Intent i = new Intent(this, ItinerariesActivity.class);
            i.putExtra("user_id", userId);
            startActivity(i);
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
            return true;
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, UserProfileActivity.class));
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == binding.btnStartPlanning.getId()) {
            startActivity(new Intent(this, AddItineraryActivity.class));
        }
    }
}
