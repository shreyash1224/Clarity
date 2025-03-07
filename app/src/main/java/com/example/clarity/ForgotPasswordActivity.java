package com.example.clarity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    // Declare variables for the EditText views, TextView for the question, and the database helper
    EditText etForgotUsername, etForgotSecurityAnswer, etForgotNewPassword, etForgotConfirmNewPassword;
    TextView tvFpaSecurityQuestion;

    DiaryDatabaseHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize the EditText fields and the TextView for the security question
        etForgotUsername = findViewById(R.id.etFpaUsername);
        etForgotSecurityAnswer = findViewById(R.id.etFpaSecurityAnswer);
        etForgotNewPassword = findViewById(R.id.etFpaNewPassword);
        etForgotConfirmNewPassword = findViewById(R.id.etFpaConfirmNewPassword);
        tvFpaSecurityQuestion = findViewById(R.id.tvFpaSecurityQuestion);

        dbHelper = new DiaryDatabaseHelper(this);

        // Retrieve the username passed from LoginActivity
        String username = getIntent().getStringExtra("username");

        if (username != null && !username.isEmpty()) {
            etForgotUsername.setText(username);  // Display the username in the EditText field
            fetchSecurityQuestion(username);  // Fetch and display the security question
        }
    }

    // Method to fetch and display the security question for the entered username
    private void fetchSecurityQuestion(String username) {
        String securityQuestion = dbHelper.getSecurityQuestion(username);

        // Set the security question to the TextView
        if (securityQuestion != null) {
            tvFpaSecurityQuestion.setText("Security Question: " + securityQuestion);
        } else {
            tvFpaSecurityQuestion.setText("Security Question: Not found");
        }
    }


    // This method is called when the user clicks the 'Reset Password' button
    public void onResetPasswordClicked(View view) {
        String username = etForgotUsername.getText().toString().trim();
        String securityAnswer = etForgotSecurityAnswer.getText().toString().trim();
        String newPassword = etForgotNewPassword.getText().toString().trim();
        String confirmPassword = etForgotConfirmNewPassword.getText().toString().trim();

        // Validate the input fields
        if (username.isEmpty() || securityAnswer.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the passwords match
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure password is at least 6 characters
        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate the security answer using the database helper
        boolean isValid = dbHelper.validateSecurityAnswer(username, securityAnswer);

        if (isValid) {
            // If the security answer is correct, update the password
            boolean result = dbHelper.updatePassword(username, newPassword);

            if (result) {
                // Show success message and finish the activity
                Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity and go back to login screen
            } else {
                // Show error message if updating the password fails
                Toast.makeText(this, "Error resetting password", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Show error message if the security answer is incorrect
            Toast.makeText(this, "Incorrect security answer", Toast.LENGTH_SHORT).show();
        }
    }

    // This method is called when the user clicks the 'Back to Login' button
    public void onBackToLoginClicked(View view) {
        // Finish the current activity and go back to the login screen
        finish();
    }

    // Method to fetch and display the security question for the entered username

    @Override
    protected void onResume() {
        super.onResume();

        // Fetch the security question when the activity is resumed (after username is entered)
        String username = etForgotUsername.getText().toString().trim();
        if (!username.isEmpty()) {
            fetchSecurityQuestion(username);
        }
    }
}
