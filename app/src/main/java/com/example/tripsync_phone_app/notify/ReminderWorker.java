package com.example.tripsync_phone_app.notify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Runs at the scheduled time and shows the phone notification.
 * Triggered by ReminderScheduler via WorkManager.
 */
public class ReminderWorker extends Worker {

    public static final String KEY_TRIP     = "tripName";
    public static final String KEY_ADDRESS  = "address";
    public static final String KEY_NOTE     = "note";
    public static final String KEY_DATE     = "date";
    public static final String KEY_TIME     = "time";
    public static final String KEY_REQ_CODE = "reqCode";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        Data d = getInputData();
        String trip  = d.getString(KEY_TRIP);
        String addr  = d.getString(KEY_ADDRESS);
        String note  = d.getString(KEY_NOTE);
        String date  = d.getString(KEY_DATE);
        String time  = d.getString(KEY_TIME);
        int reqCode  = d.getInt(KEY_REQ_CODE, (int) System.currentTimeMillis());

        NotificationHelper.showDestination(
                getApplicationContext(),
                reqCode,
                trip,
                addr,
                note,
                date,
                time
        );
        return Result.success();
    }
}
