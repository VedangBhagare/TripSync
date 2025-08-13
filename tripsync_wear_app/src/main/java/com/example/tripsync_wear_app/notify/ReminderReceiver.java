package com.example.tripsync_wear_app.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent i) {
        String trip = i.getStringExtra("tripName");
        String addr = i.getStringExtra("address");
        String note = i.getStringExtra("note");
        String date = i.getStringExtra("date");
        String time = i.getStringExtra("time");

        NotificationManagerCompat.from(ctx).notify(
                (trip + addr + time).hashCode(),
                NotificationHelper.build(ctx, trip, addr, note, date, time).build()
        );
    }
}
