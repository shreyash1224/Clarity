package com.example.clarity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ImageView imageView = findViewById(R.id.fullscreenImageView);

        // Get Image URI from Intent
        String imageUri = getIntent().getStringExtra("imageUri");
        if (imageUri != null) {
            imageView.setImageURI(Uri.parse(imageUri));
        }

        // Close on click
        imageView.setOnClickListener(v -> finish());
    }
}
