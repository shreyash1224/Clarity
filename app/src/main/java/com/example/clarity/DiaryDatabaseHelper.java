package com.example.clarity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

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
                + "userDate DATETIME DEFAULT (datetime('now', 'localtime')),"
                + "userName TEXT NOT NULL CHECK (LENGTH(userName) > 0 AND LENGTH(userName) <= 50) UNIQUE, "
                + "userPassword TEXT NOT NULL CHECK (LENGTH(userPassword) >= 6));";

        db.execSQL(userTable);

        String pageTable = "CREATE TABLE IF NOT EXISTS pages("
                + "pageId INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "pageTitle TEXT NOT NULL CHECK (LENGTH(pageTitle) > 0 AND LENGTH(pageTitle) <= 100), "
                + "pageDate DATETIME DEFAULT (datetime('now', 'localtime')), "
                + "userId INTEGER NOT NULL, "
                + "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE);";

        db.execSQL(pageTable);

        String resourceTable = "CREATE TABLE IF NOT EXISTS resources("
                + "resourceId INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "resourceContent TEXT NOT NULL CHECK (LENGTH(resourceContent) > 0), "
                + "resourceType TEXT NOT NULL CHECK (LENGTH(resourceType) <= 20), "
                + "resourceOrder INTEGER NOT NULL, "
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
    public long addUser(String username, String userPassword) {
        int userId = getNextAvailableId("users", "userId");

        Log.d("DatabaseHelper", "Adding user: userId=" + userId + ", username=" + username + ", userPassword=" + userPassword);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("userPassword", userPassword);
        values.put("username", username);
        long result =  db.insert("users", null, values);
        db.close();
        Log.d("Databasae","User Added. " +"Result: "+result);
        return result;



    }


    public boolean authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("users", null, "username" + "=? AND " + "userPassword" + "=?",
                new String[]{username, password}, null, null, null);

        boolean authenticated = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return authenticated;
    }

    public boolean isUsernameTaken(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    //Adding page to Diary.

    public int updatePage(int pageId, String title, ArrayList<String> textBlocks, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (pageId == -1) {
            // Insert new page
            ContentValues pageValues = new ContentValues();
            pageValues.put("pageTitle", title);
            pageValues.put("userId", userId);

            long newPageId = db.insert("pages", null, pageValues);
            if (newPageId == -1) {
                Log.e("DiaryDatabaseHelper", "Failed to insert new page.");
                return -1;
            }
            pageId = (int) newPageId;
            Log.d("DiaryDatabaseHelper", "Page added successfully with ID: " + pageId);
        } else {
            // Update existing page
            ContentValues pageValues = new ContentValues();
            pageValues.put("pageTitle", title);
            db.update("pages", pageValues, "pageId = ?", new String[]{String.valueOf(pageId)});
            Log.d("DiaryDatabaseHelper", "Page updated successfully with ID: " + pageId);
        }

        // Fetch existing text block IDs and their resourceOrder values for this page
        Cursor cursor = db.rawQuery("SELECT resourceId, resourceOrder FROM resources WHERE pageId = ? AND resourceType = 'text' ORDER BY resourceOrder ASC",
                new String[]{String.valueOf(pageId)});

        ArrayList<Integer> existingIds = new ArrayList<>();
        ArrayList<Integer> existingOrders = new ArrayList<>();

        while (cursor.moveToNext()) {
            existingIds.add(cursor.getInt(0)); // resourceId
            existingOrders.add(cursor.getInt(1)); // resourceOrder
        }
        cursor.close();

        // Get the current max resourceOrder for this page
        int maxOrder = 0;
        Cursor orderCursor = db.rawQuery("SELECT MAX(resourceOrder) FROM resources WHERE pageId = ?",
                new String[]{String.valueOf(pageId)});
        if (orderCursor.moveToFirst()) {
            maxOrder = orderCursor.getInt(0);
        }
        orderCursor.close();

        // Now insert or update text blocks with a unique resourceOrder
        for (int i = 0; i < textBlocks.size(); i++) {
            ContentValues values = new ContentValues();
            values.put("resourceContent", textBlocks.get(i));

            if (i < existingIds.size()) {
                // Update existing text block while keeping its original order
                values.put("resourceOrder", existingOrders.get(i));
                db.update("resources", values, "resourceId = ?", new String[]{String.valueOf(existingIds.get(i))});
            } else {
                // Insert new text block with a unique resourceOrder
                maxOrder++; // Ensure unique order for new insertions
                values.put("resourceOrder", maxOrder);
                values.put("resourceType", "text");
                values.put("pageId", pageId);
                long newResourceId = db.insert("resources", null, values);
                if (newResourceId == -1) {
                    Log.e("DiaryDatabaseHelper", "Failed to insert text block for page ID: " + pageId);
                }
            }
        }

        // Remove extra old text blocks that are no longer needed
        if (existingIds.size() > textBlocks.size()) {
            for (int i = textBlocks.size(); i < existingIds.size(); i++) {
                db.delete("resources", "resourceId = ?", new String[]{String.valueOf(existingIds.get(i))});
            }
        }

        return pageId;
    }


    // Getting all the pages
    public ArrayList<DiaryPage> getAllPages(int userId) {
        ArrayList<DiaryPage> pages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get pages along with their content
        String query = "SELECT p.pageId, p.pageTitle, p.pageDate, " +
                "(SELECT r.resourceContent FROM resources r WHERE r.pageId = p.pageId LIMIT 1) AS content " +
                "FROM pages p WHERE p.userId = ? ORDER BY p.pageDate DESC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        Log.d("DiaryDatabaseHelper", "Fetching pages for userId: " + userId);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("pageId"));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex("pageTitle"));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex("pageDate"));
                @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex("content"));

                Log.d("DiaryDatabaseHelper", "Page Loaded: ID=" + id + ", Title=" + title + ", Content=" + content);

                pages.add(new DiaryPage(id, title, date, content)); // Use updated constructor
            } while (cursor.moveToNext());
        } else {
            Log.d("DiaryDatabaseHelper", "No pages found for userId: " + userId);
        }

        cursor.close();
        return pages;

    }


    //To get next available id as per column name.
    public int getNextAvailableId(String tableName, String idColumn) {
        SQLiteDatabase db = this.getReadableDatabase();
        int newId = 1; // Default to 1 if the table is empty

        Cursor cursor = db.rawQuery(
                "SELECT MIN(t1." + idColumn + " + 1) AS newId " +
                        "FROM " + tableName + " t1 " +
                        "LEFT JOIN " + tableName + " t2 " +
                        "ON t1." + idColumn + " + 1 = t2." + idColumn + " " +
                        "WHERE t2." + idColumn + " IS NULL", null);

        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            newId = cursor.getInt(0);
        }
        cursor.close();
        return newId;
    }


    public int getUserIdByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        int userId = -1; // Default value if user is not found

        Cursor cursor = db.rawQuery("SELECT userId FROM users WHERE username = ?", new String[]{username});
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0);
        }
        cursor.close();
        db.close();

        return userId;
    }

    public DiaryPage getPageById(int pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        DiaryPage diaryPage = null;
        Cursor cursor = null;

        try {
            // Fetch page details
            cursor = db.rawQuery("SELECT p.pageId, p.pageTitle, p.pageDate, " +
                            "(SELECT GROUP_CONCAT(r.resourceContent, '\n') FROM resources r WHERE r.pageId = p.pageId) AS content " +
                            "FROM pages p WHERE p.pageId = ?",
                    new String[]{String.valueOf(pageId)});

            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("pageId"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("pageTitle"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("pageDate"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("content")); // Fetch content

                diaryPage = new DiaryPage(id, title, date, content);
            }
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return diaryPage;
    }

    public int getNextResourceOrder(int pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(resourceOrder) FROM resources WHERE pageId = ?",
                new String[]{String.valueOf(pageId)});
        int nextOrder = 1;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            nextOrder = cursor.getInt(0) + 1;
        }
        cursor.close();
        return nextOrder;
    }

    public void deleteTextBlocksByPageId(int pageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete("resources", "pageId = ? AND resourceType = ?",
                    new String[]{String.valueOf(pageId), "text"});
            db.setTransactionSuccessful();
            Log.d("DiaryDatabaseHelper", "Deleted text blocks for pageId: " + pageId);
        } catch (SQLiteException e) {
            Log.e("DiaryDatabaseHelper", "Error deleting text blocks: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void insertTextBlock(int pageId, String textContent, int resourceOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("pageId", pageId);
            values.put("resourceType", "text");  // Storing as a text resource
            values.put("resourceContent", textContent);
            values.put("resourceOrder", resourceOrder);

            long result = db.insert("resources", null, values);
            if (result == -1) {
                Log.e("DiaryDatabaseHelper", "Failed to insert text block for pageId: " + pageId);
            } else {
                Log.d("DiaryDatabaseHelper", "Inserted text block for pageId: " + pageId);
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e("DiaryDatabaseHelper", "Error inserting text block: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    public List<String> getTextBlocksByPageId(int pageId) {
        List<String> textBlocks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        try {
            String query = "SELECT resourceContent FROM resources WHERE pageId = ? AND resourceType = 'text' ORDER BY resourceOrder ASC";
            cursor = db.rawQuery(query, new String[]{String.valueOf(pageId)});

            while (cursor.moveToNext()) {
                textBlocks.add(cursor.getString(0)); // Get text content
            }


        } catch (SQLiteException e) {
            Log.e("DiaryDatabaseHelper", "Error fetching text blocks: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return textBlocks;
    }


    public void insertResource(int pageId, String resourceType, String resourceContent, int resourceOrder) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("pageId", pageId);
        values.put("resourceType", resourceType);
        values.put("resourceContent", resourceContent);
        values.put("resourceOrder", resourceOrder);

        db.insert("resources", null, values);


        db.close();
    }






    public List<String> getImagePathsByPageId(int pageId) {
        List<String> imagePaths = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

//        Cursor cursor = db.rawQuery("SELECT * FROM resources ORDER BY resourceOrder", null);

        Cursor cursor = db.rawQuery("SELECT resourceContent FROM RESOURCES WHERE pageId = ? AND resourceType = 'image'",
                new String[]{String.valueOf(pageId)});

        Log.d("DiaryDatabaseHelper", "🔍 Checking images for pageId: " + pageId);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(0);
                Log.d("DiaryDatabaseHelper", "✅ Retrieved image path: " + path);
                imagePaths.add(path);
            }
            cursor.close();
        } else {
            Log.e("DiaryDatabaseHelper", "❌ Cursor is null while fetching images for pageId: " + pageId);
        }

        return imagePaths;
    }



    public void debugResourcesTable() {
        Log.d("DatabaseCheck", "DebugResourceTable() Called In Diary Database.");
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Resources", null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String content = cursor.getString(1);
            String type = cursor.getString(2);
            int order = cursor.getInt(3);
            int pageId = cursor.getInt(4);

            Log.d("DatabaseCheck", "ID: " + id + ", PageID: " + pageId + ", Type: " + type + ", Content: " + content + ", Order: " + order);
        }
        cursor.close();
    }

}

