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
import android.util.Pair;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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





        String tasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
                "taskId INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "taskTitle TEXT NOT NULL, " +
                "startTime TEXT NOT NULL, " +
                "endTime TEXT NOT NULL, " +
                "recurring TEXT NOT NULL DEFAULT 'NONE', " +
                "completion TEXT NOT NULL DEFAULT 'Pending'" +  // New column
                ");";

        db.execSQL(tasksTable);
        Log.d("Task","Task Table Created Successfully.");


        // Add indexes to optimize foreign key searches
        db.execSQL("CREATE INDEX idx_userId ON pages(userId);");
        db.execSQL("CREATE INDEX idx_pageId ON resources(pageId);");

        Log.d("Database", "All tables created successfully.");
//The taskId is passed as resourceContent in the resources table.
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


    //updatePage() done
    public int updatePage(int pageId, String title, List<Resource> resources, int userId) {
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

        // Fetch existing resource IDs and their resourceOrder values for this page
        Cursor cursor = db.rawQuery("SELECT resourceId, resourceOrder FROM resources WHERE pageId = ? ORDER BY resourceOrder ASC",
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

        // Insert or update resources
        for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);
            ContentValues values = new ContentValues();
            values.put("resourceContent", resource.getContent());
            values.put("resourceType", resource.getType());
            values.put("pageId", pageId);

            if (i < existingIds.size()) {
                // Update existing resource while keeping its original order
                values.put("resourceOrder", existingOrders.get(i));
                db.update("resources", values, "resourceId = ?", new String[]{String.valueOf(existingIds.get(i))});
            } else {
                // Insert new resource with a unique resourceOrder
                maxOrder++;
                values.put("resourceOrder", maxOrder);
                long newResourceId = db.insert("resources", null, values);
                if (newResourceId == -1) {
                    Log.e("DiaryDatabaseHelper", "Failed to insert resource for page ID: " + pageId);
                }
            }
        }

        // Remove extra old resources that are no longer needed
        if (existingIds.size() > resources.size()) {
            for (int i = resources.size(); i < existingIds.size(); i++) {
                db.delete("resources", "resourceId = ?", new String[]{String.valueOf(existingIds.get(i))});
            }
        }

        return pageId;
    }

    //getAllPages() done
    public ArrayList<DiaryPage> getAllPages(int userId) {
        ArrayList<DiaryPage> pages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get all pages for the user
        String pageQuery = "SELECT pageId, pageTitle, pageDate FROM pages WHERE userId = ? ORDER BY pageDate DESC";
        Cursor pageCursor = db.rawQuery(pageQuery, new String[]{String.valueOf(userId)});

        Log.d("DiaryDatabaseHelper", "Fetching pages for userId: " + userId);

        if (pageCursor.moveToFirst()) {
            do {
                @SuppressLint("Range") int id = pageCursor.getInt(pageCursor.getColumnIndex("pageId"));
                @SuppressLint("Range") String title = pageCursor.getString(pageCursor.getColumnIndex("pageTitle"));
                @SuppressLint("Range") String date = pageCursor.getString(pageCursor.getColumnIndex("pageDate"));

                // Fetch all resources for this page
                ArrayList<Resource> resources = new ArrayList<>();
                String resourceQuery = "SELECT resourceId, resourceType, resourceContent, resourceOrder " +
                        "FROM resources WHERE pageId = ? ORDER BY resourceOrder ASC";
                Cursor resourceCursor = db.rawQuery(resourceQuery, new String[]{String.valueOf(id)});

                while (resourceCursor.moveToNext()) {
                    @SuppressLint("Range") int resourceId = resourceCursor.getInt(resourceCursor.getColumnIndex("resourceId"));
                    @SuppressLint("Range") String type = resourceCursor.getString(resourceCursor.getColumnIndex("resourceType"));
                    @SuppressLint("Range") String content = resourceCursor.getString(resourceCursor.getColumnIndex("resourceContent"));
                    @SuppressLint("Range") int order = resourceCursor.getInt(resourceCursor.getColumnIndex("resourceOrder"));

                    resources.add(new Resource(resourceId, id, type, content, order));
                }
                resourceCursor.close();

                // Add page with all its resources
                pages.add(new DiaryPage(id, title, date, resources));

                Log.d("DiaryDatabaseHelper", "Page Loaded: ID=" + id + ", Title=" + title + ", Resources=" + resources.size());

            } while (pageCursor.moveToNext());
        } else {
            Log.d("DiaryDatabaseHelper", "No pages found for userId: " + userId);
        }

        pageCursor.close();
        return pages;
    }


    //Not related
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


    //Not related
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

    //getPageById() done
    public DiaryPage getPageById(int pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        DiaryPage diaryPage = null;
        Cursor pageCursor = null;
        Cursor resourceCursor = null;

        try {
            // Fetch page details
            pageCursor = db.rawQuery("SELECT pageId, pageTitle, pageDate FROM pages WHERE pageId = ?",
                    new String[]{String.valueOf(pageId)});

            if (pageCursor.moveToFirst()) {
                int id = pageCursor.getInt(pageCursor.getColumnIndexOrThrow("pageId"));
                String title = pageCursor.getString(pageCursor.getColumnIndexOrThrow("pageTitle"));
                String date = pageCursor.getString(pageCursor.getColumnIndexOrThrow("pageDate"));

                // Fetch all resources for the given page, ordered correctly
                ArrayList<Resource> resources = new ArrayList<>();
                resourceCursor = db.rawQuery("SELECT resourceId, resourceType, resourceContent, resourceOrder " +
                                "FROM resources WHERE pageId = ? ORDER BY resourceOrder ASC",
                        new String[]{String.valueOf(pageId)});

                while (resourceCursor.moveToNext()) {
                    int resourceId = resourceCursor.getInt(resourceCursor.getColumnIndexOrThrow("resourceId"));
                    String type = resourceCursor.getString(resourceCursor.getColumnIndexOrThrow("resourceType"));
                    String content = resourceCursor.getString(resourceCursor.getColumnIndexOrThrow("resourceContent"));
                    int order = resourceCursor.getInt(resourceCursor.getColumnIndexOrThrow("resourceOrder"));

                    resources.add(new Resource(resourceId, id, type, content, order));
                }

                // Create DiaryPage with all resources
                diaryPage = new DiaryPage(id, title, date, resources);
            }
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "Database error: " + e.getMessage());
        } finally {
            if (pageCursor != null) pageCursor.close();
            if (resourceCursor != null) resourceCursor.close();
            db.close();
        }

        return diaryPage;
    }



