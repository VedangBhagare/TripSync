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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.tripsync_wear_app.data.DataLayerHelper;
import com.example.tripsync_wear_app.data.ItineraryStore;
import com.example.tripsync_wear_app.databinding.ActivityMainBinding;
import com.example.tripsync_wear_app.model.ItineraryModel;
import com.example.tripsync_wear_app.notify.NotificationHelper;
import com.example.tripsync_wear_app.ui.ItineraryAdapter;

import java.util.List;

public class MainActivity extends ComponentActivity implements View.OnClickListener {
    private ActivityMainBinding binding;
    private ItineraryAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new ItineraryAdapter();
        binding.rvItineraries.setLayoutManager(new LinearLayoutManager(this));
        binding.rvItineraries.setAdapter(adapter);

        SharedPreferences sp = getSharedPreferences("TripSyncWear", MODE_PRIVATE);
        binding.ckNotifications.setChecked(sp.getBoolean("notify_enabled", true));
        binding.ckNotifications.setOnCheckedChangeListener((b, checked) ->
                sp.edit().putBoolean("notify_enabled", checked).apply());

        binding.btnSync.setOnClickListener(this);

        ItineraryStore.itineraries().observe(this, (List<ItineraryModel> list) -> {
            adapter.submit(list);
            // (Optional) auto-schedule on new data if user re-enabled check box
            if (sp.getBoolean("notify_enabled", true)) {
                NotificationHelper.scheduleAll(this, list);
            }
        });

        requestRuntimePermissionsIfNeeded();
        NotificationHelper.ensureChannel(this);

        // Show whatever we last received so UI isn't empty on first open
        String cached = getSharedPreferences("TripSyncWear", MODE_PRIVATE)
                .getString("last_json", null);
        if (cached != null) {
            adapter.submit(com.example.tripsync_wear_app.util.JsonParser.parseItineraries(cached));
        }

    }

    @Override protected void onResume() {
        super.onResume();
        DataLayerHelper.pull(this);   // auto-refresh when returning to the app
    }

    @Override public void onClick(View v) {
        if (v.getId() == binding.btnSync.getId()) DataLayerHelper.pull(this);
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
