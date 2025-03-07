package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public class ToolsActivity extends AppCompatActivity {

    private NavigationView navigationView;
    private TextView tvUserName;
    private TextView tvUserId;
    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dlToolsMainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar topToolbar = findViewById(R.id.tbToolsTopToolbar);
        setSupportActionBar(topToolbar);
        getSupportActionBar().setTitle("");

        // ✅ Initialize DrawerLayout
        drawerLayout = findViewById(R.id.dlToolsMainPage);

        // ✅ Initialize Navigation Drawer
        ImageButton ibMenu = findViewById(R.id.ibToolsMenu);
        ibMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView = findViewById(R.id.nvToolsNavDrawer);
        View headerView = navigationView.getHeaderView(0);

        // ✅ Get Username from SharedPreferences
        tvUserName = headerView.findViewById(R.id.tvUsername);
        tvUserId = headerView.findViewById(R.id.tvUserId);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            Intent intent = new Intent(ToolsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));

        // ✅ Handle Navigation Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                Toast.makeText(ToolsActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(ToolsActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }


    public void logout(View view) {

        Log.d("MainActivity", "Logging out...");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(ToolsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    public void settingsActivity(MenuItem item) {
        Intent intent = new Intent(ToolsActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void toMainActivity(View view) {

        Intent intent = new Intent(ToolsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void toTaskListActivity(View view) {
        Intent intent = new Intent(ToolsActivity.this, TaskListActivity.class);
        startActivity(intent);


    }

    public void toToolActivity(View view) {
        Toast.makeText(this, "Clicked on Tools Activity.", Toast.LENGTH_SHORT).show();
    }

    public void toSwotActivity(View view) {
        Intent intent = new Intent(ToolsActivity.this, SwotActivity.class);
//        intent.putExtra("userId", tvUserId.getText().toString());
        startActivity(intent);

    }

    public void toExpenseActivity(View view) {
        Toast.makeText(this, "To Expense Activity.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ToolsActivity.this, ExpenseManagerActivity.class);
        startActivity(intent);
    }
}
