package com.example.clarity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.FileOutputStream;
import java.util.Stack;

public class DiaryPageActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    private EditText editTitle;
    private DiaryDatabaseHelper dbHelper;
    private int pageId = -1;
    private LinearLayout contentLayout;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;
    private Stack<Action> undoStack = new Stack<>();
    private Stack<Action> redoStack = new Stack<>();
    private Task pendingTask = null;
    List<Task> pendingTasks = new ArrayList<>();




    @Override
    //onCreate() done
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_diary_page);
        TaskNotificationManager.createNotificationChannel(this);


        dbHelper = new DiaryDatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        Log.d("DatabaseCheck", "Diary Page Activity onCreate() called.");

        editTitle = findViewById(R.id.etDpaTitle);
        contentLayout = findViewById(R.id.llDpaContentLayout);

        // ✅ Request storage permission before loading data


        // ✅ Load the page data only after permissions are granted
        Intent intent = getIntent();
        pageId = intent.getIntExtra("pageId", -1);

        if (pageId != -1) {
            loadPageData(pageId);
        }
    }


    //loadPageData() done
    private void loadPageData(int pageId) {
        DiaryPage page = dbHelper.getPageById(pageId);
        if (page != null) {
            editTitle.setText(page.getPageTitle());

            List<Pair<String, String>> resources = dbHelper.getResourcesByPageId(pageId);

            for (Pair<String, String> resource : resources) {
                String type = resource.first;
                String content = resource.second;

                if (type.equals("text")) {
                    addTextBlockToUI(content);
                } else if (type.equals("image")) {
                    // Step 1: Log the retrieved image path for debugging
                    Log.d("Debug", "loadPageData()->Image Path: " + content);

                    // Step 2: Create an Image object
                    Image image = new Image(content);

                    // Step 3: Pass the URI stored in Image object to insertImage()
                    insertImage(Uri.parse(image.getImageUri()));

                } else if (type.equals("task")) {
                    Log.d("Task", "LoadPage: Adding task block to UI.");

                    // Convert content (which stores taskId) to an integer
                    int taskId = Integer.parseInt(content);

                    // Fetch the task using the correct taskId
                    Task task = dbHelper.getTaskById(taskId);

                    // If task exists, add it to the UI
                    if (task != null) {
                        addTaskBlock(task);
                    } else {
                        Log.e("Task", "Failed to load page/task with ID: " + taskId);
                    }
                } else if (type.equals("page")) {
                    Log.d("Page", "LoadPage: Adding page block to UI.");

                    int pId = Integer.parseInt(content);
                    DiaryPage diaryPage = dbHelper.getPageById(pId);

                    addPageBlock(diaryPage);
                }
            }

        } else {
            Toast.makeText(this, "Failed to load page.", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        debugResourcesTable("Database");
        Log.d("LIFECYCLE", "onPause() called");

        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Log.e("onPause", "⚠️ Title is empty! Not saving to database.");
            return;
        }

        if (title.length() > 100) {
            Log.e("onPause", "⚠️ Title exceeds 100 characters! Not saving.");
            Toast.makeText(this, "Title should be 1-100 characters long", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Resource> contentBlocks = new ArrayList<>();
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        // Step 1: Retrieve resources from UI in correct order
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);

            Object tag = view.getTag();
            if (view instanceof LinearLayout) {
                EditText editText = view.findViewById(R.id.etTextBlock);

                if (editText != null) {
                    String text = editText.getText().toString().trim();
                    if (!text.isEmpty()) {
                        contentBlocks.add(new Resource(pageId, "text", text, contentBlocks.size() + 1));
                        Log.d("onPause", "📌 Saved Text Block: " + text);
                    }
                }
            }
            else if (view instanceof FrameLayout) {
                if (tag instanceof Image) {
                    Image image = (Image) tag;
                    String imagePath = image.getImageUri();
                    contentBlocks.add(new Resource(pageId, "image", imagePath, contentBlocks.size() + 1));
                } else if (tag instanceof Task) {
                    Task task = (Task) tag;
                    contentBlocks.add(new Resource(pageId, "task", String.valueOf(task.getTaskId()), contentBlocks.size() + 1));

                    // ✅ Trigger Notification if Task is Pending
                    if (task.getCompletion().equals("Pending")) {
                        if (PermissionManager.hasNotificationPermission(this)) {
                            // ✅ Directly trigger notification if permission is already granted
                            TaskNotificationManager.scheduleTaskNotification(this, task);
                        } else {
                            // ✅ Request permission if not granted (Notification will trigger later)
                            pendingTask = task;
                        }
                    } else {
                        // 🛑 Only cancel the notification if the task was previously "Pending" and is now "Completed"
                        Task oldTask = dbHelper.getTaskById(task.getTaskId());
                        if (oldTask != null && oldTask.getCompletion().equals("Pending") && task.getCompletion().equals("Completed")) {
                            TaskNotificationManager.cancelNotification(this, task.getTaskId());
                            Log.d("onPause", "❌ Task completed, notification canceled: " + task.getTaskTitle());
                        }
                    }


                    Log.d("onPause", "✅ Task Block Detected & Saved: " + task.getTaskTitle());
                } else if (tag instanceof DiaryPage) {
                    DiaryPage page = (DiaryPage) tag;
                    contentBlocks.add(new Resource(pageId, "page", String.valueOf(page.getPageId()), contentBlocks.size() + 1));
                }
            }
        }

        int userId = sharedPreferences.getInt("userId", -1);

        // Step 2: Save the ordered resources to the database
        if (!title.isEmpty() || !contentBlocks.isEmpty()) {
            if (pageId == -1) {
                pageId = dbHelper.updatePage(-1, title, contentBlocks, userId);
                Toast.makeText(this, "New Page Created", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.updatePage(pageId, title, contentBlocks, userId);
                Toast.makeText(this, "Page Updated", Toast.LENGTH_SHORT).show();
            }
        }

        dbHelper.cleanUpTasks();
        debugResourcesTable("pageResource");
    }





    //deletePage() done
    public void deletePage(View view) {
        if (pageId == -1) {
            Toast.makeText(this, "No page to delete!", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            // 1. Move associated images to trash (physically delete them)
            Cursor cursor = db.rawQuery("SELECT resourceContent FROM resources WHERE pageId = ? AND resourceType = 'image'",
                    new String[]{String.valueOf(pageId)});

            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(0);
                if (imagePath != null) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }
                }
            }
            cursor.close();

            // 2. Update the page status to 'trashed'
            ContentValues values = new ContentValues();
            values.put("pageStatus", "trashed");

            int rowsAffected = db.update("pages", values, "pageId = ?", new String[]{String.valueOf(pageId)});

            if (rowsAffected > 0) {
                Toast.makeText(this, "Page moved to Trash", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Page not found!", Toast.LENGTH_SHORT).show();
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e("DiaryPageActivity", "Error moving page to trash: " + e.getMessage());
            Toast.makeText(this, "Error moving page to Trash!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }



    private void addNewTextBlock() {
        // Avoid adding unnecessary empty blocks
        if (hasEmptyTextBlock()) {
            return;
        }

        // Inflate the text block layout
        View textBlock = getLayoutInflater().inflate(R.layout.text_block, contentLayout, false);

        // Find the EditText inside the new text block
        EditText newEditText = textBlock.findViewById(R.id.etTextBlock);
        newEditText.requestFocus(); // Move focus to the new EditText

        // Show the keyboard automatically
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
        }

        // Add the new text block to the layout
        contentLayout.addView(textBlock);

        undoStack.push(new Action(Action.ActionType.ADD, textBlock, contentLayout.getChildCount() - 1));
        redoStack.clear(); // Reset redo stack

        newEditText.setFocusableInTouchMode(true);
        newEditText.requestFocus();
    }


    //hasEmptyTextBlock() done
    private boolean hasEmptyTextBlock() {
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View child = contentLayout.getChildAt(i);
            if (child instanceof EditText) {
                String text = ((EditText) child).getText().toString().trim();
                if (text.isEmpty()) {
                    return true; // Found an empty text block
                }
            }
        }
        return false;
    }

    //onResourceTextClick() done
    public void onResourceTextClick(View view) {
        addNewTextBlock();
    }








    
//    addTextBlockToUI() done
private void addTextBlockToUI(String textContent) {
    LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

    // Prevent adding unnecessary empty blocks
    if (textContent.isEmpty() && hasEmptyTextBlock()) {
        return;
    }

    // Inflate the text block layout (text_block.xml)
    View textBlock = getLayoutInflater().inflate(R.layout.text_block, contentLayout, false);

    // Get the EditText inside the layout
    EditText newEditText = textBlock.findViewById(R.id.etTextBlock);
    newEditText.setText(textContent);
    newEditText.setSelection(newEditText.getText().length());

    // Add the complete text block layout to the content area
    contentLayout.addView(textBlock);

    // Ensure the EditText is focusable and request focus
    newEditText.setFocusableInTouchMode(true);
    newEditText.requestFocus();

    // Delay showing the keyboard to ensure proper focus
    newEditText.postDelayed(() -> {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }, 200);
}

    //openImagePicker() done
    //Image Trace 2
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    //onResourceImageClick() done
    //Image Trace 1
    public void onResourceImageClick(View view) {
        if (PermissionManager.hasStoragePermission(this)) {
            openImagePicker();
        } else {
            PermissionManager.requestStoragePermission(this);
        }
        Toast.makeText(this, "Adding Image clicked.", Toast.LENGTH_SHORT).show();
    }

    //onActivityResult() done
    //Image Trace 3
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            if (imageUri != null) {
                String imageUriString = imageUri.toString();

                // Get the last resource order for this page
                int lastOrder = dbHelper.getLastResourceOrder(pageId);
                int newOrder = lastOrder + 1; // Increment for new image

                // Insert into database with the correct resourceOrder
                long resourceId = dbHelper.insertResource(pageId, "image", imageUriString, newOrder);

                if (resourceId != -1) {
                    insertImage(imageUri);  // Display Image
                } else {
                    Log.e("DiaryPageActivity", "Failed to save image URI in database.");
                    Toast.makeText(this, "Failed to add image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }



    private void insertImage(Uri imageUri) {
        EditText focusedEditText = getCurrentFocusedEditText();

        // Inflate the image block layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View imageBlock = inflater.inflate(R.layout.image_block, contentLayout, false);
        ImageView imageView = imageBlock.findViewById(R.id.imageView);

        Image image = new Image(String.valueOf(imageUri));
        imageBlock.setTag(image);


        // Set image in ImageView
        imageView.setImageURI(imageUri);
        imageView.setOnClickListener(v -> openImageFullScreen(imageUri));


        // Calculate max height based on 8 lines of text
        int maxHeight = (focusedEditText != null)
                ? focusedEditText.getLineHeight() * 8  // 8 lines of text height
                : 600; // Fallback max height

        // Adjust ImageView dimensions while maintaining aspect ratio
        imageView.post(() -> {
            Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                int originalWidth = drawable.getIntrinsicWidth();
                int originalHeight = drawable.getIntrinsicHeight();

                // Calculate new width to maintain aspect ratio
                int newWidth = (int) (((float) originalWidth / originalHeight) * maxHeight);

                // Apply layout parameters for FrameLayout
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, // Width fills parent
                        maxHeight // Height limited to 8 lines
                );

                layoutParams.setMargins(0, 8, 0, 8); // Minimal margins
                imageView.setLayoutParams(layoutParams);
            }
        });

        if (focusedEditText != null) {
            int cursorPosition = focusedEditText.getSelectionStart();
            String textBefore = focusedEditText.getText().toString().substring(0, cursorPosition);
            String textAfter = focusedEditText.getText().toString().substring(cursorPosition);

            // Remove current EditText and replace with split blocks
            contentLayout.removeView(focusedEditText);

            // Create new EditText for text before cursor
            if (!textBefore.isEmpty()) {
                EditText newEditTextBefore = createEditText();
                newEditTextBefore.setText(textBefore);
                contentLayout.addView(newEditTextBefore);
            }

            // Insert image block
            contentLayout.addView(imageBlock);

            // Create new EditText for text after cursor
            if (!textAfter.isEmpty()) {
                EditText newEditTextAfter = createEditText();
                newEditTextAfter.setText(textAfter);
                contentLayout.addView(newEditTextAfter);
                newEditTextAfter.requestFocus(); // Move focus to new EditText
            }
        } else {
            // If no text block exists, add the image at the bottom
            contentLayout.addView(imageBlock);
        }

        // Store action for undo
        undoStack.push(new Action(Action.ActionType.ADD, imageBlock, contentLayout.getChildCount() - 1));
        redoStack.clear();
    }

    // Helper function to create a new EditText
    private EditText createEditText() {
        EditText editText = new EditText(this);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        editText.setTextSize(16);
        editText.setPadding(8, 8, 8, 8);
        return editText;
    }

    //getCurrentFocusedEditText() done
    private EditText getCurrentFocusedEditText() {
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);
            if (view instanceof EditText && view.hasFocus()) {
                return (EditText) view;
            }
        }

        // If no EditText is focused, use the last one
        for (int i = contentLayout.getChildCount() - 1; i >= 0; i--) {
            View view = contentLayout.getChildAt(i);
            if (view instanceof EditText) {
                return (EditText) view;
            }
        }

        // If no EditText exists, create a new one
        return null;

    }

    public void debugResourcesTable(String checkName) {
        Log.d(checkName, "DebugResourceTable() Called In Diary Database.");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Resources", null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String content = cursor.getString(1);
            String type = cursor.getString(2);
            int order = cursor.getInt(3);
            int pageId = cursor.getInt(4);

            Log.d(checkName, "ID: " + id + ", PageID: " + pageId + ", Type: " + type + ", Content: " + content + ", Order: " + order);
        }
        cursor.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Debug", "🔄 onResume() called. Checking ImageView tags...");

        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);
            if (view instanceof ImageView) {
                String imagePath = (String) view.getTag();
                Log.d("Debug", "🔍 ImageView Tag on Resume: " + imagePath);
            }
        }
    }




    public void onResourceTaskClick(View view) {
        Toast.makeText(this, "Adding a task.", Toast.LENGTH_SHORT).show();
        TaskDialog.showTaskDialog(this, (title, startTime, endTime, recurring) -> {
            // Save task details to the database

            long taskId = dbHelper.insertTask(title, startTime, endTime, String.valueOf(recurring), pageId);

            if (taskId != -1) {
                // Load the task block in UI
                Task task = dbHelper.getTaskById((int) taskId);
                if (task != null) {
                    addTaskBlock(task);
                }
            }
        });
    }


    private void addTaskBlock(Task task) {
        PermissionManager.requestNotificationPermission(this);
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        // Inflate Task Block XML properly
        View taskView = getLayoutInflater().inflate(R.layout.task_block, contentLayout, false);

        // Get UI Elements
        TextView title = taskView.findViewById(R.id.taskTitle);
        TextView time = taskView.findViewById(R.id.taskTime);
        TextView recurring = taskView.findViewById(R.id.taskRecurring);
        CheckBox completion = taskView.findViewById(R.id.taskCompletion);

        taskView.setTag(task);

        // Set Task Data
        title.setText(task.getTaskTitle());
        time.setText(task.getStartTime() + " - " + task.getEndTime());
        recurring.setText("Recurring: " + task.getRecurring());
        completion.setChecked(task.getCompletion().equals("Completed"));

        // ✅ Handle Completion Checkbox
        completion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Completed" : "Pending";
            dbHelper.updateTaskCompletion(task.getTaskId(), status);


        });

        // ✅ Handle Task Click - Edit Task
        taskView.setOnClickListener(v -> {
            Context context = v.getContext();
            Task selectedTask = (Task) v.getTag();

            TaskDialog.showTaskDialog(context, (getTitle, start, end, isRecurring) -> {
                // Update task in database
                dbHelper.updateTask(selectedTask.getTaskId(), getTitle, start, end, String.valueOf(isRecurring), this);

                // Refresh UI
                selectedTask.setTaskTitle(getTitle);
                selectedTask.setStartTime(start);
                selectedTask.setEndTime(end);
                selectedTask.setRecurring(String.valueOf(isRecurring));

                title.setText(getTitle);
                time.setText(start + " - " + end);
                recurring.setText("Recurring: " + isRecurring);


            }, selectedTask);
        });

        // ✅ Handle Undo/Redo with Same Task ID
        contentLayout.addView(taskView);

        undoStack.push(new Action(Action.ActionType.ADD, taskView, contentLayout.getChildCount() - 1, new Action.UndoRedoHandler() {
            @Override
            public void undo() {
                // ✅ Soft delete (Remove from UI only)
                contentLayout.removeView(taskView);

                // ✅ Cancel the same notification
                TaskNotificationManager.cancelNotification(DiaryPageActivity.this, task.getTaskId());

                // ✅ Temporarily mark task as "Deleted"
                dbHelper.updateTaskCompletion(task.getTaskId(), "Deleted");
            }

            @Override
            public void redo() {
                // ✅ Restore the same task ID and content
                contentLayout.addView(taskView);

                // ✅ Update task status back to Pending
                dbHelper.updateTaskCompletion(task.getTaskId(), "Pending");

                // ✅ Reschedule the same notification
            }
        }));

        redoStack.clear();

    }