//insertResource() done
public long insertResource(int pageId, String resourceType, String resourceContent, int resourceOrder) {
    SQLiteDatabase db = this.getWritableDatabase();
    long result = -1; // Default to -1 (failure)

    try {
        ContentValues values = new ContentValues();
        values.put("pageId", pageId);
        values.put("resourceType", resourceType);
        values.put("resourceContent", resourceContent);
        values.put("resourceOrder", resourceOrder);

        result = db.insert("resources", null, values);
        if (result == -1) {
            Log.e("DB_ERROR", "Failed to insert resource for pageId: " + pageId);
        } else {
            Log.d("DB_SUCCESS", "Resource inserted successfully for pageId: " + pageId + ", order: " + resourceOrder);
        }
    } catch (SQLiteException e) {
        Log.e("DB_ERROR", "Database error while inserting resource: " + e.getMessage());
    } finally {
        db.close();
    }

    return result;  // ✅ Now it correctly returns the insert result
}


    //getResourcesByPageId() done
    public List<Pair<String, String>> getResourcesByPageId(int pageId) {
        List<Pair<String, String>> resources = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT resourceType, resourceContent FROM resources WHERE pageId = ? ORDER BY resourceOrder",
                new String[]{String.valueOf(pageId)});

        if (cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndexOrThrow("resourceType"));
                String content = cursor.getString(cursor.getColumnIndexOrThrow("resourceContent"));
                resources.add(new Pair<>(type, content));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return resources;
    }




    public int getLastResourceOrder(int pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(resourceOrder) FROM resources WHERE pageId = ?", new String[]{String.valueOf(pageId)});

        int lastOrder = 0; // Default to 0 if no resources exist
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            lastOrder = cursor.getInt(0);
        }
        cursor.close();
        return lastOrder;
    }


    public long insertTask(String taskTitle, String startTime, String endTime, String recurring, int pageId) {
        Log.d("Task", "Inserting Task...");
        SQLiteDatabase db = this.getWritableDatabase();

        // Insert task into tasks table
        ContentValues taskValues = new ContentValues();
        taskValues.put("taskTitle", taskTitle);
        taskValues.put("startTime", startTime);
        taskValues.put("endTime", endTime);
        taskValues.put("recurring", recurring);

        long taskId = db.insert("tasks", null, taskValues);

        if (taskId != -1) {
            // Get last resource order
            int lastOrder = getLastResourceOrder((int) pageId);
            Log.d("Task", "Last Resource Order: " + lastOrder);

            // Link task to resources table
            ContentValues resourceValues = new ContentValues();
            resourceValues.put("pageId", pageId);
            resourceValues.put("resourceType", "task");
            resourceValues.put("resourceContent", String.valueOf(taskId)); // Storing taskId
            resourceValues.put("resourceOrder", lastOrder + 1);

            long resourceId = db.insert("resources", null, resourceValues);
            Log.d("Task", "Task linked to resources table with resource ID: " + resourceId);
        }

//        debugResourcesTable("Task", pageId);
        Log.d("Task","Task inserted successfully with ID: " + taskId);
        debugResourcesTable("Task",pageId);
        return taskId;
    }


    // adding task to resources Table
    public void debugResourcesTable(String checkName, int pageId) {
        Log.d(checkName, "DebugResourceTable() Called In Diary Database.");
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM RESOURCES WHERE pageId = ?", new String[]{String.valueOf(pageId)});


        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String content = cursor.getString(1);
            String type = cursor.getString(2);
            int order = cursor.getInt(3);
            int retrievedPageId = cursor.getInt(4);  // Fix: Use a separate variable

            Log.d(checkName, "ID: " + id + ", PageID: " + retrievedPageId + ", Type: " + type + ", Content: " + content + ", Order: " + order);
        }

        cursor.close();
    }

    public void updateTaskCompletion(int taskId, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("completion", status);  // Store "Completed" or "Pending"

        int rowsAffected = db.update("tasks", values, "taskId = ?", new String[]{String.valueOf(taskId)});
        db.close();

        if (rowsAffected == 0) {
            Log.e("DB_ERROR", "Failed to update completion status for taskId: " + taskId);
        }
    }


    public Task getTaskById(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Task task = null;

        Cursor cursor = db.rawQuery("SELECT * FROM tasks WHERE taskId = ?", new String[]{String.valueOf(taskId)});

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow("taskTitle"));
            String startTime = cursor.getString(cursor.getColumnIndexOrThrow("startTime"));
            String endTime = cursor.getString(cursor.getColumnIndexOrThrow("endTime"));
            String recurring = cursor.getString(cursor.getColumnIndexOrThrow("recurring"));
            String completion = cursor.getString(cursor.getColumnIndexOrThrow("completion")); // Ensure you added this column

            task = new Task(taskId, title, startTime, endTime, recurring, completion);
            cursor.close();
        }

        return task;
    }

    public void cleanUpTasks() {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete tasks that are NOT referenced in the resources table
        String deleteQuery = "DELETE FROM tasks WHERE taskId NOT IN (SELECT resourceContent FROM resources WHERE resourceType = 'task')";

        db.execSQL(deleteQuery);
        Log.d("Database", "🗑 Cleaned up unlinked tasks.");
    }


    public List<DiaryPage> getAllDiaryPages(int userId) {
        List<DiaryPage> pageList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT pageId, pageTitle FROM pages WHERE userId = ?", new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int pageId = cursor.getInt(0);
                String title = cursor.getString(1);
                pageList.add(new DiaryPage(pageId, title));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return pageList;
    }

    public List<Task> getAllTasks(int userId) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT t.taskId, t.taskTitle, t.startTime, t.endTime, t.recurring, t.completion " +
                "FROM tasks t " +
                "JOIN resources r ON r.resourceContent = t.taskId " +
                "JOIN pages p ON p.pageId = r.pageId " +
                "WHERE p.userId = ? " +
                "ORDER BY t.taskId";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int taskId = cursor.getInt(cursor.getColumnIndexOrThrow("taskId"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("taskTitle"));
                String startTime = cursor.getString(cursor.getColumnIndexOrThrow("startTime"));
                String endTime = cursor.getString(cursor.getColumnIndexOrThrow("endTime"));
                String recurring = cursor.getString(cursor.getColumnIndexOrThrow("recurring"));
                String completion = cursor.getString(cursor.getColumnIndexOrThrow("completion"));

                // Convert 'recurring' column to a boolean (assuming 'NONE' means false)
                String isRecurring = String.valueOf(!recurring.equalsIgnoreCase("NONE"));

                // Create a Task object and add it to the list
                Task task = new Task(taskId, title, startTime, endTime, isRecurring, completion);
                taskList.add(task);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return taskList;
    }


    public int getPageIdByTaskId(int taskId) {
        int pageId = -1; // Default value if not found
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT pageId FROM resources WHERE resourceContent = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(taskId)});

        if (cursor.moveToFirst()) {
            pageId = cursor.getInt(0); // Get pageId from the first column
        }

        cursor.close();
        db.close();
        return pageId;
    }


    public List<Task> getTasksSortedByDate() {
        return null;
    }

    public List<Task> getTasksByDate(String selectedDate) {
        List<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("DB_DEBUG", "Selected Date: " + selectedDate);

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = inputFormat.parse(selectedDate);
            selectedDate = outputFormat.format(date); // Converts "2025-3-5" -> "2025-03-05"
        } catch (ParseException e) {
            e.printStackTrace();
        }



        // Extract only the date part from startTime and endTime

