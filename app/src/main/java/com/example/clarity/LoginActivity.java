package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etLaUsername, etLaPassword;
    DiaryDatabaseHelper dbHelper;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etLaUsername = findViewById(R.id.etLaUsername);
        etLaPassword = findViewById(R.id.etLaPassword);
        dbHelper = new DiaryDatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        
    }

    public void onLoginClicked(View view) {
        String username = etLaUsername.getText().toString().trim();
        String password = etLaPassword.getText().toString().trim();
        
        Toast.makeText(this, username+" "+password, Toast.LENGTH_LONG).show();


        if (dbHelper.authenticateUser(username, password)) {

            Log.d("LoginActivity", "User authenticated successfully.");
            int userId = dbHelper.getUserIdByUsername(username);

            // Save username in SharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", username);
            editor.putInt("userId", userId);
            editor.apply();

            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }



        //




    }



    //Creating Account
    public void onCreateAccountClicked (View view){
        Intent intent = new Intent(this, CreateAccountActivity.class);
        Toast.makeText(this, "Creating New Account.", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

}
