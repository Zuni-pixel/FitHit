package com.example.fithit;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    public static void scheduleNotifications(Context context) {
        scheduleAtFixedTime(context, 8);   // 8 AM
        scheduleAtFixedTime(context, 12);  // 12 PM
        scheduleAtFixedTime(context, 16);  // 4 PM
        scheduleAtFixedTime(context, 20);  // 8 PM
    }

    private static void scheduleAtFixedTime(Context context, int hourOfDay) {
        Calendar now = Calendar.getInstance();
        Calendar target = (Calendar) now.clone();
        target.set(Calendar.HOUR_OF_DAY, hourOfDay);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DATE, 1); // Schedule for next day if time has passed
        }

        long delayMillis = target.getTimeInMillis() - now.getTimeInMillis();

        Data inputData = new Data.Builder()
                .putInt("hourOfDay", hourOfDay)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        String uniqueWorkName = "notification_" + hourOfDay;

        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.KEEP, // Do not reschedule if already exists
                request
        );

        Log.d("NOTIFY", "Scheduled notification for " + hourOfDay + ":00 (in " + (delayMillis / 1000 / 60) + " min)");
    }
}