// Step 1: Get task IDs that match the selectedDate
        Cursor idCursor = db.rawQuery(
                "SELECT taskId FROM tasks WHERE startTime LIKE ? OR endTime LIKE ? OR (? BETWEEN startTime AND endTime)",
                new String[]{selectedDate + "%", selectedDate + "%", selectedDate}
        );

        Log.d("DB_DEBUG", "Query: SELECT taskId FROM tasks WHERE startTime LIKE '" + selectedDate + "%' OR endTime LIKE '" + selectedDate + "%' OR ('" + selectedDate + "' BETWEEN startTime AND endTime)");

























        List<Integer> taskIds = new ArrayList<>();
        if (idCursor.moveToFirst()) {
            do {
                int taskId = idCursor.getInt(0);
                taskIds.add(taskId);
                Log.d("DB_FETCH", "Matching Task ID: " + taskId);
            } while (idCursor.moveToNext());
        }
        idCursor.close();

// Step 2: Fetch full task details for the matching task IDs
        for (int taskId : taskIds) {
            Cursor taskCursor = db.rawQuery(
                    "SELECT * FROM tasks WHERE taskId = ?",
                    new String[]{String.valueOf(taskId)}
            );

            if (taskCursor.moveToFirst()) {
                do {
                    Task task = new Task(
                            taskCursor.getInt(0), // Task ID
                            taskCursor.getString(1), // Title
                            taskCursor.getString(2), // Description
                            taskCursor.getString(3), // Start Date
                            taskCursor.getString(4), // End Date
                            taskCursor.getString(5)  // Status
                    );
                    taskList.add(task);
                } while (taskCursor.moveToNext());
            }
            taskCursor.close();
        }

        db.close();
        return taskList;
    }


    public int getSwotPageId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int pageId = -1;

        Cursor cursor = db.rawQuery("SELECT pageId FROM pages WHERE userId = ? AND pageTitle = 'SWOT' LIMIT 1",
                new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            pageId = cursor.getInt(0);
        }
        cursor.close();
        return pageId; // Returns -1 if no SWOT page exists
    }


    public Map<String, String> getSwotData(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Map<String, String> swotData = new HashMap<>();

        String query = "SELECT r.resourceType, r.resourceContent FROM resources r " +
                "JOIN pages p ON r.pageId = p.pageId " +
                "WHERE p.userId = ? AND p.pageTitle = 'SWOT'";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0); // "SWOT:Strength", etc.
                String content = cursor.getString(1);
                swotData.put(type, content);
            }
            cursor.close();
        }

        return swotData;
    }


    public void logSwotResources(int pageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT resourceType, resourContent FROM resources WHERE pageId = ?", new String[]{String.valueOf(pageId)});

        Log.d("DiaryDatabaseHelper", "Checking saved SWOT data for pageId: " + pageId);

        if (cursor.moveToFirst()) {
            do {
                String resourceType = cursor.getString(0);
                String content = cursor.getString(1);
                Log.d("DiaryDatabaseHelper", "SWOT Resource -> Type: " + resourceType + ", Content: " + content);
            } while (cursor.moveToNext());
        } else {
            Log.d("DiaryDatabaseHelper", "No SWOT data found for pageId: " + pageId);
        }
        cursor.close();
        db.close();
    }



}



