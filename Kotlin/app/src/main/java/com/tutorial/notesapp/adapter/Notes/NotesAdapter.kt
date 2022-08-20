package com.tutorial.notesapp.adapter.Notes

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.makeramen.roundedimageview.RoundedImageView
import com.tutorial.notesapp.R
import com.tutorial.notesapp.room.entiries.Note
import java.util.*

class NotesAdapter(private var notes: MutableList<Note>, val notesListener: NotesListener):
    RecyclerView.Adapter<NotesAdapter.NotesViewHolder>() {

    private val noteSource: MutableList<Note> = notes
    private var timer: Timer? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotesViewHolder {
       return NotesViewHolder(LayoutInflater.from(parent.context).inflate(
           R.layout.item_container_note, parent, false
       ))
    }

    override fun onBindViewHolder(holder: NotesViewHolder, position: Int) {
        holder.setNote(notes[position])
        holder.layoutNote.setOnClickListener {
            notesListener.onNoteClicked(notes[position], position)
        }
    }

    override fun getItemCount(): Int = notes.size

    override fun getItemViewType(position: Int): Int = position

    class NotesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: AppCompatTextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: AppCompatTextView = itemView.findViewById(R.id.tv_subtitle)
        private val tvDateTime: AppCompatTextView = itemView.findViewById(R.id.tv_date_time)
        val layoutNote: LinearLayoutCompat = itemView.findViewById(R.id.layout_note)
        private val imgNote: RoundedImageView = itemView.findViewById(R.id.img_note)

        fun setNote(note: Note) {
            tvTitle.text = note.title

            if (note.subtitle!!.trim().isEmpty())
                tvSubtitle.visibility = View.GONE else tvSubtitle.text = note.subtitle

            tvDateTime.text = note.dateTime

            val gradientDrawable = layoutNote.background as GradientDrawable

            if (note.color != null)
                gradientDrawable.setColor(Color.parseColor(note.color))
            else
                gradientDrawable.setColor(Color.parseColor("#333333"))

            if (note.imagePath != null) {
                imgNote.setImageBitmap(BitmapFactory.decodeFile(note.imagePath))
                imgNote.visibility = View.VISIBLE
            } else {
                imgNote.setImageResource(R.drawable.ic_image_24)
                imgNote.visibility = View.GONE
            }
        }
    }

    fun searchNotes(searchKeyword: String) {
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            @SuppressLint("NotifyDataSetChanged")
            override fun run() {
                if (searchKeyword.trim().isEmpty())
                    notes = noteSource
                else {
                    noteSource.forEach { note: Note ->
                        val temp = ArrayList<Note>()
                        if (note.title?.lowercase()!!.contains(searchKeyword.lowercase()) ||
                            note.subtitle?.lowercase()!!.contains(searchKeyword.lowercase()) ||
                            note.noteText?.lowercase()!!.contains(searchKeyword.lowercase()))
                            temp.add(note)
                    }
                    Handler(Looper.getMainLooper()).post { notifyDataSetChanged() }
                }
            }
        }, 100)
    }

    fun cancelTimer() = timer?.cancel()
}