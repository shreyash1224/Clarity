package com.example.clarity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.clarity.DiaryDatabaseHelper;
import com.example.clarity.Task;
import com.example.clarity.TaskNotificationManager;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("taskId", -1);
        String action = intent.getAction();

        if (taskId == -1 || action == null) return;

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(context);
        Task task = dbHelper.getTaskById(taskId);

        if (action.equals("DONE")) {
            dbHelper.updateTaskCompletion(taskId, "Completed");
            TaskNotificationManager.cancelNotification(context, taskId);
        } else if (action.equals("SNOOZE")) {
            TaskNotificationManager.triggerNotification(context, task);
        }
    }
}