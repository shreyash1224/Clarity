package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DiaryPageActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    private EditText editTitle;
    private DiaryDatabaseHelper dbHelper;
    private Integer pageId = -1;
    private LinearLayout contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_page);
        Log.d("DiaryPageActivity", "onCreate() called.");

        dbHelper = new DiaryDatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        editTitle = findViewById(R.id.etDpaTitle);
        contentLayout = findViewById(R.id.llDpaContentLayout);

        Intent intent = getIntent();
        pageId = intent.getIntExtra("pageId", -1);
        Log.d("DiaryPageActivity", "pageId received: " + pageId);
        if (pageId != -1) {
            DiaryPage page = dbHelper.getPageById(pageId);
            if (page != null) {
                editTitle.setText(page.getPageTitle());

                // Fetch and display text blocks
                List<String> textBlocks = dbHelper.getTextBlocksByPageId(pageId);
                for (String text : textBlocks) {
                    addTextBlockToUI(text);
                }

                Log.d("DiaryPageActivity", "Loaded page: " + page.toString());
            } else {
                Toast.makeText(this, "Failed to load page.", Toast.LENGTH_LONG).show();
            }
        }


    }

    @Override
    protected void onPause() {
        super.onPause();

        String title = editTitle.getText().toString().trim();
        ArrayList<String> contentBlocks = new ArrayList<>();
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);
            if (view instanceof EditText) {
                String text = ((EditText) view).getText().toString().trim();
                if (!text.isEmpty()) {
                    contentBlocks.add(text);
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


    public void deletePage(View view) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete("resources", "pageId = ?", new String[]{String.valueOf(pageId)});
            int rowsAffected = db.delete("pages", "pageId = ?", new String[]{String.valueOf(pageId)});

            if (rowsAffected > 0) {
                Toast.makeText(this, "Page Deleted", Toast.LENGTH_LONG).show();
                finish();
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e("DiaryPageActivity", "Error deleting page: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void onResourceTextClick(View view) {
        addNewTextBlock("");
    }

    private void addNewTextBlock(String text) {
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
        newEditText.setTextColor(getResources().getColor(R.color.brown_light));
        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        contentLayout.addView(newEditText);
    }

    private void saveTextBlocks() {
        dbHelper.deleteTextBlocksByPageId(pageId);
        for (int i = 0; i < contentLayout.getChildCount(); i++) {
            View view = contentLayout.getChildAt(i);
            if (view instanceof EditText) {
                String text = ((EditText) view).getText().toString().trim();
                if (!text.isEmpty()) {
                    dbHelper.insertTextBlock(pageId, text, i);
                }
            }
        }
    }

    private void loadTextBlocks() {
        List<String> textBlocks = dbHelper.getTextBlocksByPageId(pageId);
        for (String text : textBlocks) {
            addNewTextBlock(text);
        }
    }
    private void addTextBlockToUI(String textContent) {
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout);

        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newEditText.setText(textContent); // Set the retrieved text
        newEditText.setTextSize(16);
        newEditText.setPadding(8, 8, 8, 8);
        newEditText.setBackgroundColor(Color.TRANSPARENT);
        newEditText.setTextColor(getResources().getColor(R.color.brown_light));
        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        contentLayout.addView(newEditText); // Add to the UI
    }

}


