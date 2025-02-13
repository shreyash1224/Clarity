package com.example.clarity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import android.content.SharedPreferences;


public class DiaryPageActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_REQUEST = 1;

    SharedPreferences sharedPreferences;

    private EditText editTitle, editContent;
    private DiaryDatabaseHelper dbHelper;


    private Uri selectedImageUri;

    private Integer pageId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_page);
        Toast.makeText(this, "Diary Page Activity onCreate() called.", Toast.LENGTH_SHORT).show();
        dbHelper = new DiaryDatabaseHelper(this);
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

    }

    @Override
    protected void onPause() {
        super.onPause();

        editTitle = findViewById(R.id.etDpaTitle);
        editContent = findViewById(R.id.etDpaContent);

        String title = editTitle.getText().toString();
        String content = editContent.getText().toString();
        int userId = sharedPreferences.getInt("userId", -1);

        pageId = dbHelper.updatePage(pageId,title,content, userId, "Text");
        Toast.makeText(this, ""+pageId, Toast.LENGTH_SHORT).show();
        Log.d("Page Activity", ""+pageId);

    }



}