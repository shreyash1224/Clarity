package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clarity.DiaryDatabaseHelper;
import com.example.clarity.LoginActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

    }


    public void deleteUser(View view) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize database helper
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            int pageId = -1;
            // Step 1: Find all pages related to this user
            Cursor cursor = db.rawQuery("SELECT pageId FROM pages WHERE userId = ?", new String[]{String.valueOf(userId)});
            if (cursor.moveToFirst()) {
                do {
                    pageId = cursor.getInt(0);

                    // Step 2: Delete tasks linked to these pages (if stored as resources)
                    db.delete("resources", "pageId = ?", new String[]{String.valueOf(pageId)});
                } while (cursor.moveToNext());
            }
            cursor.close();

            // Step 3: Delete pages of this user
            db.delete("pages", "userId = ?", new String[]{String.valueOf(userId)});
            // Step 2: Delete tasks linked to these pages
            db.delete("resources", "pageId = ? AND resourceType = 'task'", new String[]{String.valueOf(pageId)});


            // Step 4: Delete user
            int deletedUser = db.delete("users", "userId = ?", new String[]{String.valueOf(userId)});

            if (deletedUser > 0) {
                db.setTransactionSuccessful();
                Toast.makeText(this, "User and related pages/tasks deleted!", Toast.LENGTH_SHORT).show();

                // Clear shared preferences (logout user)
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                // Redirect to LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to delete user!", Toast.LENGTH_SHORT).show();
            }
        } finally {
            db.endTransaction();
            db.close();
        }
    }

}
