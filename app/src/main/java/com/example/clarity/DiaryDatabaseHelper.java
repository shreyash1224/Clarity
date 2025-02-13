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

//    + "resourceId INTEGER PRIMARY KEY AUTOINCREMENT, "
//            + "resourceContent TEXT NOT NULL CHECK (LENGTH(resourceContent) > 0), "
//            + "resourceType TEXT NOT NULL CHECK (LENGTH(resourceType) <= 20), "
//            + "pageId INTEGER NOT NULL, "
//            + "FOREIGN KEY (pageId) REFERENCES pages(pageId) ON DELETE CASCADE);";

//
//    + "pageId INTEGER PRIMARY KEY AUTOINCREMENT, "
//            + "pageTitle TEXT NOT NULL CHECK (LENGTH(pageTitle) > 0 AND LENGTH(pageTitle) <= 100), "
//            + "pageDate DATETIME DEFAULT (datetime('now', 'localtime')), "
//            + "userId INTEGER NOT NULL, "
//            + "FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE);";


    //Adding page to Diary.
    public int updatePage(Integer pageId, String title, String content, int userId, String resourceType) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (pageId == null) {
            // Insert new page
            ContentValues pageValues = new ContentValues();
            pageValues.put("pageTitle", title);
            pageValues.put("userId", userId);

            long newPageId = db.insert("pages", null, pageValues);
            if (newPageId == -1) {
                Log.e("DiaryDatabaseHelper", "Failed to insert new page.");
                return Integer.parseInt(null);
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

        // Insert or update resource
        if (content != null && !content.isEmpty() && resourceType != null && !resourceType.isEmpty()) {
            ContentValues resourceValues = new ContentValues();
            resourceValues.put("resourceContent", content);
            resourceValues.put("resourceType", resourceType);
            resourceValues.put("pageId", pageId);

            int rowsUpdated = db.update("resources", resourceValues, "pageId = ?", new String[]{String.valueOf(pageId)});
            if (rowsUpdated == 0) {
                // Insert if no resource exists
                long newResourceId = db.insert("resources", null, resourceValues);
                if (newResourceId == -1) {
                    Log.e("DiaryDatabaseHelper", "Failed to insert resource for page ID: " + pageId);
                } else {
                    Log.d("DiaryDatabaseHelper", "Resource added successfully with ID: " + newResourceId);
                }
            } else {
                Log.d("DiaryDatabaseHelper", "Resource updated successfully for page ID: " + pageId);
            }
        }

        return pageId;
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
    public void addResource(String pageId, String resourceContent) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PAGE_ID, pageId);
        values.put("resourceContent", resourceContent);
        db.insert("resources", null, values);
        db.close();
    }

    @SuppressLint("Range")
    public ArrayList<String> getResourcesForPage(String pageId) {
        ArrayList<String> resourceContents = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("resources", new String[]{"resourceContent"}, COLUMN_PAGE_ID + "=?",
                new String[]{pageId}, null, null, null);
        while (cursor.moveToNext()) {
            resourceContents.add(cursor.getString(cursor.getColumnIndex("resourceContent")));
        }
        cursor.close();
        return resourceContents;
    }
    */


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
//    public int getPageIdByTitle(String pageTitle) {
//        SQLiteDatabase db = this.getReadableDatabase();
//        int pageId = -1; // Default value if page is not found
//
//        Cursor cursor = db.rawQuery("SELECT pageId FROM pages WHERE pageTitle = ?", new String[]{pageTitle});
//        if (cursor.moveToFirst()) {
//            pageId = cursor.getInt(0);
//        }
//        cursor.close();
//        db.close();
//
//        return pageId;
//    }

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

}
