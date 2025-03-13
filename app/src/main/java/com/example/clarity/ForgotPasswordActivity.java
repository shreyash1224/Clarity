package com.example.clarity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.regex.Pattern;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText etFpaUsername, etFpaSecurityAnswer, etFpaNewPassword, etFpaConfirmPassword;
    TextView tvFpaSecurityQuestion;
    DiaryDatabaseHelper dbHelper;
    String storedSecurityQuestion = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        dbHelper = new DiaryDatabaseHelper(this);


        etFpaUsername = findViewById(R.id.etFpaUsername);
        etFpaSecurityAnswer = findViewById(R.id.etFpaSecurityAnswer);
        etFpaNewPassword = findViewById(R.id.etFpaNewPassword);
        etFpaConfirmPassword = findViewById(R.id.etFpaConfirmNewPassword);
        tvFpaSecurityQuestion = findViewById(R.id.tvFpaSecurityQuestion);
        String receivedUsername = getIntent().getStringExtra("username");
        if (receivedUsername != null && !receivedUsername.isEmpty()) {
            etFpaUsername.setText(receivedUsername);  // Autofill username
            fetchSecurityQuestion(receivedUsername); // Fetch security question
        }

        dbHelper = new DiaryDatabaseHelper(this);
    }

    private void fetchSecurityQuestion(String username) {
        if (!dbHelper.isUsernameTaken(username)) {
            etFpaUsername.setError("Username does not exist");
            return;
        }

        storedSecurityQuestion = dbHelper.getSecurityQuestion(username);
        if (storedSecurityQuestion != null && !storedSecurityQuestion.isEmpty()) {
            tvFpaSecurityQuestion.setText("Security Question: " + storedSecurityQuestion);
        } else {
            tvFpaSecurityQuestion.setText("Security Question not found.");
        }
    }

    public void onResetPasswordClicked(View view) {
        String username = etFpaUsername.getText().toString().trim();
        String securityAnswer = etFpaSecurityAnswer.getText().toString().trim();
        String newPassword = etFpaNewPassword.getText().toString().trim();
        String confirmPassword = etFpaConfirmPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etFpaUsername.setError("Username is required");
            return;
        }

        if (!dbHelper.isUsernameTaken(username)) {
            etFpaUsername.setError("Username does not exist");
            return;
        }

        if (!dbHelper.checkSecurityAnswer(username, securityAnswer)) {
            etFpaSecurityAnswer.setError("Incorrect security answer");
            return;
        }

        if (!isValidPassword(newPassword)) {
            etFpaNewPassword.setError("Password must be at least 6 chars with an uppercase, a number, and a special char");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etFpaConfirmPassword.setError("Passwords do not match");
            return;
        }

        boolean updateSuccess = dbHelper.updateUserPassword(username, newPassword);
        if (updateSuccess) {
            Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Error resetting password", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPassword(String password) {
        Pattern PASSWORD_PATTERN = Pattern.compile(
                "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$"
        );
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
