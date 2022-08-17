package com.tutorial.notesapp.activity;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;

import com.tutorial.notesapp.adapter.NotesAdapter;
import com.tutorial.notesapp.databinding.ActivityMainBinding;
import com.tutorial.notesapp.room.database.NotesDatabase;
import com.tutorial.notesapp.room.entities.Note;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
        getNotes();
        setListNotes();
    }

    private void setListener() {
        binding.imgAddNoteMain.setOnClickListener(view ->
                addNote.launch(new Intent(this, CreateNoteActivity.class)));
    }

    ActivityResultLauncher<Intent> addNote  = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                // Handle the Intent
                getNotes();
            }
        }
    });

    private void setListNotes() {
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList);

        binding.rvNotes.setLayoutManager(new StaggeredGridLayoutManager(
                2, StaggeredGridLayoutManager.VERTICAL
        ));
        binding.rvNotes.setAdapter(notesAdapter);
    }

    private void getNotes() {
        class SaveNoteTask extends AsyncTask<Void, Void, List<Note>> {
            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                Log.e("MY_NOTES", notes.toString());
                if (noteList.size() == 0) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else {
                    // 最新的資料在最上面
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                }
                binding.rvNotes.smoothScrollToPosition(0);
            }
        }

        new SaveNoteTask().execute();
    }
}