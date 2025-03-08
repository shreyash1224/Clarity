package com.example.clarity;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.clarity.NotificationActionReceiver;

import java.util.HashSet;

public class TaskNotificationManager {

    private static final int MAX_SNOOZE_COUNT = 3;
    private static HashSet<Integer> notifiedTasks = new HashSet<>();

    public static void triggerNotification(Context context, Task task) {
        Log.d("Notification", "📩 Triggering Notification for Task: " + task.getTaskTitle());

        // ✅ Prevent duplicate notifications unless modified
        if (notifiedTasks.contains(task.getTaskId()) && task.getCompletion().equals("Pending")) {
            Log.d("Notification", "🚫 Task already notified. Skipping notification for: " + task.getTaskTitle());
            return;
        }

        // ✅ Check if the app has notification permission
        if (!PermissionManager.hasNotificationPermission((DiaryPageActivity) context)) {
            Log.w("Notification", "⚠️ Notification permission not granted! Requesting permission...");

            // ✅ Request notification permission if not granted
            if (context instanceof DiaryPageActivity) {
                PermissionManager.requestNotificationPermission((DiaryPageActivity) context);
            }

            Log.w("Notification", "📭 Notification will be triggered later if permission is granted.");
            return;
        }

        // ✅ If permission is granted, proceed with notification
        notifiedTasks.add(task.getTaskId());
        Log.d("Notification", "✅ Permission granted. Proceeding to trigger notification for: " + task.getTaskTitle());

        // ✅ Create the "Done" action
        Intent doneIntent = new Intent(context, NotificationActionReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("taskId", task.getTaskId());

        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, task.getTaskId(), doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ✅ Create the "Snooze" action
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("taskId", task.getTaskId());
        snoozeIntent.putExtra("snoozeCount", 1);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, task.getTaskId() + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ✅ Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_channel")
                .setSmallIcon(R.drawable.ic_task)
                .setContentTitle(task.getTaskTitle())
                .setContentText("Task is pending.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_done, "Done", donePendingIntent)
                .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
                .setAutoCancel(true);

        // ✅ Trigger the notification
        try {
            if (ActivityCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(task.getTaskId(), builder.build());
                Log.d("Notification", "🎉 Notification TRIGGERED for: " + task.getTaskTitle());
            } else {
                Log.w("Notification", "⚠️ Permission not granted. Skipping notification.");
            }
        } catch (SecurityException e) {
            Log.e("Notification", "❌ SecurityException: Cannot trigger notification without permission.");
        }

        Log.d("Notification", "🎉 Notification TRIGGERED for: " + task.getTaskTitle() +
                " [taskId: " + task.getTaskId() + "]");
    }

    public static void snoozeNotification(Context context, Task task, int snoozeCount) {
        if (snoozeCount > MAX_SNOOZE_COUNT) {
            Log.d("Notification", "Max snooze limit reached. No more notifications.");
            return;
        }

        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("SNOOZE");
        snoozeIntent.putExtra("taskId", task.getTaskId());
        snoozeIntent.putExtra("snoozeCount", snoozeCount + 1);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, task.getTaskId() + 1000, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + 2 * 60 * 1000; // 2 minutes later

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, snoozePendingIntent);
    }

    public static void cancelNotification(Context context, int taskId) {
        NotificationManagerCompat.from(context).cancel(taskId);

        Intent intent = new Intent(context, NotificationActionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, taskId + 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }



    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "task_channel",
                    "Task Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for pending tasks");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


}
