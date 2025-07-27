package com.example.fithit;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int hour = getInputData().getInt("hourOfDay", -1);

        boolean isWorkoutCompleted = getApplicationContext()
                .getSharedPreferences("workoutPrefs", Context.MODE_PRIVATE)
                .getBoolean("workoutCompletedToday", false);

        String message;
        switch (hour) {
            case 8:
                message = "Good morning! Time for your workout.";
                break;
            case 12:
                message = "Midday stretch — move a bit!";
                break;
            case 16:
                message = "Afternoon fitness reminder.";
                break;
            case 20:
                message = isWorkoutCompleted
                        ? "Evening workout — finish strong!"
                        : "Evening reminder — you haven't completed your workout today!";
                break;
            default:
                message = "Time for your workout session! Let’s crush it.";
                break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                "fitness_channel"
        )
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Workout Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(getApplicationContext()).notify(hour, builder.build());

        Log.d("NOTIFY", "Notification shown for " + hour + ":00 → " + message);
        return Result.success();
    }
}