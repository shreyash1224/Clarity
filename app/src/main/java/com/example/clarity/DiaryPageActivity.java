package com.example.clarity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        Intent intent = getIntent();
        pageId = intent.getIntExtra("pageId", -1);
        Log.d("DiaryPageActivity", "pageId received: " + pageId);

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
            Log.d("DiaryPageActivity", "Retrieved " + resources.size() + " resources.");

            for (Pair<String, String> resource : resources) {
                String type = resource.first;
                String content = resource.second;

                if (type.equals("text")) {
                    Log.d("DiaryPageActivity", "Text Block: " + content);
                    addTextBlockToUI(content);
                } else if (type.equals("image")) {
                    Log.d("DiaryPageActivity", "Image Path: " + content);
                    addImageToUI(content);
                }
            }

            Log.d("DiaryPageActivity", "Loaded page: " + page);
        } else {
            Toast.makeText(this, "Failed to load page.", Toast.LENGTH_LONG).show();
        }
    }


    //onPause() done
    @Override
    protected void onPause() {
        super.onPause();

        String title = editTitle.getText().toString().trim();
        ArrayList<Resource> contentBlocks = new ArrayList<>();
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);

            if (view instanceof EditText) {
                String text = ((EditText) view).getText().toString().trim();
                if (!text.isEmpty()) {
                    contentBlocks.add(new Resource(pageId, "text", text, i));
                }
            } else if (view instanceof ImageView) {
                String imagePath = (String) view.getTag();  // Retrieve image path from setTag()
                if (imagePath != null) {
                    contentBlocks.add(new Resource(pageId, "image", imagePath, i));
                }
            }
        }

        int userId = sharedPreferences.getInt("userId", -1);

        if (!title.isEmpty() || !contentBlocks.isEmpty()) {
            if (pageId == -1) {
                pageId = dbHelper.updatePage(-1, title, contentBlocks, userId);
                Log.d("DiaryPageActivity", "New Page Created: " + pageId);
                Toast.makeText(this, "New Page Created", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.updatePage(pageId, title, contentBlocks, userId);
                Log.d("DiaryPageActivity", "Page Updated: " + pageId);
                Toast.makeText(this, "Page Updated", Toast.LENGTH_SHORT).show();
            }
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
    private void addNewTextBlock(String text) {
        // Avoid adding unnecessary empty blocks
        if (text.isEmpty() && hasEmptyTextBlock()) {
            return;
        }

        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newEditText.setHint("New Text Block");
        newEditText.setText(text);
        newEditText.setTextSize(16);
        newEditText.setPadding(8, 8, 8, 8);
        newEditText.setBackgroundColor(Color.TRANSPARENT);
        newEditText.setTextColor(ContextCompat.getColor(this, R.color.brown_light)); // API-safe color
        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        // Add the new EditText to the layout
        contentLayout.addView(newEditText);

        // Move focus to the new EditText
        newEditText.requestFocus();
        newEditText.setSelection(newEditText.getText().length()); // Cursor at the end

        // Show the keyboard automatically
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(newEditText, InputMethodManager.SHOW_IMPLICIT);
        }

        // Enable double-tap delete
//        enableDoubleTapToDelete(newEditText);
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
        addNewTextBlock("");
    }










//    private void saveTextBlocks() {
//        dbHelper.deleteTextBlocksByPageId(pageId);
//        for (int i = 0; i < contentLayout.getChildCount(); i++) {
//            View view = contentLayout.getChildAt(i);
//            if (view instanceof EditText) {
//                String text = ((EditText) view).getText().toString().trim();
//                if (!text.isEmpty()) {
//                    dbHelper.insertTextBlock(pageId, text, i);
//                }
//            }
//        }
//    }


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


//    addImageButton() done
    private void addImageToUI(String imagePath) {
        Log.d("DatabaseCheck", "Adding image to UI: " + imagePath);
        debugResourcesTable();
        if (imagePath == null || imagePath.trim().isEmpty()) {
            Log.e("DiaryPageActivity", "Invalid image path.");
            return;
        }

        File file = new File(imagePath);
        if (!file.exists()) {
            Log.e("DiaryPageActivity", "Image file not found: " + imagePath);
            return;
        }

        Log.d("DiaryPageActivity", "Adding image to UI: " + imagePath);
        debugResourcesTable();

        // Optimize Bitmap Loading (Prevent OutOfMemoryError)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int reqWidth = contentLayout.getWidth(); // Match parent width
        int reqHeight = 600; // Limit height

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

        // Create ImageView
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,  // Maintain aspect ratio
                reqHeight
        );
        layoutParams.setMargins(10, 10, 10, 10); // Add spacing
        imageView.setLayoutParams(layoutParams);
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // Optional: Add delete button
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        frameLayout.addView(imageView);
        frameLayout.addView(createDeleteButton(frameLayout)); // Add delete button

        contentLayout.addView(frameLayout);
    }

    // Function to calculate inSampleSize for efficient image loading

    //calculateInSampleSize() done
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // Function to create a delete button for images
    //createDeleteButton() done
    private ImageView createDeleteButton(final View parentView) {
        ImageView deleteButton = new ImageView(this);
        deleteButton.setImageResource(R.drawable.ic_delete); // Add a close icon in drawable
        deleteButton.setBackgroundResource(R.drawable.rounded_button);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                80, 80, Gravity.TOP | Gravity.END
        );
        layoutParams.setMargins(10, 10, 10, 10);
        deleteButton.setLayoutParams(layoutParams);

        deleteButton.setOnClickListener(v -> {
            contentLayout.removeView(parentView);
        });

        return deleteButton;
    }


    //openImagePicker() done
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }


    //onResourceImageClick() done
    public void onResourceImageClick(View view) {
        openImagePicker();
        Toast.makeText(this, "Adding Image clicked.", Toast.LENGTH_SHORT).show();
    }

    //onActivityResult() done
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                String savedImagePath = saveImageToInternalStorage(imageUri);
                if (savedImagePath != null) {
                    insertImage(Uri.parse(savedImagePath));
                } else {
                    Log.e("DiaryPageActivity", "Failed to save image.");
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

        // Save Image and Get Path
        saveImageToDatabase(imageUri);

        // Create ImageView
        ImageView imageView = new ImageView(this);
        imageView.setImageURI(imageUri);
        imageView.setAdjustViewBounds(true);
        imageView.setMaxHeight(focusedEditText.getLineHeight() * 8);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Insert Image Below Focused EditText
        int cursorIndex = contentLayout.indexOfChild(focusedEditText);
        contentLayout.addView(imageView, cursorIndex + 1);
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



    //saveImageToDatabase() done
    private void saveImageToDatabase(Uri imageUri) {
        try {
            // Convert URI to Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Create a file inside app's internal storage
            File imageFile = new File(getFilesDir(), "image_" + System.currentTimeMillis() + ".jpg");

            // Save the bitmap to the file
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            // Get absolute file path
            String imagePath = imageFile.getAbsolutePath();

            // Save Image Path to Database
            int resourceOrder = contentLayout.indexOfChild(getCurrentFocusedEditText()) + 1;
            dbHelper.insertResource(pageId, "image", imagePath, resourceOrder);

            Log.d("DiaryPageActivity", "Image saved: " + imagePath);
        } catch (IOException e) {
            Log.e("DiaryPageActivity", "Error saving image: " + e.getMessage());
        }
    }

    public void debugResourcesTable() {
        Log.d("DatabaseCheck", "DebugResourceTable() Called In Diary Database.");
        SQLiteDatabase db = dbHelper.getReadableDatabase();
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


