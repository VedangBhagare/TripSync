package com.example.tripsync_phone_app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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
import com.example.tripsync_phone_app.databinding.ActivityRegisterBinding;

import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Pad bottom by keyboard height so content is never hidden
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottom = Math.max(ime.bottom, bars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom);
            return insets;
        });

        // Press "done" on confirm password to submit
        binding.registerConfirmPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.registerButton.performClick();
                return true;
            }
            return false;
        });

        db = AppDatabase.getInstance(this);

        binding.registerButton.setOnClickListener(v -> {
            hideKeyboard();

            String name = safe(binding.registerUsername.getText());
            String email = safe(binding.registerEmail.getText());
            String password = safe(binding.registerPassword.getText());
            String confirm = safe(binding.registerConfirmPassword.getText());

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

            long rowId = db.userDao().insertUser(user);
            int userId = (int) rowId;

            new SessionManager(this).saveLogin(userId, user.email, user.username);

            Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.gotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private static String safe(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
