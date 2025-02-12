package com.example.clarity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    EditText etCaaUsername, etCaaPassword, etCaaConfirmPassword;
    DiaryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        etCaaUsername = findViewById(R.id.etCaaUsername);
        etCaaPassword = findViewById(R.id.etCaaPassword);
        etCaaConfirmPassword = findViewById(R.id.etCaaConfirmPassword);
        dbHelper = new DiaryDatabaseHelper(this);
    }

    // This method is called when the user clicks the 'Create Account' button
    public void onCreateAccountClicked(View view) {
        String username = etCaaUsername.getText().toString().trim();
        String password = etCaaPassword.getText().toString().trim();
        String confirmPassword = etCaaConfirmPassword.getText().toString().trim();

        Toast.makeText(this, username+" "+password+" "+confirmPassword, Toast.LENGTH_LONG).show();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        /*
        if (dbHelper.usernameExists(username)) {
            Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
        } else {
            long result = dbHelper.createUser(username, password);
            if (result != -1) {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
            }
        }

         */
    }


    // Optional: Method to handle the 'Back to Login' button
    public void onBackToLoginClicked(View view) {
        Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close the current activity
    }
}
