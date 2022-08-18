package com.tutorial.notesapp.manager;

import android.app.Activity;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

public class ToastManager {

    private static ToastManager instance;

    public static synchronized ToastManager getInstance() {
        if (instance == null) {
            instance = new ToastManager();
        }
        return instance;
    }

    private Toast toast = null;

    public void showToast(Activity activity, String content, Boolean isShort) {
        cancelToast();
        toast = Toast.makeText(activity, content, (isShort) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.show();
    }

    public void cancelToast() {
        if (toast != null)
            toast.cancel();
    }
}
