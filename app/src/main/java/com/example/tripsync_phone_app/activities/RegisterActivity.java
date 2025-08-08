package com.example.tripsync_phone_app.activities;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.User;
import com.example.tripsync_phone_app.databinding.ActivityRegisterBinding;

import com.example.tripsync_phone_app.R;


import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        binding.registerButton.setOnClickListener(v -> {
            String name = binding.registerUsername.getText().toString().trim();
            String email = binding.registerEmail.getText().toString().trim();
            String password = binding.registerPassword.getText().toString();
            String confirm = binding.registerConfirmPassword.getText().toString();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 8) {
                Toast.makeText(this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
                return;
            }

            User existingUser = db.userDao().getUserByEmail(email);
            if (existingUser != null) {
                Toast.makeText(this, getString(R.string.email_exists), Toast.LENGTH_SHORT).show();
                return;
            }

            User user = new User();
            user.username = name;
            user.email = email;
            user.password = BCrypt.hashpw(password, BCrypt.gensalt());

            db.userDao().insertUser(user);
            Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
            finish(); // return to login
        });
    }
}
