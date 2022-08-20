package com.tutorial.notesapp.manager

import android.content.Context
import androidx.room.Room
import com.tutorial.notesapp.room.database.NotesDatabase
import com.tutorial.notesapp.tools.Notes_Database

class DataManger private constructor(){
    companion object {
        val instance: DataManger by lazy { DataManger() }
    }

    private var notesDatabase: NotesDatabase? = null

    fun initDatabase(context: Context): NotesDatabase? {
        notesDatabase = Room.databaseBuilder(context, NotesDatabase::class.java, Notes_Database)
            .fallbackToDestructiveMigration()
            .build()

        return notesDatabase
    }

    fun closeDatabase() {
        notesDatabase?.close()
        notesDatabase = null
    }
}