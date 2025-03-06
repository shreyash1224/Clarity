package com.example.clarity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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

import java.util.ArrayList;
import java.util.List;

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
                drawerLayout.closeDrawer(GravityCompat.START);  // Close drawer if open
            } else {
                drawerLayout.openDrawer(GravityCompat.START);  // Open drawer if closed
            }
        });

        // Navigation Header Setup
        View headerView = navigationView.getHeaderView(0);
        tvUserName = headerView.findViewById(R.id.tvUsername);
        tvUserId = headerView.findViewById(R.id.tvUserId);





        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        int userId = sharedPreferences.getInt("userId", -1);

        if (username == null || userId == -1) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        Toast.makeText(this, username + " " + userId, Toast.LENGTH_LONG).show();
        tvUserName.setText(username);
        tvUserId.setText(String.valueOf(userId));

        // Handle navigation item clicks
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
                int pageId = pages.get(position).getPageId();
                Intent intent = new Intent(MainActivity.this, DiaryPageActivity.class);
                intent.putExtra("pageId", pageId);
                startActivity(intent);
                Toast.makeText(MainActivity.this, ""+pageId, Toast.LENGTH_SHORT).show();


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
}