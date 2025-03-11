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

public class ReportsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView tvUserName, tvUserId;
    private ImageView profileImage;
    private SharedPreferences sharedPreferences;
    private PieChart pieChart;
    private BarChart barChart;

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
        barChart = findViewById(R.id.tasksChart);

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

                if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                    Uri imageUri;

                    if (profilePicturePath.startsWith("content://")) {
                        imageUri = Uri.parse(profilePicturePath);
                    } else {
                        imageUri = FileProvider.getUriForFile(
                                this,
                                "com.example.clarity.fileprovider",
                                new File(profilePicturePath)
                        );
                    }

                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        profileImage.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        profileImage.setImageResource(R.drawable.ic_done);
                    }
                } else {
                    profileImage.setImageResource(R.drawable.ic_done);
                }
            }
            cursor.close();
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(ReportsActivity.this, ProfileActivity.class));
            } else if (item.getItemId() == R.id.nav_settings) {
                startActivity(new Intent(ReportsActivity.this, SettingsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        loadPieChart(dbHelper, userId);
        loadBarChart(dbHelper, userId);
    }

    private void loadPieChart(DiaryDatabaseHelper dbHelper, int userId) {
        ArrayList<PieEntry> entries = dbHelper.getPieChartData(userId);
        PieDataSet pieDataSet = new PieDataSet(entries, "Expenses vs Income");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
    }

    private void loadBarChart(DiaryDatabaseHelper dbHelper, int userId) {
        ArrayList<BarEntry> entries = dbHelper.getBarChartData(userId);
        BarDataSet barDataSet = new BarDataSet(entries, "Monthly Expenses");
        barDataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.invalidate();
    }



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
    
}
