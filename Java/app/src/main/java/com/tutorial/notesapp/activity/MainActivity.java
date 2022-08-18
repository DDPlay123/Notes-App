package com.tutorial.notesapp.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.tutorial.notesapp.adapter.Notes.NotesAdapter;
import com.tutorial.notesapp.databinding.ActivityMainBinding;
import com.tutorial.notesapp.databinding.DialogAddUrlBinding;
import com.tutorial.notesapp.manager.ToastManager;
import com.tutorial.notesapp.room.database.NotesDatabase;
import com.tutorial.notesapp.room.entities.Note;
import com.tutorial.notesapp.tools.Method;
import com.tutorial.notesapp.tools.Reference;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

enum noteStatus {
    ShowNote, NewNote, UpdateNote
}

public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;

    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private int selectedNoteId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        setListener();
        getNotes(noteStatus.ShowNote, false);
        setSearchListener();
        setListNotes();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Reference.Request_PERMISSION && grantResults.length > 0)
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                selectImage();
            else
                ToastManager.getInstance().showToast(this, "權限不足!", true);
    }

    private void setListener() {
        binding.imgAddNoteMain.setOnClickListener(view -> newNote.launch(new Intent(this, CreateNoteActivity.class)));

        binding.imgAddNote.setOnClickListener(view -> newNote.launch(new Intent(this, CreateNoteActivity.class)));

        binding.imgAddImage.setOnClickListener(view -> {
            if (!Method.hasPermissions(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE))
                Method.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            else
                selectImage();
        });

        binding.imgAddWebLink.setOnClickListener(view -> showAddURLDialog());
    }

    ActivityResultLauncher<Intent> newNote = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            getNotes(noteStatus.NewNote, false);
        }
    });

    private void setListNotes() {
        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, (note, position) -> {
            selectedNoteId = position;
            Intent intent = new Intent(this, CreateNoteActivity.class);
            intent.putExtra("isViewOrUpdate", true);
            intent.putExtra("note", note);
            updateNote.launch(intent);
        });

        binding.rvNotes.setLayoutManager(new StaggeredGridLayoutManager(
                2, StaggeredGridLayoutManager.VERTICAL
        ));
        binding.rvNotes.setAdapter(notesAdapter);
    }

    ActivityResultLauncher<Intent> updateNote = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Intent intent = result.getData();
            getNotes(noteStatus.UpdateNote, intent.getBooleanExtra("isDeleted", false));
        }
    });

    private void getNotes(noteStatus type, Boolean isDeleted) {
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
                switch (type) {
                    case ShowNote:
                        noteList.addAll(notes);
                        notesAdapter.notifyDataSetChanged();
                        break;
                    case NewNote:
                        noteList.add(0, notes.get(0));
                        notesAdapter.notifyItemInserted(0);
                        binding.rvNotes.smoothScrollToPosition(0);
                        break;
                    case UpdateNote:
                        noteList.remove(selectedNoteId);
                        if (isDeleted)
                            notesAdapter.notifyItemRemoved(selectedNoteId);
                        else {
                            noteList.add(selectedNoteId, notes.get(selectedNoteId));
                            notesAdapter.notifyItemInserted(selectedNoteId);
                        }
                        notesAdapter.notifyDataSetChanged();
                        break;
                }
            }
        }

        new SaveNoteTask().execute();
    }

    private void setSearchListener() {
        binding.edSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0)
                    notesAdapter.searchNotes(editable.toString());
            }
        });
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            mGetImage.launch(intent);
        }
    }

    ActivityResultLauncher<Intent> mGetImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            try {
                String selectedImageFilePath = getPathFromUri(result.getData().getData());
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                intent.putExtra("isFromQuickActions", true);
                intent.putExtra("quickActionType", "image");
                intent.putExtra("imagePath", selectedImageFilePath);
                newNote.launch(intent);

            } catch (Exception e) {
                e.printStackTrace();
                ToastManager.getInstance().showToast(this, "發生錯誤，請稍後嘗試", true);
            }
        }
    });

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null)
            filePath = contentUri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void showAddURLDialog() {
        AlertDialog dialogAddURL;

        DialogAddUrlBinding _binding = DialogAddUrlBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(_binding.getRoot());

        dialogAddURL = builder.create();
        dialogAddURL.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogAddURL.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialogAddURL.show();

        _binding.tvAdd.setOnClickListener(view -> {
            if (_binding.edAddUrl.getText().toString().trim().isEmpty())
                ToastManager.getInstance().showToast(this, "請輸入網址", true);
            else if (!Patterns.WEB_URL.matcher(_binding.edAddUrl.getText().toString()).matches())
                ToastManager.getInstance().showToast(this, "請輸入正確的網址", true);
            else {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                intent.putExtra("isFromQuickActions", true);
                intent.putExtra("quickActionType", "URL");
                intent.putExtra("URL", _binding.edAddUrl.getText().toString());
                newNote.launch(intent);
                dialogAddURL.dismiss();
            }
        });

        _binding.tvCancel.setOnClickListener(view -> dialogAddURL.dismiss());
    }
}