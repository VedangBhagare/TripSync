package com.example.tripsync_phone_app.notify;

import android.content.Context;
import android.text.TextUtils;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.tripsync_phone_app.database.AppDatabase;
import com.example.tripsync_phone_app.database.Destination;
import com.example.tripsync_phone_app.database.Itinerary;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Schedules/cancels WorkManager jobs that fire 1 hour before each destination time.
 * Keeps the rest of the app untouched.
 */
public class ReminderScheduler {

    // Work tags so we can cancel per itinerary
    private static String tagFor(int itineraryId) { return "itinerary-" + itineraryId; }

    /** Back-compat alias if any old call sites remain. */
    public static void scheduleDestination(Context ctx, int itineraryId) {
        scheduleForItinerary(ctx, itineraryId);
    }

    /** Public API: call this right after saving an itinerary & its destinations. */
    public static void scheduleForItinerary(Context ctx, int itineraryId) {
        cancelAllForItinerary(ctx, itineraryId);

        AppDatabase db = AppDatabase.getInstance(ctx.getApplicationContext());
        Itinerary it = db.itineraryDao().getItineraryById(itineraryId);
        if (it == null) return;

        List<Destination> dests = db.destinationDao().getDestinationsForItinerary(itineraryId);
        if (dests == null || dests.isEmpty()) return;

        long now = System.currentTimeMillis();
        WorkManager wm = WorkManager.getInstance(ctx.getApplicationContext());

        // We enqueue a separate OneTimeWork for each destination
        for (int i = 0; i < dests.size(); i++) {
            Destination d = dests.get(i);
            long eventAt = toEpochMillis(d.date, d.time);
            if (eventAt == Long.MAX_VALUE) continue;

            long triggerAt = eventAt - TimeUnit.HOURS.toMillis(1); // 1 hour before
            long delayMs = triggerAt - now;
            if (delayMs <= 1000) continue; // too late to schedule

            int reqCode = itineraryId * 1000 + (i & 0x3FF);

            Data input = new Data.Builder()
                    .putString(ReminderWorker.KEY_TRIP, it.tripName == null ? "" : it.tripName)
                    .putString(ReminderWorker.KEY_ADDRESS, d.address == null ? "" : d.address)
                    .putString(ReminderWorker.KEY_NOTE, d.note == null ? "" : d.note)
                    .putString(ReminderWorker.KEY_DATE, d.date == null ? "" : d.date)
                    .putString(ReminderWorker.KEY_TIME, d.time == null ? "" : d.time)
                    .putInt(ReminderWorker.KEY_REQ_CODE, reqCode)
                    .build();

            OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(tagFor(itineraryId))
                    .build();

            // put data after builder because setInputData returns the builder in older versions
            work = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .addTag(tagFor(itineraryId))
                    .setInputData(input)
                    .build();

            // Use a unique name per destination index so re-scheduling replaces the same slot
            String uniqueName = tagFor(itineraryId) + "-dest-" + i;
            wm.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, work);
        }
    }

    /** Public API: call this before re-saving an itinerary or when deleting it. */
    public static void cancelAllForItinerary(Context ctx, int itineraryId) {
        WorkManager.getInstance(ctx.getApplicationContext())
                .cancelAllWorkByTag(tagFor(itineraryId));
    }

    // --- date parsing identical to your activities ---
    private static long toEpochMillis(String dateStr, String timeStr) {
        if (TextUtils.isEmpty(dateStr)) return Long.MAX_VALUE;
        SimpleDateFormat df1 = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat df2 = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        try {
            try { cal.setTime(df1.parse(dateStr)); }
            catch (ParseException e) { cal.setTime(df2.parse(dateStr)); }
            int hour = 0, minute = 0;
            if (!TextUtils.isEmpty(timeStr) && !timeStr.startsWith("Select")) {
                try {
                    String[] parts = timeStr.split(":");
                    hour = Integer.parseInt(parts[0]);
                    minute = Integer.parseInt(parts[1]);
                } catch (Exception ignore) {}
            }
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }
}
