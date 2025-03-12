package com.example.clarity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.app.PendingIntent;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("taskId", -1);
        int snoozeCount = intent.getIntExtra("snoozeCount", 0);

        if (taskId == -1) return;

        // ✅ Check notification permission (Android 13+)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Notification", "🚫 Missing notification permission. Skipping notification.");
            return;
        }

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(context);
        Task task = dbHelper.getTaskById(taskId);

        if (task == null || "Completed".equals(task.getCompletion())) {
            Log.d("Notification", "🚫 Task no longer exists or is completed. Skipping notification.");
            return;
        }

        // ✅ Ensure notification channel exists
        TaskNotificationManager.createNotificationChannel(context);

        // ✅ Create "Done" action
        Intent doneIntent = new Intent(context, NotificationActionReceiver.class);
        doneIntent.setAction("DONE");
        doneIntent.putExtra("taskId", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, taskId, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // ✅ Create "Snooze" action (if within limit)
        NotificationCompat.Action snoozeAction = null;
        if (snoozeCount < TaskNotificationManager.MAX_SNOOZE_COUNT) {
            Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
            snoozeIntent.setAction("SNOOZE");
            snoozeIntent.putExtra("taskId", taskId);
            snoozeIntent.putExtra("snoozeCount", snoozeCount);

            int uniqueSnoozeRequestCode = taskId * 10 + snoozeCount;
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                    context, uniqueSnoozeRequestCode, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            snoozeAction = new NotificationCompat.Action(R.drawable.ic_snooze, "Snooze", snoozePendingIntent);
        }

        // ✅ Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "task_channel")
                .setSmallIcon(R.drawable.ic_task)
                .setContentTitle(task.getTaskTitle())
                .setContentText("Task is due!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_done, "Done", donePendingIntent)
                .setAutoCancel(true);

        if (snoozeAction != null) {
            builder.addAction(snoozeAction);
        }

        NotificationManagerCompat.from(context).notify(taskId, builder.build());
        Log.d("Notification", "🎉 Notification displayed for: " + task.getTaskTitle());
    }
}
