package com.example.tripsync_wear_app.notify;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.tripsync_wear_app.R;
import com.example.tripsync_wear_app.model.DestinationModel;
import com.example.tripsync_wear_app.model.ItineraryModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {
    private static final String TAG = "TripSyncNotify";

    public static final String CH_ID = "tripsync_itinerary_v2";

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= 26) {
            // OLD: IMPORTANCE_DEFAULT
            NotificationChannel ch = new NotificationChannel(
                    CH_ID, "Trip reminders", NotificationManager.IMPORTANCE_HIGH); // <- NEW (HIGH for heads-up)
            ch.setDescription("1-hour early trip/destination reminders");           // <- NEW
            ch.enableVibration(true);                                               // <- NEW
            ch.setVibrationPattern(new long[]{0, 250, 150, 250});                   // <- NEW
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC); // <- NEW
            ctx.getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }


    public static void scheduleAll(Context ctx, List<ItineraryModel> list) {
        ensureChannel(ctx);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences sp = ctx.getSharedPreferences("TripSyncWear", Context.MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarms not allowed yet — open Settings to grant permission");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US);
        int scheduled = 0;

        for (ItineraryModel it : list) {
            for (int idx = 0; idx < it.destinations.size(); idx++) {
                DestinationModel d = it.destinations.get(idx);

                long when = parseMillis(sdf, d.date, d.time);
                if (when <= 0) { Log.d(TAG, "skip invalid: " + d.date + " " + d.time); continue; }

                long trigger = when - 60 * 60 * 1000L; // 1 hour before
                long now = System.currentTimeMillis();
                if (trigger <= now) { Log.d(TAG, "skip past trigger for " + it.tripName); continue; }

                String key = "sched_" + it.id + "_" + idx + "_" + d.date + "_" + d.time;
                if (sp.getBoolean(key, false)) { Log.d(TAG, "already scheduled " + key); continue; }

                Intent i = new Intent(ctx, ReminderReceiver.class)
                        .putExtra("tripName", it.tripName)
                        .putExtra("address", d.address)
                        .putExtra("note", d.note)
                        .putExtra("date", d.date)
                        .putExtra("time", d.time);

                int reqCode = (it.id * 1000) + idx;
                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, reqCode, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, trigger, pi);
                }
                sp.edit().putBoolean(key, true).apply();
                scheduled++;
                Log.d(TAG, "scheduled " + it.tripName + " → " + d.date + " " + d.time + " (fires 1h early)");
            }
        }
        Log.d(TAG, "total scheduled: " + scheduled);
    }

    private static long parseMillis(SimpleDateFormat sdf, String date, String time) {
        try { return sdf.parse(date + " " + time).getTime(); }
        catch (ParseException e) { return -1; }
    }

    public static NotificationCompat.Builder build(Context ctx,
                                                   String trip, String address,
                                                   String note, String date, String time) {
        ensureChannel(ctx);
        return new NotificationCompat.Builder(ctx, CH_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(trip)
                .setContentText(address + " — " + note)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(address + " — " + note + "\n" + date + " " + time))
                .setAutoCancel(true)
                // OLD: PRIORITY_DEFAULT
                .setPriority(NotificationCompat.PRIORITY_MAX)              // <- NEW (forces heads-up for compat)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)         // <- NEW (or CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)       // <- NEW
                .setDefaults(NotificationCompat.DEFAULT_ALL);              // <- NEW (sound/vibrate per channel)
    }

}
