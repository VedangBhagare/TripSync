package com.example.tripsync_phone_app.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tripsync_phone_app.R;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private int progress = 0;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.welcomeText.setText("Welcome to TripSync");

        animateProgress();
    }

    private void animateProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progress += 2;
                binding.progressBar.setProgress(progress);
                binding.percentText.setText(progress + "%");

                if (progress < 100) {
                    handler.postDelayed(this, 30); // smooth loading speed
                } else {
                    // ✅ After loading, go to LoginActivity
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }, 30);
    }
}