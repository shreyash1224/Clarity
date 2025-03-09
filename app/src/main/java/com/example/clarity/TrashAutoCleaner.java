package com.example.clarity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Calendar;

public class TrashAutoCleaner extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_DATE_CHANGED.equals(intent.getAction())) {

            // ✅ Check if today is the end of the month
            Calendar calendar = Calendar.getInstance();
            int lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            int today = calendar.get(Calendar.DAY_OF_MONTH);

            if (today == lastDayOfMonth) {
                // ✅ It's the last day of the month, clear the trash
                clearTrash(context);
            }
        }
    }

    private void clearTrash(Context context) {
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int rowsDeleted = db.delete("trash", null, null);
            Log.d("TrashAutoCleaner", "Trash cleared successfully. Rows deleted: " + rowsDeleted);
        } catch (Exception e) {
            Log.e("TrashAutoCleaner", "Failed to clear trash: " + e.getMessage());
        } finally {
            db.close();
        }
    }
}
