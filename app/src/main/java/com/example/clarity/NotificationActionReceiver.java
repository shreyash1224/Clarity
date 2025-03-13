package com.example.clarity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("taskId", -1);
        int snoozeCount = intent.getIntExtra("snoozeCount", 0);
        String action = intent.getAction();

        if (taskId == -1 || action == null) return;

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(context);
        Task task = dbHelper.getTaskById(taskId);

        if (task == null) {
            Log.d("Notification", "🚫 Task not found. Skipping action.");
            return;
        }

        switch (action) {
            case "DONE":
                Log.d("Notification", "✅ 'Done' clicked. Marking task as completed.");
                dbHelper.updateTaskCompletion(taskId, "Completed");
                TaskNotificationManager.cancelNotification(context, taskId);
                break;

            case "SNOOZE":
                Log.d("Notification", "⏳ 'Snooze' clicked. Snooze count: " + snoozeCount);

//                if (snoozeCount >= TaskNotificationManager.MAX_SNOOZE_COUNT) {
//                    Log.d("Notification", "🚫 Max snooze limit reached. No more snoozes.");
//                    return;
//                }

                // ❌ Cancel the current notification first
                TaskNotificationManager.cancelNotification(context, taskId);
                Log.d("Notification", "🛑 Existing notification canceled before snoozing.");

                // 🕐 Schedule a new snooze notification after 5 seconds
                TaskNotificationManager.snoozeNotification(context, task, snoozeCount);
                break;

        }
    }
}