//   Changing addTaskBlock() so it can load the taskBlock appropriately. Problem might be with onPause(). Problem was with onPause if-else there was two if conditions check Linear Layout availability.
//



    public void deleteResource(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            ViewGroup grandParent = (ViewGroup) parent.getParent();
            if (grandParent != null) {
                int position = ((LinearLayout) grandParent).indexOfChild(parent);

                // Check if the block is a Task Block
                Object tag = parent.getTag();
                if (tag instanceof Task) {
                    Task task = (Task) tag;

                    // ✅ Step 1: Delete Task from Database
                    dbHelper.deleteTask(task.getTaskId());

                    // ✅ Step 2: Cancel any Pending/Snoozed Notifications for this Task
                    TaskNotificationManager.cancelNotification(this, task.getTaskId());

                    // ✅ Step 3: Store action for undo
                    undoStack.push(new Action(Action.ActionType.DELETE, parent, position));
                    redoStack.clear();

                    // ✅ Step 4: Remove UI Block
                    grandParent.removeView(parent);

                    Log.d("TaskDelete", "🗑️ Task deleted and notification canceled.");
                } else {
                    // If it's not a Task Block, simply remove it.
                    undoStack.push(new Action(Action.ActionType.DELETE, parent, position));
                    redoStack.clear();
                    grandParent.removeView(parent);
                }
            }
        }
    }



    public void onResourcePageClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search_page, null);
        builder.setView(dialogView);

        EditText etSearchPage = dialogView.findViewById(R.id.etSearchPage);
        ListView lvSearchResults = dialogView.findViewById(R.id.lvSearchResults);

        // Fetch pages from the database
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);

        int userId = sharedPreferences.getInt("userId", -1);
        List<DiaryPage> pages = dbHelper.getAllDiaryPages(userId);
        List<String> pageTitles = new ArrayList<>();
        for (DiaryPage page : pages) {
            pageTitles.add(page.getPageId() + " - " + page.getPageTitle());
        }

        // Use custom adapter with filtering
        PageAdapter adapter = new PageAdapter(this, android.R.layout.simple_list_item_1, pageTitles);
        lvSearchResults.setAdapter(adapter);

        // Filter search results based on user input
        etSearchPage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s); // Uses custom filter logic
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle item selection
        lvSearchResults.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = adapter.getItem(position);
            int selectedPageId = Integer.parseInt(selectedItem.split(" - ")[0]); // Extract pageId
            Toast.makeText(this, "Selected Page ID: " + selectedPageId, Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // Pass the selected page ID to a function
            onPageSelected(selectedPageId);
        });
    }

    public void onPageSelected(int pageId) {
        Log.d("Diary", "Page selected: " + pageId);
        Toast.makeText(this, "Selected Page ID: " + pageId, Toast.LENGTH_SHORT).show();

        // Save the selected page as a resource
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        int userId = sharedPreferences.getInt("userId", -1);
        List<DiaryPage> pages = dbHelper.getAllDiaryPages(userId);

        for (DiaryPage page : pages) {
            if (page.getPageId() == pageId) {
                addPageBlock(page); // Add the view

                // ✅ Set pageId as the tag

                break;
            }
        }
    }

    private void addPageBlock(DiaryPage page) {
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        // Inflate Page Block layout
        View pageView = getLayoutInflater().inflate(R.layout.page_block, contentLayout, false);

        // ✅ Set pageId as the tag
        pageView.setTag(page);

        Log.d("addPageBlock", "Adding Page Block. Tag: " + pageView.getTag());

        // Get UI Elements
        TextView title = pageView.findViewById(R.id.tvBpPageTitle);
        TextView pageIdText = pageView.findViewById(R.id.tvBpPageId);

        // Set Data
        title.setText(page.getPageTitle());
        pageIdText.setText("#" + page.getPageId());



        // ✅ Add the Page Block to the Diary Page
        contentLayout.addView(pageView);

        undoStack.push(new Action(Action.ActionType.ADD, pageView, contentLayout.getChildCount() - 1));
        redoStack.clear();
    }


    public void loadPage(View view) {
        // Ensure the tag is not null
        DiaryPage page = (DiaryPage) view.getTag();
        if (page == null) {
            Toast.makeText(view.getContext(), "Error: Page ID not set", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("loadPage", "PageId: " + page.getPageId());

        try {
            int pageId = page.getPageId(); // Get page ID
            String pageTitle = page.getPageTitle(); // Get page title

            // Check if the page is a SWOT page
            if ("SWOT".equalsIgnoreCase(pageTitle)) {
                Toast.makeText(view.getContext(), "SWOT page cannot be loaded here", Toast.LENGTH_SHORT).show();
                Intent  intent = new Intent(DiaryPageActivity.this, SwotActivity.class);
                startActivity(intent);
            }else {

                // Show a Toast message
                Toast.makeText(view.getContext(), "Loading page: " + pageId, Toast.LENGTH_SHORT).show();

                // Create an Intent to start DiaryPageActivity
                Intent intent = new Intent(view.getContext(), DiaryPageActivity.class);
                intent.putExtra("pageId", pageId); // Pass the page ID

                view.getContext().startActivity(intent);

            }


        } catch (Exception e) {
            Toast.makeText(view.getContext(), "Error loading page", Toast.LENGTH_SHORT).show();
            Log.e("loadPage", "Error: ", e);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Call default back behavior

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        // Apply slide-out animation (DiaryPageActivity slides to the right)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);

        finish(); // Close current activity
    }



    public void undo(View view) {
        if (!undoStack.isEmpty()) {
            Action lastAction = undoStack.pop();
            LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

            if (lastAction.type == Action.ActionType.ADD) {
                contentLayout.removeView(lastAction.view);
            } else if (lastAction.type == Action.ActionType.DELETE) {
                contentLayout.addView(lastAction.view, lastAction.position);
            }

            redoStack.push(lastAction);
        }
    }

    public void redo(View view) {
        if (!redoStack.isEmpty()) {
            Action lastAction = redoStack.pop();
            LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

            if (lastAction.type == Action.ActionType.ADD) {
                contentLayout.addView(lastAction.view, lastAction.position);
            } else if (lastAction.type == Action.ActionType.DELETE) {
                contentLayout.removeView(lastAction.view);
            }

            undoStack.push(lastAction);
        }
    }




    private void openImageFullScreen(Uri imageUri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(imageUri, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionManager.STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker(); // ✅ Open image picker after permission is granted
            } else {
                Toast.makeText(this, "Storage permission is required to add an image.", Toast.LENGTH_SHORT).show();
            }
        }
    }



}




//Todo: Cleaning of database. Especially for tasks and image. View Removing is done for task and text only image remaining.
//Todo: Drag and restructuring of resoucrces.
