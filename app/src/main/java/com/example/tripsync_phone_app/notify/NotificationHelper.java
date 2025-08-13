package com.example.tripsync_phone_app.notify;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.tripsync_phone_app.R;

public class NotificationHelper {

    private static final String CHANNEL_ID = "trip_reminders";
    private static final String CHANNEL_NAME = "Trip Reminders";
    private static final String CHANNEL_DESC = "Notifications one hour before destination time";

    private static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription(CHANNEL_DESC);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    public static void showDestination(Context ctx,
                                       int notificationId,
                                       String tripName,
                                       String destination,
                                       String note,
                                       String date,
                                       String time) {

        ensureChannel(ctx);

        String title = "Trip in 1 hour: " + (tripName == null ? "" : tripName);
        StringBuilder content = new StringBuilder();
        if (destination != null && !destination.isEmpty()) content.append(destination);
        if (time != null && !time.isEmpty()) {
            if (content.length() > 0) content.append(" • ");
            content.append(time);
        }
        if (date != null && !date.isEmpty()) {
            if (content.length() > 0) content.append(" • ");
            content.append(date);
        }
        if (note != null && !note.isEmpty()) {
            if (content.length() > 0) content.append("\n");
            content.append(note);
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                // Use an icon that exists in your project. `ic_notification` caused your error.
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content.toString())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content.toString()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(ctx).notify(notificationId, b.build());
    }
}
