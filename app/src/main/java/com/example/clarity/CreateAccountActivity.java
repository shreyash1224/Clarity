package com.example.clarity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

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
        Button btnCaaCreateAccount = findViewById(R.id.btnCaaCreateAccount);
        dbHelper = new DiaryDatabaseHelper(this);
    }

    public void createAccount(View view) {
        String username = etCaaUsername.getText().toString().trim();
        String password = etCaaPassword.getText().toString().trim();
        String confirmPassword = etCaaConfirmPassword.getText().toString().trim();
        String securityQuestion = etCaaSecurityQuestion.getText().toString().trim();
        String securityAnswer = etCaaSecurityAnswer.getText().toString().trim();

        // Perform input validation
        if (!isValidUsername(username)) {
            etCaaUsername.setError("Invalid username (3-50 chars, letters/numbers/underscores only)");
            return;
        }

        if (dbHelper.isUsernameTaken(username)) {
            etCaaUsername.setError("Username already taken");
            return;
        }

        if (securityQuestion.isEmpty()) {
            etCaaSecurityQuestion.setError("Security question is required");
            return;
        }

        if (securityAnswer.isEmpty() || securityAnswer.length() < 2) {
            etCaaSecurityAnswer.setError("Security answer must be at least 2 characters");
            return;
        }

        if (!isValidPassword(password)) {
            etCaaPassword.setError("Password must be at least 6 chars with an uppercase, a number, and a special char");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etCaaConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Save to the database
        long result = dbHelper.addUser(username, password, securityQuestion, securityAnswer);
        if (result != -1) {
            Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CreateAccountActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Error creating account", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9_]{3,50}$");
    }

    private boolean isValidPassword(String password) {
        Pattern PASSWORD_PATTERN = Pattern.compile(
                "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$"
        );
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
