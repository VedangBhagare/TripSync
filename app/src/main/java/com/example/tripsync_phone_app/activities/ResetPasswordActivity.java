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
import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.User;
import com.example.tripsync_phone_app.databinding.ActivityResetPasswordBinding;

import org.mindrot.jbcrypt.BCrypt;

public class ResetPasswordActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Keyboard-safe padding: lets the card move above the IME
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets ime  = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    Math.max(ime.bottom, bars.bottom));
            return insets;
        });

        // Press "done" on confirm password to submit
        binding.resetConfirmPassword.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_UP)) {
                binding.resetButton.performClick();
                return true;
            }
            return false;
        });

        db = AppDatabase.getInstance(this);

        binding.resetButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                hideKeyboard();

                String email   = safe(binding.resetEmail.getText());
                String newPass = safe(binding.resetNewPassword.getText());
                String confirm = safe(binding.resetConfirmPassword.getText());

                if (email.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirm)) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.password_mismatch), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPass.length() < 8) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
                    return;
                }

                User user = db.userDao().getUserByEmail(email);
                if (user == null) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.email_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                String hashed = BCrypt.hashpw(newPass, BCrypt.gensalt());
                int rows = db.userDao().updatePassword(email, hashed);
                if (rows > 0) {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.password_reset_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(ResetPasswordActivity.this, getString(R.string.password_reset_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private static String safe(CharSequence cs) { return cs == null ? "" : cs.toString().trim(); }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
