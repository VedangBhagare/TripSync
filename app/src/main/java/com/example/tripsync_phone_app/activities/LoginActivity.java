package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.tripsync_phone_app.R;
import com.example.tripsync_phone_app.auth.SessionManager;
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

        // Make the screen pad itself to the keyboard height (works across OEMs)
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottom = Math.max(ime.bottom, bars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });

        db = AppDatabase.getInstance(this);

        // Trigger login when the user presses "Done" on the keyboard
        binding.loginPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.loginButton.performClick();
                return true;
            }
            return false;
        });

        binding.loginButton.setOnClickListener(v -> {
            hideKeyboard();

            String email = binding.loginEmail.getText() == null ? "" :
                    binding.loginEmail.getText().toString().trim();
            String password = binding.loginPassword.getText() == null ? "" :
                    binding.loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            User user = db.userDao().getUserByEmail(email);

            if (user != null && BCrypt.checkpw(password, user.password)) {
                String token = JWTUtils.generateToken(email);

                getSharedPreferences("TripSyncPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("jwt_token", token)
                        .putString("email", email)
                        .putString("username", user.username)
                        .apply();

                SessionManager session = new SessionManager(this);
                session.saveLogin(user.id, user.email, user.username);

                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                startActivity(new Intent(this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
            }
        });

        binding.gotoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        binding.forgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
