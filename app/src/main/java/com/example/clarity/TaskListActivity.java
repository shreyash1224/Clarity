package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class TaskListActivity extends AppCompatActivity {


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
        setContentView(R.layout.activity_task_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dlTlaMainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // ✅ Initialize Toolbar correctly
        Toolbar topToolbar = findViewById(R.id.tbTlaTopToolbar);
        setSupportActionBar(topToolbar); // ✅ Fix variable name

        // ✅ Initialize DrawerLayout
        drawerLayout = findViewById(R.id.dlTlaMainPage);

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
        // ✅ Fix: Check login status properly
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            // User is not logged in, redirect to LoginActivity
            Intent intent = new Intent(TaskListActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Prevent going back to MainActivity
        }

        Toast.makeText(this, username+" "+userId, Toast.LENGTH_LONG).show();
        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_profile) {

                    Toast.makeText(TaskListActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();
                } else if (item.getItemId() == R.id.nav_settings) {


                    Toast.makeText(TaskListActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
                }
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);




        //List View
        listView = findViewById(R.id.lvTlalistView); // Initialize ListView

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        List<Task> taskList = dbHelper.getAllTasks(); // Fetch all tasks

        if (taskList.isEmpty()) {
            Toast.makeText(this, "No tasks available.", Toast.LENGTH_SHORT).show();
        } else {
            TaskAdapter taskAdapter = new TaskAdapter(this, taskList);
            listView.setAdapter(taskAdapter); // Set adapter to ListView
        }


    }

    @Override
    protected void onPause() {
        super.onPause();

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);

        // Loop through all tasks and save their completion status
        for (int i = 0; i < listView.getCount(); i++) {
            Task task = (Task) listView.getItemAtPosition(i);
            dbHelper.updateTaskCompletion(task.getTaskId(), task.getCompletion());
        }
    }



    public void toMainActivity(View view) {

        Intent intent = new Intent(TaskListActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void toTaskListActivity(View view) {
        Toast.makeText(this, "Already in task list activity...", Toast.LENGTH_SHORT).show();

    }


    public void settingsActivity(MenuItem item) {
        Intent intent = new Intent(TaskListActivity.this, SettingsActivity.class);
        startActivity(intent);
    }
}