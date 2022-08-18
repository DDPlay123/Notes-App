package com.tutorial.notesapp.adapter.Notes;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.tutorial.notesapp.R;
import com.tutorial.notesapp.room.entities.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NotesViewHolder> {

    private List<Note> notes;
    private NotesListener notesListener;
    private List<Note> noteSource;

    private Timer timer;

    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        this.noteSource = notes;
    }

    @NonNull
    @Override
    public NotesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotesViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_container_note, parent, false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
        holder.setNote(notes.get(position));
        holder.layoutNote.setOnClickListener(view -> {
            notesListener.onNoteClicked(notes.get(position), position);
        });
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NotesViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvTitle, tvSubtitle, tvDateTime;
        private final LinearLayoutCompat layoutNote;
        private final RoundedImageView imgNote;

        public NotesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            layoutNote = itemView.findViewById(R.id.layout_note);
            imgNote = itemView.findViewById(R.id.img_note);
        }

        void setNote(Note note) {
            tvTitle.setText(note.getTitle());
            if (note.getSubtitle().trim().isEmpty())
                tvSubtitle.setVisibility(View.GONE);
            else
                tvSubtitle.setText(note.getSubtitle());

            tvDateTime.setText(note.getDateTime());

            GradientDrawable gradientDrawable = (GradientDrawable) layoutNote.getBackground();
            if (note.getColor() != null)
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            else
                gradientDrawable.setColor(Color.parseColor("#333333"));

            if (note.getImagePath() != null) {
                imgNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                imgNote.setVisibility(View.VISIBLE);
            } else {
                imgNote.setImageResource(R.drawable.ic_image_24);
                imgNote.setVisibility(View.GONE);
            }
        }
    }

    public void searchNotes(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty())
                    notes = noteSource;
                else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : noteSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                            note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase()) ||
                            note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase()))
                            temp.add(note);
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 100);
    }

    public void cancelTimer() {
        if (timer != null)
            timer.cancel();
    }
}
