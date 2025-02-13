package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {


    private NavigationView navigationView;
    private TextView tvUserName;
    private TextView tvUserId;
    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dlMaMainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Using Shared Preferences to check if the user is logged in or not.
        sharedPreferences = getSharedPreferences("logStatus", MODE_PRIVATE);
        boolean status = sharedPreferences.getBoolean("logStatus", false);

        if (!status) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            Toast.makeText(this, "Starting Login Activity.", Toast.LENGTH_SHORT).show();
            Log.d("Database", "Start");

            sharedPreferences.edit().putBoolean("logStatus", true).apply();
            startActivity(intent);
        }

        // ✅ Initialize Toolbar correctly
        Toolbar topToolbar = findViewById(R.id.tbMaTopToolbar);
        setSupportActionBar(topToolbar); // ✅ Fix variable name

        // ✅ Initialize DrawerLayout
        drawerLayout = findViewById(R.id.dlMaMainPage);

        // Navigation Drawer Toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, topToolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Navigation View
        navigationView = findViewById(R.id.nvMaNavDrawer);
        View headerView = navigationView.getHeaderView(0);


        // Get Username from SharedPreferences
        tvUserName = headerView.findViewById(R.id.tvUsername);
        tvUserId = headerView.findViewById(R.id.tvUserId);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", "Guest");
        int userId = sharedPreferences.getInt("userId", -1);
        Toast.makeText(this, username+" "+userId, Toast.LENGTH_LONG).show();
        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));
    }

}