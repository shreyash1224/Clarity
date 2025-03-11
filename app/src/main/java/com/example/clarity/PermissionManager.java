package com.example.clarity;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {

    public static final int NOTIFICATION_PERMISSION_CODE = 101;

    // ✅ Check if notification permission is granted
    public static boolean hasNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true; // No permission needed for Android 12 or below
        }

        // ✅ Prevent crash for Android < 33
        return ContextCompat.checkSelfPermission(activity,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        android.Manifest.permission.POST_NOTIFICATIONS : "")
                == PackageManager.PERMISSION_GRANTED;
    }

    // ✅ Request notification permission if not granted
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.POST_NOTIFICATIONS)) {
                // ✅ Only request if not denied previously
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            } else {
                // ✅ Optional: Show custom message explaining why permission is needed
                // OR simply do nothing to avoid annoying the user.
            }
        }
    }

    // ✅ Handle permission result in the calling Activity
}
