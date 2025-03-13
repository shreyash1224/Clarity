package com.example.clarity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TaskNotificationManager {

    public static final int MAX_SNOOZE_COUNT = 3;
    private static final String CHANNEL_ID = "task_channel";

    /**
     * Schedules a notification for the given task at its end time.
     */
    public static void scheduleTaskNotification(Context context, Task task) {
        long triggerTime = getTaskEndTimeInMillis(task) - (20 * 1000); // 20 seconds before task end

        if (triggerTime <= System.currentTimeMillis()) {
            Log.w("Notification", "⏳ Task end time has already passed. Skipping notification.");
            return;
        }

        Log.d("Notification", "📅 Scheduling notification for Task: " + task.getTaskTitle() + " at " + task.getEndTime());

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("taskId", task.getTaskId());
        intent.putExtra("snoozeCount", 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // ✅ Create a fresh PendingIntent instance
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, task.getTaskId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ✅ Cancel any existing notification before scheduling a new one
        alarmManager.cancel(pendingIntent);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String formattedTime = sdf.format(new Date(triggerTime));
        Log.d("Notification", "⏰ Notification will trigger at (IST): " + formattedTime);

        // ✅ Delay the scheduling slightly to prevent race conditions
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }, 500);
    }

    /**
     * Schedules a snooze notification after 5 seconds.
     */
    public static void snoozeNotification(Context context, Task task, int snoozeCount) {
        if (snoozeCount >= MAX_SNOOZE_COUNT) {
            Log.d("Notification", "🚫 Maximum snooze limit reached for Task ID: " + task.getTaskId());
            return;
        }

        Log.d("Notification", "⏳ Scheduling snooze notification in 5 seconds. Snooze count: " + snoozeCount);

        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtra("taskId", task.getTaskId());
        snoozeIntent.putExtra("snoozeCount", snoozeCount + 1);

        // Use a unique request code to prevent conflicts
        int uniqueSnoozeRequestCode = task.getTaskId() * 100 + snoozeCount;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, uniqueSnoozeRequestCode, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Cancel existing snooze notification before setting a new one
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);

        long triggerTime = System.currentTimeMillis() + 5 * 1000; // 5 seconds later
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    /**
     * Cancels a scheduled notification for a task.
     */
    public static void cancelNotification(Context context, int taskId) {
        Log.d("Notification", "🗑️ Cancelling notification for task ID: " + taskId);

        NotificationManagerCompat.from(context).cancel(taskId);

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Creates the notification channel (for Android 8.0+).
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for pending tasks");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Converts the task end time to milliseconds.
     */
    private static long getTaskEndTimeInMillis(Task task) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date endDate = sdf.parse(task.getEndTime());
            if (endDate != null) {
                return endDate.getTime();
            }
        } catch (ParseException e) {
            Log.e("Notification", "❌ Error parsing end time: " + task.getEndTime(), e);
        }
        return System.currentTimeMillis();
    }
}
