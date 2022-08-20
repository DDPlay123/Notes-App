package com.tutorial.notesapp.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tutorial.notesapp.R
import com.tutorial.notesapp.adapter.Notes.NotesAdapter
import com.tutorial.notesapp.adapter.Notes.NotesListener
import com.tutorial.notesapp.databinding.ActivityMainBinding
import com.tutorial.notesapp.databinding.DialogAddUrlBinding
import com.tutorial.notesapp.manager.DataManger
import com.tutorial.notesapp.manager.DialogManager
import com.tutorial.notesapp.manager.ToastManager
import com.tutorial.notesapp.room.entiries.Note
import com.tutorial.notesapp.tools.Method
import com.tutorial.notesapp.tools.NoteStatus
import com.tutorial.notesapp.tools.Request_PERMISSION
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var noteList: MutableList<Note>
    private lateinit var notesAdapter: NotesAdapter
    private var selectedNoteId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setListener()
        setListNotes()
        setSearchListener()
        getNotes(NoteStatus.ShowNote, false)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Request_PERMISSION && grantResults.isNotEmpty())
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                selectImage()
            else
                ToastManager.instance.showToast(this, "權限不足!", true)
    }

    // 開新筆記
    private val newNote = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            getNotes(NoteStatus.NewNote, false)
        }
    }

    // 更新筆記
    private val updateNote = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val intent = result.data
            getNotes(NoteStatus.UpdateNote, intent!!.getBooleanExtra("isDeleted", false))
        }
    }

    // 從相簿中選照片，再新增筆記
    private val getImage = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
        if (result != null) {
            try {
                val selectedImageFilePath: String? = result.data?.data?.let { getPathFromUri(it) }
                val intent = Intent(applicationContext, CreateNoteActivity::class.java)
                intent.putExtra("isFromQuickActions", true)
                intent.putExtra("quickActionType", "image")
                intent.putExtra("imagePath", selectedImageFilePath)
                newNote.launch(intent)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                ToastManager.instance.showToast(this, "發生錯誤，請稍後嘗試", true)
            }
        }
    }

    private fun setListener() {
        binding.run {
            imgAddNoteMain.setOnClickListener { newNote.launch(Intent(applicationContext, CreateNoteActivity::class.java)) }
            imgAddNote.setOnClickListener { newNote.launch(Intent(applicationContext, CreateNoteActivity::class.java)) }
            imgAddImage.setOnClickListener {
                try { Method.requestPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) }
                catch (e: Exception) { e.printStackTrace() }
                finally { selectImage() }
            }
            imgAddWebLink.setOnClickListener {
                DialogManager.instance.showCustomDialog(this@MainActivity,
                    DialogAddUrlBinding.inflate(LayoutInflater.from(this@MainActivity)).root, true
                )?.let { view ->
                    val tvCancel = view.findViewById<AppCompatTextView>(R.id.tv_cancel)
                    val tvAdd = view.findViewById<AppCompatTextView>(R.id.tv_add)
                    val edAddURL = view.findViewById<AppCompatEditText>(R.id.ed_add_url)
                    tvCancel.setOnClickListener { DialogManager.instance.cancelDialog() }
                    tvAdd.setOnClickListener {
                        if (edAddURL.text.toString().trim().isEmpty())
                            ToastManager.instance.showToast(this@MainActivity, "請輸入網址", true)
                        else if (!Patterns.WEB_URL.matcher(edAddURL.text.toString()).matches())
                            ToastManager.instance.showToast(this@MainActivity, "請輸入正確的網址", true)
                        else run {
                            intent = Intent(applicationContext, CreateNoteActivity::class.java)
                            intent.putExtra("isFromQuickActions", true)
                            intent.putExtra("quickActionType", "URL")
                            intent.putExtra("URL", edAddURL.text.toString())
                            newNote.launch(intent)
                            DialogManager.instance.cancelDialog()
                        }
                    }
                }
            }
        }
    }

    private fun setListNotes() {
        noteList = ArrayList()
        notesAdapter = NotesAdapter(noteList, object : NotesListener{
            override fun onNoteClicked(note: Note, position: Int) {
                ToastManager.instance.showToast(this@MainActivity, "修改筆記", true)
                selectedNoteId = position
                val intent = Intent(applicationContext, CreateNoteActivity::class.java)
                intent.putExtra("isViewOrUpdate", true)
                intent.putExtra("note", note)
                updateNote.launch(intent)
            }
        })
        binding.rvNotes.layoutManager = StaggeredGridLayoutManager(
            2, StaggeredGridLayoutManager.VERTICAL
        )
        binding.rvNotes.adapter = notesAdapter
    }

    private fun getNotes(type: NoteStatus, isDeleted: Boolean) {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            val notes: MutableList<Note>? = DataManger.instance.initDatabase(applicationContext)?.noteDao()?.getAllNotes()
            handler.post {
                if (notes != null)
                    when (type) {
                        NoteStatus.ShowNote -> {
                            noteList.addAll(notes)
                            notesAdapter.notifyDataSetChanged()
                        }
                        NoteStatus.NewNote -> {
                            ToastManager.instance.showToast(this, "新增成功", true)
                            noteList.add(0, notes[0])
                            notesAdapter.notifyItemInserted(0)
                            binding.rvNotes.smoothScrollToPosition(0)
                        }
                        NoteStatus.UpdateNote -> {
                            ToastManager.instance.showToast(this, "更新成功", true)
                            noteList.removeAt(selectedNoteId)
                            if (isDeleted)
                                notesAdapter.notifyItemRemoved(selectedNoteId)
                            else {
                                noteList.add(selectedNoteId, notes[selectedNoteId])
                                notesAdapter.notifyItemInserted(selectedNoteId)
                            }
                            notesAdapter.notifyDataSetChanged()
                        }
                    }
            }
        }
    }

    private fun setSearchListener() {
        binding.edSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                notesAdapter.cancelTimer()
            }

            override fun afterTextChanged(editable: Editable?) {
                if (noteList.isNotEmpty())
                    notesAdapter.searchNotes(editable.toString())
            }
        })
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            getImage.launch(intent)
        }
    }

    private fun getPathFromUri(contentUri: Uri): String? {
        val filePath: String?
        val cursor = contentResolver
            .query(contentUri, null, null, null, null)
        if (cursor == null) filePath = contentUri.path else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }
        return filePath
    }
}