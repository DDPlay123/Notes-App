package com.tutorial.notesapp.activity

import androidx.appcompat.app.AppCompatActivity
import com.tutorial.notesapp.manager.DataManger

abstract class BaseActivity: AppCompatActivity() {

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level <= TRIM_MEMORY_BACKGROUND)
            System.gc()
    }

    override fun onStop() {
        super.onStop()
        DataManger.instance.closeDatabase()
    }

    override fun onDestroy() {
        super.onDestroy()
        DataManger.instance.closeDatabase()
    }
}