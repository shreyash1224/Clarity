package com.example.clarity;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;

public class DiaryPageActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    private EditText editTitle, editContent;
    private DiaryDatabaseHelper dbHelper;
    private Integer pageId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_page);
        Log.d("DiaryPageActivity", "onCreate() called.");

        dbHelper = new DiaryDatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        editTitle = findViewById(R.id.etDpaTitle);
//        editContent = findViewById(R.id.etDpaContent);

        Intent intent = getIntent();
        pageId = intent.getIntExtra("pageId", -1);
        Log.d("DiaryPageActivity", "pageId received: " + pageId);

        if (pageId != -1) {
            DiaryPage page = dbHelper.getPageById(pageId);
            if (page != null) {
                editTitle.setText(page.getPageTitle());
                editContent.setText(page.getPageContent());
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
        String content = editContent.getText().toString().trim();
        int userId = sharedPreferences.getInt("userId", -1);

        if (!title.isEmpty() || !content.isEmpty()) {
            if (pageId == -1) {
                pageId = dbHelper.updatePage(-1, title, content, userId, "Text");
                Log.d("DiaryPageActivity", "New Page Created: " + pageId);
                Toast.makeText(this, "New Page Created", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.updatePage(pageId, title, content, userId, "Text");
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

    public void onResourceImageClick(View view) {
        // TODO: Implement image selection and insertion logic
        Toast.makeText(this, "Image adder clicked.", Toast.LENGTH_SHORT).show();
    }

    public void onResourceTextClick(View view) {
        LinearLayout contentLayout = findViewById(R.id.llDpaContentLayout); // Make sure you have an ID for the parent layout

        EditText newEditText = new EditText(this);
        newEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        newEditText.setHint("New Text Block");
        newEditText.setTextSize(16);
        newEditText.setPadding(8, 8, 8, 8);
        newEditText.setBackgroundColor(Color.TRANSPARENT);
        newEditText.setTextColor(getResources().getColor(R.color.brown_light));
        newEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        contentLayout.addView(newEditText);
    }

}
