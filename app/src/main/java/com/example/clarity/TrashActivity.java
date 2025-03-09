package com.example.clarity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class TrashActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayList<String> pageTitles = new ArrayList<>();
    private ArrayList<Integer> pageIds = new ArrayList<>();
    private DiaryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        listView = findViewById(R.id.lvTrash);
        dbHelper = new DiaryDatabaseHelper(this);

        loadTrashPages();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            int pageId = pageIds.get(position);
            showRestoreDeleteOptions(pageId);
        });
    }

    private void loadTrashPages() {
        pageTitles.clear();
        pageIds.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pageId, pageTitle FROM pages WHERE pageStatus = 'trashed'", null);

        while (cursor.moveToNext()) {
            int pageId = cursor.getInt(0);
            String pageTitle = cursor.getString(1);
            pageIds.add(pageId);
            pageTitles.add(pageTitle);
        }
        cursor.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pageTitles);
        listView.setAdapter(adapter);
    }

    private void showRestoreDeleteOptions(int pageId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Manage Page")
                .setMessage("Do you want to restore or delete this page?")
                .setPositiveButton("Restore", (dialog, which) -> restorePage(pageId))
                .setNegativeButton("Delete Permanently", (dialog, which) -> permanentlyDeletePage(pageId))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void restorePage(int pageId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pageStatus", "active");

        int rowsUpdated = db.update("pages", values, "pageId = ?", new String[]{String.valueOf(pageId)});
        db.close();

        if (rowsUpdated > 0) {
            Toast.makeText(this, "Page Restored", Toast.LENGTH_SHORT).show();
            loadTrashPages();
        } else {
            Toast.makeText(this, "Failed to Restore Page", Toast.LENGTH_SHORT).show();
        }
    }

    private void permanentlyDeletePage(int pageId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Delete all resources linked to the page
        db.delete("resources", "pageId = ?", new String[]{String.valueOf(pageId)});

        // Finally, delete the page itself
        int rowsDeleted = db.delete("pages", "pageId = ?", new String[]{String.valueOf(pageId)});
        db.close();

        if (rowsDeleted > 0) {
            Toast.makeText(this, "Page Permanently Deleted", Toast.LENGTH_SHORT).show();
            loadTrashPages();
        } else {
            Toast.makeText(this, "Failed to Delete Page", Toast.LENGTH_SHORT).show();
        }
    }
}
