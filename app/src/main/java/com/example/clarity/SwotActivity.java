package com.example.clarity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class SwotActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    private static final String TAG = "SwotActivity";
    private int userId = 1;  // Replace with actual user ID
    private int pageId = -1; // Will be retrieved from DB
    private DiaryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swot);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);
        Log.d("Swot",""+userId);

        dbHelper = new DiaryDatabaseHelper(this);
        pageId = dbHelper.getSwotPageId(userId);

        List<Pair<String, String>> swotData = new ArrayList<>();
        if (pageId != -1) {
            swotData = dbHelper.getResourcesByPageId(pageId);
        }

        String strengthContent = "", weaknessContent = "", opportunityContent = "", threatContent = "";

        for (Pair<String, String> entry : swotData) {
            switch (entry.first) {
                case "text":
                case "swotStrength":
                    strengthContent = entry.second;
                    break;
                case "swotWeakness":
                    weaknessContent = entry.second;
                    break;
                case "swotOpportunity":
                    opportunityContent = entry.second;
                    break;
                case "swotThreat":
                    threatContent = entry.second;
                    break;
            }
        }

        loadFragment(R.id.fragment_strength, "Strength", strengthContent);
        loadFragment(R.id.fragment_weakness, "Weakness", weaknessContent);
        loadFragment(R.id.fragment_opportunity, "Opportunity", opportunityContent);
        loadFragment(R.id.fragment_threat, "Threat", threatContent);
    }


    private void loadFragment(int containerId, String type, String content) {
        SwotFragment fragment = new SwotFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("content", content);  // Pass the retrieved content
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(containerId, fragment)
                .commit();
    }


    @Override
    protected void onPause() {
        super.onPause();
        saveSwotData();
//        dbHelper.logSwotResources(pageId);
    }

    private void saveSwotData() {
        // Check if there is an existing SWOT page for this user
        pageId = dbHelper.getSwotPageId(userId);
        Toast.makeText(this, ""+pageId, Toast.LENGTH_SHORT).show();

        if (pageId == -1) {
            Log.e(TAG, "No SWOT page found for userId " + userId + ". Creating a new one.");
            pageId = dbHelper.createSwotPage(userId);
        }

        List<Resource> resourceList = List.of(
                createResource("Strength", 1),
                createResource("Weakness", 2),
                createResource("Opportunity", 3),
                createResource("Threat", 4)
        );

        // Update or create resources in the database
        pageId = dbHelper.updatePage(pageId, "SWOT", resourceList, userId);
    }



    private Resource createResource(String type, int order) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentId(type));
        if (fragment instanceof SwotFragment) {
            SwotFragment swotFragment = (SwotFragment) fragment;
            String content = swotFragment.getContent().trim(); // Remove leading/trailing spaces

            // Ensure content is not empty, else provide a default placeholder
            if (content.isEmpty()) {
                content = "[No Content]"; // Placeholder text to avoid SQLite constraint failure
            }

            String resourceType = "swot" + type; // Format as "swotStrength", "swotWeakness", etc.

            Log.d(TAG, type + " Content (Saved): " + content);
            return new Resource(pageId, resourceType, content, order);
        } else {
            Log.e(TAG, "Fragment not found for " + type);
            return new Resource(pageId, "swot" + type, "[No Content]", order); // Avoid empty content
        }
    }


    private int getFragmentId(String type) {
        switch (type) {
            case "Strength":
                return R.id.fragment_strength;
            case "Weakness":
                return R.id.fragment_weakness;
            case "Opportunity":
                return R.id.fragment_opportunity;
            case "Threat":
                return R.id.fragment_threat;
            default:
                throw new IllegalArgumentException("Invalid SWOT type: " + type);
        }
    }
}
