package com.tutorial.notesapp.manager

import android.app.Activity
import android.widget.Toast

class ToastManager private constructor(){
    companion object {
        val instance: ToastManager by lazy { ToastManager() }
    }

    private var toast: Toast? = null

    fun showToast(activity: Activity, content: String, isShort: Boolean) {
        cancelToast()
        toast = Toast.makeText(activity, content, if (isShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG)
        toast?.show()
    }

    private fun cancelToast() = toast?.cancel()
}