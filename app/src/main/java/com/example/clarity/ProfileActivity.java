package com.example.clarity;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class ProfileActivity extends AppCompatActivity {

    int userId = -1;
    String username = null;

    SharedPreferences sharedPreferences;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private ImageView profilePicture;
    private Button editProfilePicture, saveButton;
    private EditText nameField, emailField, phoneField;
    private TextView usernameField, userIdField;

    private DiaryDatabaseHelper dbHelper;
    private static final String TAG = "ProfileActivity";

    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        username = sharedPreferences.getString("username", null);
        userId = sharedPreferences.getInt("userId", -1);

        // Initialize views
        profilePicture = findViewById(R.id.profilePicture);
//        editProfilePicture = findViewById(R.id.editProfilePicture);
        saveButton = findViewById(R.id.saveButton);

        nameField = findViewById(R.id.nameField);
        emailField = findViewById(R.id.emailField);
        phoneField = findViewById(R.id.phoneField);
        usernameField = findViewById(R.id.usernameField);
        userIdField = findViewById(R.id.userIdField);

        dbHelper = new DiaryDatabaseHelper(this);

        // Load user data
        loadUserData();

        profilePicture.setOnClickListener(v -> checkPermissionAndOpenGallery());
        saveButton.setOnClickListener(v -> saveProfileData());
    }

    // ✅ Check permission before opening the gallery
    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        STORAGE_PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else {
            // For older Android versions
            openGallery();
        }
    }

    // ✅ Open gallery to pick image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // ✅ Handle the image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            if (imageUri != null) {
                profilePicture.setImageURI(imageUri);
            }
        }
    }

    // ✅ Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permission Denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ✅ Save profile data to the database
    private void saveProfileData() {
        String name = nameField.getText().toString();
        String email = emailField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String profilePictureUri = (imageUri != null) ? imageUri.toString() : null;

        boolean success = dbHelper.updateUserProfile(userId, name, email, phone, profilePictureUri);

        if (success) {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Load user data from the database
    private void loadUserData() {
        Cursor cursor = dbHelper.getUserProfile(userId);

        if (cursor.moveToFirst()) {
            nameField.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            emailField.setText(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            phoneField.setText(cursor.getString(cursor.getColumnIndexOrThrow("phoneNumber")));
            usernameField.setText(cursor.getString(cursor.getColumnIndexOrThrow("userName")));
            userIdField.setText(cursor.getString(cursor.getColumnIndexOrThrow("userId")));



            // Handle Profile Picture
            String imageUri = cursor.getString(cursor.getColumnIndexOrThrow("profilePicture"));
            if (imageUri != null && !imageUri.isEmpty()) {
                try {
                    profilePicture.setImageURI(Uri.parse(imageUri));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load profile image", e);
                }
            }
        }
        cursor.close();
    }
}
