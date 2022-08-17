package com.tutorial.notesapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.tutorial.notesapp.R;
import com.tutorial.notesapp.databinding.ActivityCreateNoteBinding;
import com.tutorial.notesapp.databinding.LayoutMiscellaneousBinding;
import com.tutorial.notesapp.manager.ToastManager;
import com.tutorial.notesapp.room.database.NotesDatabase;
import com.tutorial.notesapp.room.entities.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {
    private ActivityCreateNoteBinding binding;

    private String selectedNoteColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        selectedNoteColor = "#333333"; // Default note color

        setDateTime();
        setListener();

        setSubtitleIndicatorColor();
        initMiscellaneous();
    }

    private void setDateTime() {
        binding.tvDateTime.setText(new SimpleDateFormat(
                "yyyy MMMM dd, EEEE a hh:mm",
                Locale.TAIWAN
        ).format(new Date()));
    }

    private void setListener() {
        binding.imgBack.setOnClickListener(view -> onBackPressed());
        binding.imgSave.setOnClickListener(view -> saveNote());
    }

    private void saveNote() {
        if (binding.edNoteTitle.getText().toString().trim().isEmpty()) {
            ToastManager.getInstance().showToast(this, "請輸入筆記標題!", true);
            return;
        } else if (binding.edNoteSubtitle.getText().toString().trim().isEmpty()){
            ToastManager.getInstance().showToast(this, "請輸入筆記子標題!", true);
            return;
        }

        final Note note = new Note();
        note.setTitle(binding.edNoteTitle.getText().toString());
        note.setSubtitle(binding.edNoteSubtitle.getText().toString());
        note.setNoteText(binding.edNote.getText().toString());
        note.setDateTime(binding.tvDateTime.getText().toString());
        note.setColor(selectedNoteColor);

        class SaveNoteTask extends AsyncTask<Void, Void, Void> {
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        }

        new SaveNoteTask().execute();
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) binding.viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void initMiscellaneous() {
        final LinearLayoutCompat layoutMiscellaneous = binding.layoutNoteColor.layoutMiscellaneous;
        final BottomSheetBehavior<LinearLayoutCompat> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);

        binding.layoutNoteColor.tvMiscellaneous.setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        AppCompatImageView[] imgColor = {binding.layoutNoteColor.imgColor1, binding.layoutNoteColor.imgColor2,
                                         binding.layoutNoteColor.imgColor3, binding.layoutNoteColor.imgColor4,
                                         binding.layoutNoteColor.imgColor5};

        setNoteColorListener(imgColor, "#333333", 0);
        setNoteColorListener(imgColor, "#FDBE3B", 1);
        setNoteColorListener(imgColor, "#FF4842", 2);
        setNoteColorListener(imgColor, "#3A52FC", 3);
        setNoteColorListener(imgColor, "#018786", 4);
    }

    private void setNoteColorListener(AppCompatImageView[] imgColor, String color, int number) {
        imgColor[number].setOnClickListener(view -> {

            for (int i = 0; i < imgColor.length; i++) {
                if (i == number)
                    imgColor[i].setImageResource(R.drawable.ic_done_24);
                else
                    imgColor[i].setImageResource(0);
            }

            selectedNoteColor = color;
            setSubtitleIndicatorColor();
        });
    }
}