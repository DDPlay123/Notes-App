package com.tutorial.notesapp.tools;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class Method {

    public static void requestPermission(Activity activity, String... permissions) {
        if (!hasPermissions(activity, permissions))
            ActivityCompat.requestPermissions(activity, permissions, Reference.Request_PERMISSION);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }
}
