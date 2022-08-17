package com.tutorial.notesapp.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tutorial.notesapp.R;
import com.tutorial.notesapp.room.entities.Note;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NotesViewHolder> {

    private List<Note> notes;

    public NotesAdapter(List<Note> notes) {
        this.notes = notes;
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

        public NotesViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubtitle = itemView.findViewById(R.id.tv_subtitle);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);
            layoutNote = itemView.findViewById(R.id.layout_note);
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
        }
    }
}
