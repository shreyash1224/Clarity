package com.example.clarity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    EditText etCaaUsername, etCaaPassword, etCaaConfirmPassword, etCaaSecurityQuestion, etCaaSecurityAnswer;
    DiaryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        etCaaUsername = findViewById(R.id.etCaaUsername);
        etCaaPassword = findViewById(R.id.etCaaNewPassword);
        etCaaConfirmPassword = findViewById(R.id.etCaaConfirmPassword);
        etCaaSecurityQuestion = findViewById(R.id.etCaaSecurityQuestion);
        etCaaSecurityAnswer = findViewById(R.id.etCaaSecurityAnswer);
        dbHelper = new DiaryDatabaseHelper(this);
    }

    // This method is called when the user clicks the 'Create Account' button
    public void onCreateAccountClicked(View view) {
        String username = etCaaUsername.getText().toString().trim();
        String password = etCaaPassword.getText().toString().trim();
        String confirmPassword = etCaaConfirmPassword.getText().toString().trim();
        String securityQuestion = etCaaSecurityQuestion.getText().toString().trim();
        String securityAnswer = etCaaSecurityAnswer.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || securityQuestion.isEmpty() || securityAnswer.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dbHelper.isUsernameTaken(username)) {
            Toast.makeText(this, "Username already taken. Please choose another.", Toast.LENGTH_SHORT).show();
        } else {
            if(password.length() < 6){
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show();
            } else {
                long result = dbHelper.addUser(username, password, securityQuestion, securityAnswer);
                if (result != -1) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Optional: Method to handle the 'Back to Login' button
    public void onBackToLoginClicked(View view) {
        Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close the current activity
    }
}
