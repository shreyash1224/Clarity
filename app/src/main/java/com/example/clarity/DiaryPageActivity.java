package com.example.clarity;
import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
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
        Toast.makeText(this, "Diary Page Activity onCreate() called.", Toast.LENGTH_SHORT).show();
        dbHelper = new DiaryDatabaseHelper(this);
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        editTitle = findViewById(R.id.etDpaTitle);
        editContent = findViewById(R.id.etDpaContent);

        // Get pageId from Intent
        Intent intent = getIntent();
        pageId = intent.getIntExtra("pageId", -1); // Default -1 if not found
        Log.d("Page Activity", "pageId: " + pageId);


        //Loading existing page
        if (pageId != -1) {
            DiaryPage page = dbHelper.getPageById(pageId);
            Log.d("Diary Page Activity:", "After if");
            if (page != null) {
                Log.d("Diary Page Activity:", "After if page");
                editTitle.setText(page.getPageTitle());
                Log.d("Page Activity Debug: ", page.getPageTitle());
                editContent.setText(page.getPagetContent());

                Log.d("Page Activity Debug: ", page.getPagetContent());
            } else {
                Toast.makeText(this, "Page null", Toast.LENGTH_LONG).show();
            }
        }

    }

    protected void onPause() {
        super.onPause();

        editTitle = findViewById(R.id.etDpaTitle);
        editContent = findViewById(R.id.etDpaContent);

        String title = editTitle.getText().toString();
        String content = editContent.getText().toString();
        int userId = sharedPreferences.getInt("userId", -1);

        if (!title.isEmpty() || !content.isEmpty()) {
            if (pageId == -1) {  // New page (instead of checking for null)
                pageId = dbHelper.updatePage(-1, title, content, userId, "Text");
                Toast.makeText(this, "New Page Created: " + pageId, Toast.LENGTH_SHORT).show();
            } else {  // Existing page update
                dbHelper.updatePage(pageId, title, content, userId, "Text");
                Toast.makeText(this, "Page Updated: " + pageId, Toast.LENGTH_SHORT).show();
            }
            Log.d("Page Activity", "pageId after update: " + pageId);
        }

    }



}