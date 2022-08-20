package com.tutorial.notesapp.adapter.Notes

import com.tutorial.notesapp.room.entiries.Note

interface NotesListener {
    fun onNoteClicked(note: Note, position: Int)
}