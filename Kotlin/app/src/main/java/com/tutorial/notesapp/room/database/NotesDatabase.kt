package com.tutorial.notesapp.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tutorial.notesapp.room.dao.NoteDao
import com.tutorial.notesapp.room.entiries.Note

@Database(entities = [Note::class], version = 2, exportSchema = false)
abstract class NotesDatabase: RoomDatabase() {
    abstract fun noteDao(): NoteDao
}