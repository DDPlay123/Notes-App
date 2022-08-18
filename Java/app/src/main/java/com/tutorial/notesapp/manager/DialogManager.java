package com.tutorial.notesapp.manager;

import android.app.AlertDialog;

public class DialogManager {

    private static DialogManager instance;

    public static synchronized DialogManager getInstance() {
        if (instance == null) {
            instance = new DialogManager();
        }
        return instance;
    }

    private AlertDialog dialog;

    public Boolean isShow() {
        if (dialog == null)
            return false;
        return dialog.isShowing();
    }

    public void cancelDialog() {
        if (dialog != null)
            dialog.dismiss();
    }
}
