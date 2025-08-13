package com.example.tripsync_wear_app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.tripsync_wear_app.data.DataLayerHelper;
import com.example.tripsync_wear_app.databinding.ActivityMainBinding;
import com.example.tripsync_wear_app.notify.NotificationHelper;

public class MainActivity extends ComponentActivity implements View.OnClickListener {
    private ActivityMainBinding binding;

    private static final long AUTO_PULL_MS = 15_000; // 15s
    private final Runnable autoPull = new Runnable() {
        @Override public void run() {
            if (isFinishing() || isDestroyed()) return;
            DataLayerHelper.pull(MainActivity.this);
            binding.getRoot().postDelayed(this, AUTO_PULL_MS);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Buttons
        binding.btnItineraries.setOnClickListener(this);
        binding.btnAddItinerary.setOnClickListener(this);

        // Channel + permissions as before
        requestRuntimePermissionsIfNeeded();
        NotificationHelper.ensureChannel(this);

        // Warm up a pull shortly after layout so the node connection is ready
        binding.getRoot().postDelayed(() -> DataLayerHelper.pull(this), 600);
    }

    @Override protected void onStart() {
        super.onStart();
        binding.getRoot().postDelayed(() -> DataLayerHelper.pull(this), 600);
    }

    @Override protected void onResume() {
        super.onResume();
        DataLayerHelper.pull(this);
        binding.getRoot().removeCallbacks(autoPull);
        binding.getRoot().postDelayed(autoPull, AUTO_PULL_MS);
    }

    @Override protected void onPause() {
        super.onPause();
        binding.getRoot().removeCallbacks(autoPull);
    }

    // Button click
    @Override public void onClick(View v) {
        int id = v.getId();
        if (id == binding.btnItineraries.getId()) {
            startActivity(new Intent(this, ItinerariesActivity.class));
        } else if (id == binding.btnAddItinerary.getId()) {
            startActivity(new Intent(this, AddWearItineraryActivity.class));
        }
    }

    private void requestRuntimePermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                } catch (Exception ignored) {}
            }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] p, @NonNull int[] r) {
        super.onRequestPermissionsResult(requestCode, p, r);
        if (requestCode == 101 && r.length > 0 && r[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
        }
    }
}
