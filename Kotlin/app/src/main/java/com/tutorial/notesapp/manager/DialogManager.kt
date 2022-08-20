package com.tutorial.notesapp.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import java.lang.Exception

class DialogManager private constructor() {
    companion object {
        val instance: DialogManager by lazy { DialogManager() }
    }

    private var dialog: AlertDialog? = null

    fun showCustomDialog(activity: Activity, layout: View, keyboard: Boolean = false): View? {
        if (!activity.isDestroyed) {
            try {
                cancelDialog()

                dialog = AlertDialog.Builder(activity).create()
                dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog?.show()

                if (keyboard) {
                    dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

                    dialog?.setOnDismissListener {
                        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)

                        var virtualKeyboardHeight = 0
                        val res = activity.resources
                        val resourceId = activity.resources.getIdentifier("navigation_bar_height", "dimen", "android")
                        if (resourceId > 0) virtualKeyboardHeight = res.getDimensionPixelSize(resourceId)

                        val rect = Rect()
                        activity.window.decorView.getWindowVisibleDisplayFrame(rect)
                        val screenHeight = activity.window.decorView.rootView.height
                        val heiDifference = screenHeight - (rect.bottom + virtualKeyboardHeight)

                        if (heiDifference > 0) {
                            val inputMgr = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMgr.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0)
                        }
                    }
                }
                dialog?.setContentView(layout)
                return layout
            } catch (e: Exception) {
                ToastManager.instance.showToast(activity, "發生一些錯誤", true)
                e.printStackTrace()
                return null
            }
        }
        return null
    }

    fun isShow(): Boolean {
        return dialog?.isShowing ?: false
    }

    fun cancelDialog() {
        dialog?.dismiss()
    }
}