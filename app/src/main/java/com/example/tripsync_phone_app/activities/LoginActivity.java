package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.activities.HomeActivity;
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.User;
import com.example.tripsync_phone_app.databinding.ActivityLoginBinding;
import com.example.tripsync_phone_app.utils.JWTUtils;

import org.mindrot.jbcrypt.BCrypt;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.loginEmail.getText().toString().trim();
            String password = binding.loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            User user = db.userDao().getUserByEmail(email);

            if (user != null && BCrypt.checkpw(password, user.password)) {
                // ✅ Generate JWT token
                String token = JWTUtils.generateToken(email);

                // ✅ Store token and email
                getSharedPreferences("TripSyncPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("jwt_token", token)
                        .putString("email", email)
                        .putString("username", user.username)
                        .apply();

                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                // ✅ Redirect to Home
                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
            }
        });

        binding.gotoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }
}
