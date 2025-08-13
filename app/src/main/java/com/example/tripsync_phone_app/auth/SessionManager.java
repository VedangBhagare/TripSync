package com.example.tripsync_phone_app.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "TripSyncPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(int userId, String email, String name) {
        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .apply();
    }

    public int getUserId() { return prefs.getInt(KEY_USER_ID, -1); }
    public String getEmail() { return prefs.getString(KEY_USER_EMAIL, null); }
    public String getName() { return prefs.getString(KEY_USER_NAME, null); }

    public void clear() { prefs.edit().clear().apply(); }
}
