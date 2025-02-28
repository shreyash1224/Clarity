package com.example.clarity;

import android.Manifest;
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

import androidx.appcompat.app.AlertDialog;
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
        debugResourcesTable("Database");
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


            if (view instanceof LinearLayout) { // Check if it's a LinearLayout container
                EditText editText = view.findViewById(R.id.etTextBlock);

                if (editText != null) {  // Handling Text Block
                    String text = editText.getText().toString().trim();
                    if (!text.isEmpty()) {
                        contentBlocks.add(new Resource(pageId, "text", text, contentBlocks.size() + 1));
                        Log.d("onPause", "📌 Saved Text Block: " + text);
                    }
                } else { // Handling Task Block
                    Object tag = view.getTag();
                    if (tag instanceof Task) {
                        Task task = (Task) tag;
                        contentBlocks.add(new Resource(pageId, "task", String.valueOf(task.getTaskId()), contentBlocks.size() + 1));
                        Log.d("onPause", "✅ Task Block Detected & Saved: " + task.getTaskTitle());
                    } else {
                        Log.e("onPause", "❌ Task Block Missing Tag!");
                    }
                }
            } else if (view instanceof ImageView) {  // Handling Image Block
                String imagePath = (String) view.getTag();
                Log.d("Debug", "onPause()->Image Path: " + imagePath);

                if (imagePath != null) {
                    Log.d("Image", "imagePath: " + imagePath);
                    contentBlocks.add(new Resource(pageId, "image", imagePath, contentBlocks.size() + 1));
                    Log.d("onPause", "🖼 Saved Image: " + imagePath);
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
        } else {
            Log.d("onPause", "⚠️ Nothing to save, skipping database update.");
        }

        dbHelper.cleanUpTasks();
    }

    //Todo: onPause() changed.

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
    newEditText.setText(textContent); // Set the retrieved text
    newEditText.setSelection(newEditText.getText().length()); // Place cursor at the end

    // Add the complete text block layout to the content area
    contentLayout.addView(textBlock);

    // Move focus to the new EditText
    newEditText.requestFocus();

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

//    private void insertImage(Uri imageUri) {
//        EditText focusedEditText = getCurrentFocusedEditText();
//
//        if (focusedEditText == null) {
//            Toast.makeText(this, "No active text block found!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//
//
//        // Create ImageView
//        ImageView imageView = new ImageView(this);
//        imageView.setImageURI(imageUri);
//        imageView.setAdjustViewBounds(true);
//        imageView.setMaxHeight(focusedEditText.getLineHeight() * 8);
//        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
//
//        // 🔹 Store image URI inside ImageView for later retrieval
//        imageView.setTag(imageUri.toString());  // ✅ Add this line
//
//        // Insert Image Below Focused EditText
//        int cursorIndex = contentLayout.indexOfChild(focusedEditText);
//        contentLayout.addView(imageView, cursorIndex + 1);
//
//    }
//
//

    private void insertImage(Uri imageUri) {
        EditText focusedEditText = getCurrentFocusedEditText();
        ViewGroup textBlock = null;


        // Create ImageView
        ImageView imageView = new ImageView(this);
        imageView.setImageURI(imageUri);

        imageView.setFocusable(true);
        imageView.setFocusableInTouchMode(true);
        imageView.requestFocus();


        // 🔹 Calculate max height based on 10 lines of text
        int maxHeight = (focusedEditText != null)
                ? focusedEditText.getLineHeight() * 10  // 10 lines of text height
                : 600; // Fallback max height

        // 🔹 Set the ImageView scale type to maintain aspect ratio
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // 🔹 Measure the image dimensions
        imageView.post(() -> {
            Drawable drawable = imageView.getDrawable();
            if (drawable != null) {
                int originalWidth = drawable.getIntrinsicWidth();
                int originalHeight = drawable.getIntrinsicHeight();

                // Calculate new width to maintain aspect ratio
                int newWidth = (int) (((float) originalWidth / originalHeight) * maxHeight);

                // Apply final layout parameters
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        newWidth,  // Width proportional to height
                        maxHeight  // Max height limited to 10 lines
                );

                layoutParams.setMargins(0, 8, 0, 8); // Add minimal margins
                imageView.setLayoutParams(layoutParams);
            }
        });

        // Store image URI inside ImageView for later retrieval
        imageView.setTag(imageUri.toString());

        if (textBlock != null) {
            // Insert Image Below the Text Block's Parent (LinearLayout)
            int index = contentLayout.indexOfChild(textBlock);
            contentLayout.addView(imageView, index + 1);
        } else {
            // If no text block exists, add the image at the bottom
            contentLayout.addView(imageView);
        }
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

//   Changing addTaskBlock() so it can load the taskBlock appropriately. Problem might be with onPause(). Problem was with onPause if-else there was two if conditions check Linear Layout availability.
//



    public void deleteResource(View view) {
        // Get the resource container (assuming the button is inside a resource layout)
        ViewGroup parent = (ViewGroup) view.getParent();

        if (parent != null) {
            ViewGroup grandParent = (ViewGroup) parent.getParent();
            if (grandParent != null) {
                grandParent.removeView(parent); // Removes the whole block
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
        List<Page> pages = dbHelper.getAllDiaryPages();
        List<String> pageTitles = new ArrayList<>();
        for (Page page : pages) {
            pageTitles.add(page.getPageId() + " - " + page.getTitle());
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
        List<Page> pages = dbHelper.getAllDiaryPages();

        for (Page page : pages) {
            if (page.getPageId() == pageId) {
                addDiaryPage(pageId, page.getTitle()); // Add the view
                break;
            }
        }

//        long resourceId = dbHelper.addResource(pageId, "page", String.valueOf(pageId));
//
//        if (resourceId != -1) {
//            Toast.makeText(this, "Page saved as resource!", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Failed to save page as resource!", Toast.LENGTH_SHORT).show();
//        }
    }

    private void addDiaryPage(int pageId, String title) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View pageView = inflater.inflate(R.layout.page_block, null, false);

        TextView tvPageTitle = pageView.findViewById(R.id.tvDiplPageTitle);
        TextView tvPageId = pageView.findViewById(R.id.tvDiplPageId);

        tvPageTitle.setText(title);
        tvPageId.setText("#"+String.valueOf(pageId));

        // Add the view to the diary page layout
        LinearLayout diaryPageContainer = findViewById(R.id.llDpaContentLayout);
        diaryPageContainer.addView(pageView);

        // Optional: Click listener to open the referenced diary page
//        pageView.setOnClickListener(v -> {
//            Toast.makeText(this, "Opening Page ID: " + pageId, Toast.LENGTH_SHORT).show();
//            openDiaryPage(pageId); // Function to open the diary page
//        });
    }







}


//Todo: Cleaning of database. Especially for tasks and image. View Removing is done for task and text only image remaining.
//Todo: Drag and restructuring of resoucrces.
