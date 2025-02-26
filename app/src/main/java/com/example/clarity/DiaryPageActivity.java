package com.example.clarity;

import android.Manifest;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.io.FileOutputStream;

public class DiaryPageActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    private EditText editTitle;
    private DiaryDatabaseHelper dbHelper;
    private int pageId = -1;
    private LinearLayout contentLayout;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 100;



    @Override
    //onCreate() done
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_page);

        dbHelper = new DiaryDatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        Log.d("DatabaseCheck", "Diary Page Activity onCreate() called.");

        editTitle = findViewById(R.id.etDpaTitle);
        contentLayout = findViewById(R.id.llDpaContentLayout);

        // ✅ Request storage permission before loading data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        } else { // Android 12 and below
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }
        }

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
                    //Step 2: Checking if the imagePath(content is getting passed to the addImageToUI())
                    Log.d("Debug","loadPageData()->Image Path: " + content);

//                    addImageToUI(content);
                    insertImage(Uri.parse(content));
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
                    Log.e("Task", "Failed to load task with ID: " + taskId);
                }
            }


        }

        } else {
            Toast.makeText(this, "Failed to load page.", Toast.LENGTH_LONG).show();
        }
    }


    //onPause() done
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("LIFECYCLE", "onPause() called");

        String title = editTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Log.e("onPause", "⚠️ Title is empty! Not saving to database.");
            return; // Prevent the app from attempting an update
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


            if (view instanceof EditText) {
                String text = ((EditText) view).getText().toString().trim();
                if (!text.isEmpty()) {
                    contentBlocks.add(new Resource(pageId, "text", text, contentBlocks.size() + 1));
                    Log.d("onPause", "📌 Saved Text Block: " + text);
                }
            } else if (view instanceof ImageView) {
                String imagePath = (String) view.getTag();
                Log.d("Debug", "onPause()->Image Path: " + imagePath);

                if (imagePath != null) {
                    contentBlocks.add(new Resource(pageId, "image", imagePath, contentBlocks.size() + 1));
                    Log.d("onPause", "🖼 Saved Image: " + imagePath);
                }
            } else if (view.getTag() instanceof Task) {
                // Ensure task data is saved
                Task task = (Task) view.getTag();
                contentBlocks.add(new Resource(pageId, "task", String.valueOf(task.getTaskId()), contentBlocks.size() + 1));
                Log.d("onPause", "✅ Saved Task Block: " + task.getTaskTitle());
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
        } else {
            Log.d("onPause", "⚠️ Nothing to save, skipping database update.");
        }
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

            // Get image file paths before deleting resources
            Cursor cursor = db.rawQuery("SELECT resourceContent FROM resources WHERE pageId = ? AND resourceType = 'image'",
                    new String[]{String.valueOf(pageId)});

            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(0);
                if (imagePath != null) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists() && imageFile.delete()) {
                        Log.d("DiaryPageActivity", "Deleted image file: " + imagePath);
                    } else {
                        Log.e("DiaryPageActivity", "Failed to delete image file: " + imagePath);
                    }
                }
            }
            cursor.close();

            // Delete resources linked to the page
            db.delete("resources", "pageId = ?", new String[]{String.valueOf(pageId)});

            // Delete the actual page
            int rowsAffected = db.delete("pages", "pageId = ?", new String[]{String.valueOf(pageId)});

            if (rowsAffected > 0) {
                Toast.makeText(this, "Page Deleted", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Page not found!", Toast.LENGTH_SHORT).show();
            }

            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e("DiaryPageActivity", "Error deleting page: " + e.getMessage());
            Toast.makeText(this, "Error deleting page!", Toast.LENGTH_SHORT).show();
        } finally {
            db.endTransaction();
            db.close();
        }
    }


    //addNewTextBlock() done
//    private void addNewTextBlock() {
//        String text = "";
//        // Avoid adding unnecessary empty blocks
//        if (hasEmptyTextBlock()) {
//            return;
//        }
//
//        EditText newEditText = new EditText(this);
//        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//        ));
//        newEditText.setHint("New Text Block");
//        newEditText.setText(text);
//        newEditText.setTextSize(16);
//        newEditText.setPadding(8, 8, 8, 8);
//        newEditText.setBackgroundColor(Color.TRANSPARENT);
//        newEditText.setTextColor(ContextCompat.getColor(this, R.color.brown_light)); // API-safe color
//        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
//
//        // Add the new EditText to the layout
//        contentLayout.addView(newEditText);
//
//        // Move focus to the new EditText
//        newEditText.requestFocus();
//        newEditText.setSelection(newEditText.getText().length()); // Cursor at the end
//
//        // Show the keyboard automatically
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null) {
//            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
//        }
//
//        // Enable double-tap delete
////        enableDoubleTapToDelete(newEditText);
//    }

    private void addNewTextBlock() {
        // Avoid adding unnecessary empty blocks
        if (hasEmptyTextBlock()) {
            return;
        }



        View textBlock = getLayoutInflater().inflate(R.layout.text_block, contentLayout, false);
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        EditText newEditText = textBlock.findViewById(R.id.etTextBlock);
        newEditText.requestFocus(); // Move focus to the new EditText

        // Show the keyboard automatically
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
        }

        // Add the text block to the layout
        contentLayout.addView(textBlock);
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

        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newEditText.setText(textContent); // Set the retrieved text
        newEditText.setTextSize(16);
        int paddingPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()
        );
        newEditText.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        newEditText.setBackgroundColor(Color.TRANSPARENT);
        newEditText.setTextColor(ContextCompat.getColor(this, R.color.brown_light)); // API-safe color
        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        contentLayout.addView(newEditText); // Add to UI

        // Move focus to the new EditText
        newEditText.requestFocus();
        newEditText.setSelection(newEditText.getText().length()); // Cursor at the end

        // Show the keyboard automatically
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
        }
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
        openImagePicker();
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


    //saveImageToInternalStorage() done
    private String saveImageToInternalStorage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            File directory = new File(getFilesDir(), "diary_images");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("DiaryPageActivity", "Error saving image: " + e.getMessage());
            return null;
        }
    }


    //insertImage() done
    private void insertImage(Uri imageUri) {
        EditText focusedEditText = getCurrentFocusedEditText();

        if (focusedEditText == null) {
            Toast.makeText(this, "No active text block found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inflate Image Block XML (like text block)
        View imageBlock = getLayoutInflater().inflate(R.layout.image_block, contentLayout, false);

        // Get ImageView & Delete Button
        ImageView imageView = imageBlock.findViewById(R.id.ivImageBlock);
        ImageButton deleteButton = imageBlock.findViewById(R.id.btnDeleteImage);

        // Set Image
        imageView.setImageURI(imageUri);
        imageView.setTag(imageUri.toString());

        // Delete Button Functionality
        deleteButton.setOnClickListener(v -> contentLayout.removeView(imageBlock));

        // Insert Image Below Focused EditText
        int cursorIndex = contentLayout.indexOfChild(focusedEditText);
        contentLayout.addView(imageBlock, cursorIndex + 1);
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

// Task------------------------------


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

        // Update task completion status on checkbox change
        completion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Completed" : "Pending";
            dbHelper.updateTaskCompletion(task.getTaskId(), status);
        });

        // Add Task Block to Diary Page
        contentLayout.addView(taskView);
    }

}




