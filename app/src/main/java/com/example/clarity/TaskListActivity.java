package com.example.clarity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TaskListActivity extends AppCompatActivity {

    CircleImageView profileImage;


    private NavigationView navigationView;
    private TextView tvUserName;
    private TextView tvUserId;
    private SharedPreferences sharedPreferences;
    private DrawerLayout drawerLayout;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dlTlaMainPage), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Toolbar topToolbar = findViewById(R.id.tbTlaTopToolbar);
        setSupportActionBar(topToolbar);
        getSupportActionBar().setTitle("");

        // ✅ Initialize DrawerLayout
        drawerLayout = findViewById(R.id.dlTlaMainPage);

        // ✅ Initialize Navigation Drawer
        ImageButton ibMenu = findViewById(R.id.ibMenu);
        ibMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navigationView = findViewById(R.id.nvMaNavDrawer);
        View headerView = navigationView.getHeaderView(0);

        // ✅ Get Username from SharedPreferences
        tvUserName = headerView.findViewById(R.id.tvUsername);
        tvUserId = headerView.findViewById(R.id.tvUserId);
        profileImage = headerView.findViewById(R.id.ciProfilePicture);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            Intent intent = new Intent(TaskListActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));


        // ✅ Initialize ListView
        listView = findViewById(R.id.lvTlalistView); // Replace with actual ListView ID

        // ✅ Load Tasks from Database
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        List<Task> taskList = dbHelper.getAllTasks(userId);

        // ✅ Set Adapter
        TaskAdapter adapter = new TaskAdapter(this, taskList);
        listView.setAdapter(adapter);



        Cursor cursor = dbHelper.getUserProfilePicture(userId);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("profilePicture");

            if (columnIndex != -1) {
                String profilePicturePath = cursor.getString(columnIndex);
                Log.e("ProfilePicture", "Retrieved Profile Picture Path: " + profilePicturePath);

                if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                    Uri imageUri;

                    // ✅ Check if it's already a content URI
                    if (profilePicturePath.startsWith("content://")) {
                        imageUri = Uri.parse(profilePicturePath);
                    } else {
                        // ✅ If it's a file path, convert it to URI
                        imageUri = FileProvider.getUriForFile(
                                this,
                                "com.yourpackagename.fileprovider",  // ✅ Replace with your package name
                                new File(profilePicturePath)
                        );
                    }

                    try {
                        // ✅ Load image using ContentResolver
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        profileImage.setImageBitmap(bitmap);
                        Log.e("ProfilePicture", "Profile picture loaded successfully.");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e("ProfilePicture", "Failed to load profile picture.");
                        profileImage.setImageResource(R.drawable.ic_done); // Fallback image
                    }
                } else {
                    Log.e("ProfilePicture", "Profile picture path is null or empty.");
                    profileImage.setImageResource(R.drawable.ic_done); // Fallback image
                }
            }
            cursor.close();
        }



        // ✅ Handle Navigation Clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                Toast.makeText(TaskListActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(TaskListActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (listView == null) {
            Log.e("TaskListActivity", "listView is null, skipping task update.");
            return;
        }

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);

        for (int i = 0; i < listView.getCount(); i++) {
            Task task = (Task) listView.getItemAtPosition(i);
            dbHelper.updateTaskCompletion(task.getTaskId(), task.getCompletion());
        }
    }


    public void logout(View view) {
        Log.d("TaskListActivity", "Logging out...");

        // Ensure sharedPreferences is initialized
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(TaskListActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    public void test(View view) {
        TaskAdapter.ViewHolder holder = (TaskAdapter.ViewHolder) view.getTag(); // Get ViewHolder
        if (holder != null && holder.task != null) {
            Task task = holder.task; // Retrieve Task from ViewHolder
            int taskId = task.getTaskId(); // Get taskId from Task
            DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
            int pageId = dbHelper.getPageIdByTaskId(taskId);
            Toast.makeText(this, "Test " + pageId, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TaskListActivity.this, DiaryPageActivity.class);
            intent.putExtra("pageId", pageId);
            startActivity(intent);


        } else {
            Toast.makeText(this, "Task not found!", Toast.LENGTH_SHORT).show();
        }
    }

    public void sortTasks(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.sort_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(TaskListActivity.this);
            List<Task> taskList;

            if (item.getItemId() == R.id.sort_all) {
                // Show all tasks
                int userId = sharedPreferences.getInt("userId", -1);

                taskList = dbHelper.getAllTasks(userId);
                Toast.makeText(this, "Showing all tasks", Toast.LENGTH_SHORT).show();
                updateTaskList(taskList);
            } else if (item.getItemId() == R.id.sort_by_date) {
                // Open Date Picker
                showDatePicker();
            } else {
                return false;
            }
            return true;
        });

        popupMenu.show();
    }
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Format selected date
            String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;

            // Fetch tasks that match the date range
            DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(TaskListActivity.this);
            Log.d("DB_DEBUG", "Calling getTasksByDate with date: " + selectedDate);
            List<Task> filteredTasks = dbHelper.getTasksByDate(selectedDate);

            if (filteredTasks.isEmpty()) {
                Toast.makeText(this, "No tasks found for this date", Toast.LENGTH_SHORT).show();
            }

            // Update ListView with filtered tasks
            updateTaskList(filteredTasks);

        }, year, month, day);

        datePickerDialog.show();


    }

    private void updateTaskList(List<Task> taskList) {
        TaskAdapter adapter = new TaskAdapter(this, taskList);
        listView.setAdapter(adapter);
    }


    public void toToolActivity(View view) {
        Toast.makeText(this, "Clicked on Tools Activity.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(TaskListActivity.this, ToolsActivity.class);
        startActivity(intent);
    }

    public void profileActivity(MenuItem item) {
        Intent intent = new Intent(TaskListActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    public void trashActivity(MenuItem item) {
        Intent intent = new Intent(TaskListActivity.this, TrashActivity.class);
        startActivity(intent);
    }

    public void ReportActivity(View view) {
        Intent intent = new Intent(TaskListActivity.this, ReportsActivity.class);
        startActivity(intent);
    }

}