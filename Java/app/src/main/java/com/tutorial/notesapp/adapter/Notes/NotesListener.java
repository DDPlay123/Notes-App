package com.tutorial.notesapp.adapter.Notes;

import com.tutorial.notesapp.room.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
