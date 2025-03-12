package com.example.clarity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TaskNotificationManager {

    public static final int MAX_SNOOZE_COUNT = 3;
    private static final String CHANNEL_ID = "task_channel";

    /**
     * Schedules a notification for the given task at its end time.
     */
    public static void scheduleTaskNotification(Context context, Task task) {
        long triggerTime = getTaskEndTimeInMillis(task);
        if (triggerTime <= System.currentTimeMillis()) {
            Log.w("Notification", "⏳ Task end time has already passed. Skipping notification.");
            return;
        }

        Log.d("Notification", "📅 Scheduling notification for Task: " + task.getTaskTitle() + " at " + task.getEndTime());

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("taskId", task.getTaskId());
        intent.putExtra("snoozeCount", 0);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, task.getTaskId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    /**
     * Schedules a snooze notification after 5 seconds.
     */
    public static void snoozeNotification(Context context, Task task, int snoozeCount) {
        if (snoozeCount >= MAX_SNOOZE_COUNT) {
            Log.d("Notification", "🚫 Max snooze limit reached. No more snoozes.");
            return;
        }

        Log.d("Notification", "⏳ Scheduling snooze notification in 5 seconds. Snooze count: " + snoozeCount);

        Intent snoozeIntent = new Intent(context, NotificationReceiver.class);
        snoozeIntent.putExtra("taskId", task.getTaskId());
        snoozeIntent.putExtra("snoozeCount", snoozeCount + 1);

        int uniqueSnoozeRequestCode = task.getTaskId() * 10 + snoozeCount;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, uniqueSnoozeRequestCode, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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


    public static void cancelNotificationAfterSnooze(Context context, int taskId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(taskId);
        Log.d("Notification", "🛑 Notification canceled for taskId: " + taskId);
    }

}
