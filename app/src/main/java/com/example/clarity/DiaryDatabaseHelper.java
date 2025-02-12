package com.example.clarity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class DiaryDatabaseHelper extends SQLiteOpenHelper {

    //Database Details
    private static final String DATABASE_NAME = "ClarityDB";
    private static final int DATABASE_VERSION = 1;



    public DiaryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        SQLiteDatabase db = this.getWritableDatabase();

        Log.d("DatabaseHelper", "DiaryDatabaseHelper constructor called.");
    }

    @Override

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;"); // Enable foreign keys

        String userTable = "CREATE TABLE IF NOT EXISTS users("
                + "userId INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "userName TEXT NOT NULL CHECK (LENGTH(userName) > 0 AND LENGTH(userName) <= 50), "
                + "userPassword TEXT NOT NULL CHECK (LENGTH(userPassword) >= 6));";

        db.execSQL(userTable);

        String pageTable = "CREATE TABLE IF NOT EXISTS pages("
                + "pageId INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "pageTitle TEXT NOT NULL CHECK (LENGTH(pageTitle) > 0 AND LENGTH(pageTitle) <= 100), "
                + "pageDate DATETIME DEFAULT (datetime('now', 'localtime')), "
                + "pageCount INTEGER DEFAULT 1 CHECK (pageCount >= 1), "
                + "userId INTEGER NOT NULL, "
                + "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE);";

        db.execSQL(pageTable);

        String resourceTable = "CREATE TABLE IF NOT EXISTS resources("
                + "resourceId INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "resourceUri TEXT NOT NULL CHECK (LENGTH(resourceUri) > 0), "
                + "resourceNumber INTEGER CHECK (resourceNumber > 0), "
                + "resourceType TEXT NOT NULL CHECK (LENGTH(resourceType) <= 20), "
                + "pageId INTEGER NOT NULL, "
                + "FOREIGN KEY (pageId) REFERENCES pages(pageId) ON DELETE CASCADE);";

        db.execSQL(resourceTable);

        // Add indexes to optimize foreign key searches
        db.execSQL("CREATE INDEX idx_userId ON pages(userId);");
        db.execSQL("CREATE INDEX idx_pageId ON resources(pageId);");

        Log.d("Database", "All tables created successfully.");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON;"); // Ensure foreign keys remain enabled
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL("DROP TABLE IF EXISTS pages" );
        db.execSQL("DROP TABLE IF EXISTS resources");
        onCreate(db);
    }
    public void addUser(int userId, String username, String userPassword) {
        Log.d("DatabaseHelper", "Adding user: userId=" + userId + ", username=" + username + ", userPassword=" + userPassword);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("userPassword", userPassword);
        values.put("username", username);
        db.insert("users", null, values);
        db.close();
    }


    /*





    }




    public boolean authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null, COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);

        boolean authenticated = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return authenticated;
    }

    public boolean usernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public long createUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);
    }

    // Diary Page Management
    public String addPage(String title, String content, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("date", date);

        long newRowId = db.insert("diary_pages", null, values);

        if (newRowId == -1) {
            Log.e("DiaryDatabaseHelper", "Failed to insert new page.");
            return null;
        }

        Log.d("DiaryDatabaseHelper", "Page added successfully with ID: " + newRowId);
        return String.valueOf(newRowId);
    }




    @SuppressLint("Range")
    public DiaryPage getPageById(String pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DIARY, null, COLUMN_PAGE_ID + "=?", new String[]{pageId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
            cursor.close();
            return new DiaryPage(pageId, title, content);
        }
        return null;
    }


    public void updatePage(String pageId, String newTitle, String newContent) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, newTitle);
        values.put(COLUMN_CONTENT, newContent);
        db.update(TABLE_DIARY, values, COLUMN_PAGE_ID + "=?", new String[]{pageId});
        db.close();
    }

    public void deletePage(String pageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("diary", "id=?", new String[]{pageId});
        db.close();
    }


    public ArrayList<DiaryPage> getAllPages() {
        ArrayList<DiaryPage> pages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM diary_pages ORDER BY date DESC", null);

        Log.d("DiaryDatabaseHelper", "Fetching all pages...");

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String title = cursor.getString(cursor.getColumnIndex("title"));
                String content = cursor.getString(cursor.getColumnIndex("content"));
                String date = cursor.getString(cursor.getColumnIndex("date"));

                Log.d("DiaryDatabaseHelper", "Page Loaded: ID=" + id + ", Title=" + title);

                pages.add(new DiaryPage(id, title, content, date));
            } while (cursor.moveToNext());
        } else {
            Log.d("DiaryDatabaseHelper", "No pages found.");
        }

        cursor.close();
        return pages;
    }


    public boolean pageExistsById(String pageId) {
        if (pageId == null) {
            Log.e("DatabaseHelper", "pageExistsById: pageId is NULL!");
            return false; // Return false to prevent crash
        }

        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM diary_pages WHERE id = ?";

        Cursor cursor = db.rawQuery(query, new String[]{pageId});
        boolean exists = false;

        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }

        cursor.close();
        db.close();

        return exists;
    }


    // Resource Management
    public void addResource(String pageId, String resourceUri) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAGE_ID, pageId);
        values.put("resourceUri", resourceUri);
        db.insert("resources", null, values);
        db.close();
    }

    @SuppressLint("Range")
    public ArrayList<String> getResourcesForPage(String pageId) {
        ArrayList<String> resourceUris = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("resources", new String[]{"resourceUri"}, COLUMN_PAGE_ID + "=?",
                new String[]{pageId}, null, null, null);
        while (cursor.moveToNext()) {
            resourceUris.add(cursor.getString(cursor.getColumnIndex("resourceUri")));
        }
        cursor.close();
        return resourceUris;
    }
    */

}
