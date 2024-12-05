package com.saif.mymusic;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MyMusicPermissions {

    public static final int REQUEST_MEDIA_PERMISSION = 100;
    private static final String PREFS_NAME = "MyMusicPrefs";
    private static final String KEY_PERMISSION_GRANTED = "PermissionGranted";

    public boolean isMediaPermissionGranted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PERMISSION_GRANTED, false) || isPermissionGrantedFromSystem(context);
    }

    public void requestMediaPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_MEDIA_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_MEDIA_PERMISSION);
        }
    }

    public void setPermissionGranted(Context context, boolean granted) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PERMISSION_GRANTED, granted).apply();
    }

    private boolean isPermissionGrantedFromSystem(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
}
