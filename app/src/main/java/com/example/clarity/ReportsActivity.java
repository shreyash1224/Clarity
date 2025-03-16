package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.navigation.NavigationView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvUserName, tvUserId;
    private ImageView profileImage;
    private SharedPreferences sharedPreferences;
    private PieChart pieChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        drawerLayout = findViewById(R.id.dlReportMainPage);
        navigationView = findViewById(R.id.nvReportNavDrawer);
        tvUserName = navigationView.getHeaderView(0).findViewById(R.id.tvUsername);
        tvUserId = navigationView.getHeaderView(0).findViewById(R.id.tvUserId);
        profileImage = navigationView.getHeaderView(0).findViewById(R.id.ciProfilePicture);
        pieChart = findViewById(R.id.expenseChart);

        // Replace barChart with a new PieChart for tasks
        PieChart tasksPieChart = findViewById(R.id.tasksChart);

        Toolbar topToolbar = findViewById(R.id.tbReportTopToolbar);
        setSupportActionBar(topToolbar);
        getSupportActionBar().setTitle("");

        ImageButton ibMenu = findViewById(R.id.ibReportMenu);
        ibMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            Intent intent = new Intent(ReportsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));

        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
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

        // Update Charts
        loadPieChart(dbHelper, userId);
        loadTasksPieChart(dbHelper, userId, tasksPieChart); // Updated function call
        loadExpenseCategoryText(dbHelper, userId);
        loadExpenseVsSavingsPieChart(dbHelper, userId);
        loadSummaryInfo(dbHelper, userId);
    }

    // Updated loadTasksPieChart function
    private void loadTasksPieChart(DiaryDatabaseHelper dbHelper, int userId, PieChart tasksPieChart) {
        int pendingTasks = dbHelper.getPendingTasks(userId);
        int completedTasks = dbHelper.getCompletedTasks(userId);

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(pendingTasks, "Pending"));
        entries.add(new PieEntry(completedTasks, "Completed"));

        PieDataSet pieDataSet = new PieDataSet(entries, "Task Status");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(pieDataSet);

        tasksPieChart.setData(pieData);
        tasksPieChart.invalidate();
    }

    private void loadPieChart(DiaryDatabaseHelper dbHelper, int userId) {
        ArrayList<PieEntry> entries = dbHelper.getPieChartData(userId);
        PieDataSet pieDataSet = new PieDataSet(entries, "Expenses vs Income");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

//    private void loadBarChart(DiaryDatabaseHelper dbHelper, int userId) {
//        ArrayList<BarEntry> entries = dbHelper.getBarChartData(userId);
//        BarDataSet barDataSet = new BarDataSet(entries, "Monthly Expenses");
//        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
//        BarData barData = new BarData(barDataSet);
//        barChart.setData(barData);
//        barChart.invalidate();
//    }



    public void settingsActivity(MenuItem item) {
        Intent intent = new Intent(ReportsActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void profileActivity(MenuItem item) {
        Intent intent = new Intent(ReportsActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    public void trashActivity(MenuItem item) {
        Intent intent = new Intent(ReportsActivity.this, TrashActivity.class);
        startActivity(intent);
    }


    public void logout(View view) {

        Log.d("MainActivity", "Logging out...");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(ReportsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    public void ReportActivity(View view) {
        Intent intent = new Intent(ReportsActivity.this, ReportsActivity.class);
        startActivity(intent);
    }

    public void toExpenseActivity(View view) {
        Intent intent = new Intent(ReportsActivity.this, ExpenseManagerActivity.class);
        startActivity(intent);
    }

    public void toMainActivity(View view) {

        Intent intent = new Intent(ReportsActivity.this, MainActivity.class);
        startActivity(intent);
    }

    public void toTaskListActivity(View view) {
        Intent intent = new Intent(ReportsActivity.this, TaskListActivity.class);
        startActivity(intent);


    }


    public void toToolActivity(View view) {
        Intent intent = new Intent(ReportsActivity.this, ToolsActivity.class);
        startActivity(intent);


    }


    //

    private void loadExpenseCategoryText(DiaryDatabaseHelper dbHelper, int userId) {
        List<String> topCategories = dbHelper.getTopExpenseCategories(userId);
        TextView tvCategory1 = findViewById(R.id.tvTopExpense1);
        TextView tvCategory2 = findViewById(R.id.tvTopExpense2);
        TextView tvCategory3 = findViewById(R.id.tvTopExpense3);


        if (topCategories.size() > 0) tvCategory1.setText(topCategories.get(0));
        if (topCategories.size() > 1) tvCategory2.setText(topCategories.get(1));
        if (topCategories.size() > 2) tvCategory3.setText(topCategories.get(2));
    }

    private void loadExpenseVsSavingsPieChart(DiaryDatabaseHelper dbHelper, int userId) {
        float expenses = dbHelper.getTotalExpenses(userId);
        float savings = dbHelper.getTotalSavings(userId);

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(expenses, "Expenses"));
        entries.add(new PieEntry(savings, "Savings"));

        PieDataSet pieDataSet = new PieDataSet(entries, "Total Transactions");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void loadTasksPieChart(DiaryDatabaseHelper dbHelper, int userId) {
        int pendingTasks = dbHelper.getPendingTasks(userId);
        int completedTasks = dbHelper.getCompletedTasks(userId);

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(pendingTasks, "Pending"));
        entries.add(new PieEntry(completedTasks, "Completed"));

        PieDataSet pieDataSet = new PieDataSet(entries, "Total Tasks");
        pieDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();

    }

    private void loadSummaryInfo(DiaryDatabaseHelper dbHelper, int userId) {
        TextView tvTotalImages = findViewById(R.id.totalImages);
        TextView tvTotalTasks = findViewById(R.id.totalTasksInfo);
        TextView tvTotalTextBlocks = findViewById(R.id.totalTextBlocks);
        TextView tvTotalPages = findViewById(R.id.totalPages);
        TextView totalTasks = findViewById(R.id.totalTasks);


        tvTotalImages.setText("Total Images: " + dbHelper.getTotalImages(userId));
        tvTotalTasks.setText("Total Tasks: " + dbHelper.getTotalTasks(userId));
        totalTasks.setText("Total Tasks: " + dbHelper.getTotalTasks(userId));

        tvTotalTextBlocks.setText("Total Text Blocks: " + dbHelper.getTotalTextBlocks(userId));
        tvTotalPages.setText("Total Pages: " + dbHelper.getTotalPages(userId));
    }


}
