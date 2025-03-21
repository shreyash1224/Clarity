package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {


    private NavigationView navigationView;
    private TextView tvUserName;
    private TextView tvUserId;

    CircleImageView profileImage;

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

        // Shared Preferences for login check
        sharedPreferences = getSharedPreferences("logStatus", MODE_PRIVATE);
        boolean status = sharedPreferences.getBoolean("logStatus", false);

        if (!status) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            Toast.makeText(this, "Starting Login Activity.", Toast.LENGTH_SHORT).show();
            sharedPreferences.edit().putBoolean("logStatus", true).apply();
            startActivity(intent);
        }

        // Initialize Toolbar
        Toolbar topToolbar = findViewById(R.id.tbMaTopToolbar);
        setSupportActionBar(topToolbar);
        getSupportActionBar().setTitle("");  // Removes the title text

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.dlMaMainPage);
        navigationView = findViewById(R.id.nvMaNavDrawer);

        // Get ibMenu Button and set click listener
        ImageButton ibMenu = findViewById(R.id.ibMenu);
        ibMenu.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ✅ Navigation Header Setup (Correct Way)
        View headerView = navigationView.getHeaderView(0);
        tvUserName = headerView.findViewById(R.id.tvUsername);
        tvUserId = headerView.findViewById(R.id.tvUserId);
        profileImage = headerView.findViewById(R.id.ciProfilePicture);

        // ✅ Fetch user details from SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        // ✅ Set username and userId in Nav Header
        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));

        // ✅ Fetch Profile Picture from Database
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        Cursor cursor = dbHelper.getUserProfilePicture(userId);

        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndex("profilePicture");

            if (columnIndex != -1) {
                String profilePicturePath = cursor.getString(columnIndex);
                Log.e("ProfilePicture", "Retrieved Profile Picture Path: " + profilePicturePath);

                if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                    if (profilePicturePath.startsWith("content://")) {
                        // ✅ It's a content URI, use ContentResolver
                        Log.e("ProfilePicture", "Profile picture is a content URI.");
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(Uri.parse(profilePicturePath));
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            profileImage.setImageBitmap(bitmap);
                            Log.e("ProfilePicture", "Profile picture loaded from content URI.");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Log.e("ProfilePicture", "Failed to load profile picture from URI.");
                            profileImage.setImageResource(R.drawable.ic_done); // Fallback image
                        }
                    } else {
                        // ✅ It's a direct file path, use BitmapFactory
                        Log.e("ProfilePicture", "Profile picture is a file path.");
                        File imgFile = new File(profilePicturePath);

                        if (imgFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                            profileImage.setImageBitmap(bitmap);
                            Log.e("ProfilePicture", "Profile picture loaded from file.");
                        } else {
                            Log.e("ProfilePicture", "Profile picture file does not exist at: " + profilePicturePath);
                            profileImage.setImageResource(R.drawable.ic_done); // Fallback image
                        }
                    }
                } else {
                    Log.e("ProfilePicture", "Profile picture path is null or empty.");
                    profileImage.setImageResource(R.drawable.ic_done); // Fallback image
                }
            }
            cursor.close();
        }

        // ✅ Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_profile) {
                Toast.makeText(MainActivity.this, "Profile Selected", Toast.LENGTH_SHORT).show();
            } else if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(MainActivity.this, "Settings Selected", Toast.LENGTH_SHORT).show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        listView = findViewById(R.id.lvMalistView);
    }

    //Logging out.
    public void logout(View view) {

        Log.d("MainActivity", "Logging out...");
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void addPage(View view) {
        Toast.makeText(this, "Adding Page.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, DiaryPageActivity.class);
        startActivity(intent);
    }

    private void loadPages() {
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("userId", -1);
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);
        ArrayList<DiaryPage> pages = dbHelper.getAllPages(userId);

        Log.d("MainActivity", "Loading " + pages.size() + " pages into ListView.");

        if (pages.isEmpty()) {
            Toast.makeText(this, "No diary pages found.", Toast.LENGTH_SHORT).show();
        }

        DiaryPageAdapter adapter = new DiaryPageAdapter(this, pages);
        listView.setAdapter(adapter);

        // Open a diary page when clicked
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DiaryPage selectedPage = pages.get(position);
                int pageId = selectedPage.getPageId();
                String pageTitle = selectedPage.getPageTitle(); // Get page title

                // Prevent opening the SWOT page
                if ("SWOT".equalsIgnoreCase(pageTitle)) {
                    Toast.makeText(MainActivity.this, "SWOT page cannot be opened here", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, SwotActivity.class);
                    startActivity(intent);
                } else{
                    // Open the selected diary page
                    Intent intent = new Intent(MainActivity.this, DiaryPageActivity.class);
                    intent.putExtra("pageId", pageId);
                    startActivity(intent);
                    Toast.makeText(MainActivity.this, "Opening page: " + pageId, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "Loading Pages", Toast.LENGTH_SHORT).show();
        loadPages();  // Refresh pages when MainActivity is resumed
    }

    public void settingsActivity(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void toMainActivity(View view) {
        Toast.makeText(this, "Already in main activity...", Toast.LENGTH_SHORT).show();
    }

    public void toTaskListActivity(View view) {
        Intent intent = new Intent(MainActivity.this, TaskListActivity.class);
        startActivity(intent);
    }

    public void searchPage(View view) {
        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_search_page, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Initialize views
        EditText etSearchPage = dialogView.findViewById(R.id.etSearchPage);
        ListView lvSearchResults = dialogView.findViewById(R.id.lvSearchResults);

        // Fetch all pages from database
        DiaryDatabaseHelper dbHelper = new DiaryDatabaseHelper(this);

        int userId = sharedPreferences.getInt("userId", -1);
        List<DiaryPage> allPages = dbHelper.getAllPages(userId);  // Ensure this method exists in DB Helper

        // Setup adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        lvSearchResults.setAdapter(adapter);

        // Search function
        etSearchPage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.clear();
                String query = s.toString().trim().toLowerCase();
                for (DiaryPage page : allPages) {
                    if (page.getPageTitle().toLowerCase().contains(query) || String.valueOf(page.getPageId()).equals(query)) {
                        adapter.add(page.getPageTitle() + " (ID: " + page.getPageId() + ")");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Handle page selection
        lvSearchResults.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedItem = adapter.getItem(position);
            int pageId = extractPageId(selectedItem); // Extract ID from the selected string

            // Open the selected page
            Intent intent = new Intent(MainActivity.this, DiaryPageActivity.class);
            intent.putExtra("pageId", pageId);
            startActivity(intent);

            dialog.dismiss();
        });
    }

    // Extract pageId from list item text
    private int extractPageId(String text) {
        String idPart = text.substring(text.lastIndexOf("ID: ") + 4, text.length() - 1);
        return Integer.parseInt(idPart);
    }

    public void toToolActivity(View view) {
        Toast.makeText(this, "Clicked on Tools Activity.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, ToolsActivity.class);
        startActivity(intent);
    }


    public void profileActivity(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    public void trashActivity(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, TrashActivity.class);
        startActivity(intent);
    }

    public void ReportActivity(View view) {
        Intent intent = new Intent(MainActivity.this, ReportsActivity.class);
        startActivity(intent);
    }
}