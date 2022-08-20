package com.tutorial.notesapp.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tutorial.notesapp.R
import com.tutorial.notesapp.databinding.ActivityCreateNoteBinding
import com.tutorial.notesapp.databinding.DialogAddUrlBinding
import com.tutorial.notesapp.databinding.DialogDeleteNoteBinding
import com.tutorial.notesapp.manager.DataManger
import com.tutorial.notesapp.manager.DialogManager
import com.tutorial.notesapp.manager.ToastManager
import com.tutorial.notesapp.room.entiries.Note
import com.tutorial.notesapp.tools.Method
import com.tutorial.notesapp.tools.Method.requestPermission
import com.tutorial.notesapp.tools.Request_PERMISSION
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.properties.Delegates


class CreateNoteActivity : BaseActivity() {
    private lateinit var binding: ActivityCreateNoteBinding

    private lateinit var selectedNoteColor: String
    private lateinit var selectedImageFilePath: String

    // 是否為新筆記
    private var isViewOrUpdate by Delegates.notNull<Boolean>()
    private lateinit var alreadyAvailableNote: Note

    // 是否從快捷鍵進來
    private var isFromQuickActions by Delegates.notNull<Boolean>()
    private lateinit var quickActionType: String
    private lateinit var imagePath: String
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNoteBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        // 設定當前時間
        binding.tvDateTime.text =
            SimpleDateFormat("yyyy MMMM dd, EEEE a hh:mm", Locale.TAIWAN).format(Date())
        // 進階選項的預設顏色及預設圖徑
        selectedNoteColor = "#333333" // Default note color
        selectedImageFilePath = "" // Default file uri is empty
        // 取得 Arg
        getArguments()
        // 筆記初始化
        setViewOrUpdateNote()
        // 按鈕功能
        setListener()
        // 預設圖標顏色
        setSubtitleIndicatorColor()
        // 進階功能
        initMiscellaneous()
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

    private val getImage = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result != null) {
            try {
                val inputStream = contentResolver.openInputStream(
                    result.data!!.data!!
                )
                val bitmap = BitmapFactory.decodeStream(inputStream)
                binding.imgNote.setImageBitmap(bitmap)
                binding.imgNote.visibility = View.VISIBLE
                binding.imgRemoveImage.visibility = View.VISIBLE
                selectedImageFilePath = result.data?.data?.let { getPathFromUri(it) }.toString()
            } catch (e: Exception) {
                e.printStackTrace()
                ToastManager.instance.showToast(this, "發生錯誤，請稍後嘗試", true)
            }
        }
    }

    private fun getArguments() {
        intent.extras.let {
            isViewOrUpdate = intent.getBooleanExtra("isViewOrUpdate", false)
            alreadyAvailableNote = (intent.getSerializableExtra("note") ?: Note()) as Note

            isFromQuickActions = intent.getBooleanExtra("isFromQuickActions", false)
            quickActionType = intent.getStringExtra("quickActionType") ?: ""
            imagePath = intent.getStringExtra("imagePath") ?: ""
            url = intent.getStringExtra("URL") ?: ""
        }
    }

    private fun setViewOrUpdateNote() {
        binding.run {
            if (isViewOrUpdate) {
                // 如果不是新筆記，填入筆記內容
                edNoteTitle.setText(alreadyAvailableNote.title)
                edNoteSubtitle.setText(alreadyAvailableNote.subtitle)
                edNote.setText(alreadyAvailableNote.noteText)
                tvDateTime.text = alreadyAvailableNote.dateTime

                if (alreadyAvailableNote.imagePath != null && alreadyAvailableNote.imagePath?.trim()!!.isNotEmpty()) {
                    imgNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.imagePath))
                    imgNote.visibility = View.VISIBLE
                    imgRemoveImage.visibility = View.VISIBLE
                    selectedImageFilePath = alreadyAvailableNote.imagePath!!
                }

                if (alreadyAvailableNote.webLink != null && alreadyAvailableNote.webLink?.trim()!!.isNotEmpty()) {
                    tvWebUrl.text = alreadyAvailableNote.webLink
                    layoutWebUrl.visibility = View.VISIBLE
                }
            } else if (isFromQuickActions && quickActionType == "image") {
                selectedImageFilePath = imagePath
                imgNote.setImageBitmap(BitmapFactory.decodeFile(selectedImageFilePath))
                imgNote.visibility = View.VISIBLE
                imgRemoveImage.visibility = View.VISIBLE
            } else if (isFromQuickActions && quickActionType == "URL") {
                tvWebUrl.text = url
                layoutWebUrl.visibility = View.VISIBLE
            }
        }
    }

    private fun setListener() {
        binding.run {
            imgBack.setOnClickListener {
                Method.hideKeyBoard(this@CreateNoteActivity)
                onBackPressed()
            }
            imgSave.setOnClickListener {
                Method.hideKeyBoard(this@CreateNoteActivity)
                saveNote()
            }
            imgRemoveImage.setOnClickListener { view ->
                binding.imgNote.setImageBitmap(null)
                binding.imgNote.visibility = View.GONE
                view.visibility = View.GONE
                selectedImageFilePath = ""
                Method.hideKeyBoard(this@CreateNoteActivity)
            }
            imgRemoveWebUrl.setOnClickListener {
                binding.tvWebUrl.text = ""
                binding.layoutWebUrl.visibility = View.GONE
                Method.hideKeyBoard(this@CreateNoteActivity)
            }
        }
    }

    private fun saveNote() {
        binding.run {
            if (edNoteTitle.text.toString().trim().isEmpty()) {
                ToastManager.instance.showToast(this@CreateNoteActivity, "請輸入筆記標題!", true)
                return
            } else if (binding.edNoteSubtitle.text.toString().trim().isEmpty()) {
                ToastManager.instance.showToast(this@CreateNoteActivity, "請輸入筆記子標題!", true)
                return
            }
            // 儲存筆記內容
            val note = Note()
            note.title = edNoteTitle.text.toString()
            note.subtitle = edNoteSubtitle.text.toString()
            note.noteText = edNote.text.toString()
            note.dateTime = tvDateTime.text.toString()
            note.color = selectedNoteColor
            note.imagePath = selectedImageFilePath

            if (layoutWebUrl.visibility == View.VISIBLE)
                note.webLink = tvWebUrl.text.toString()

            // 如果要修改筆記內容，將筆記的Id替換為原先的Id，筆記才不會跑到最上面
            note.id = alreadyAvailableNote.id

            // 儲存筆記並回傳
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())

            executor.execute {
                DataManger.instance.initDatabase(applicationContext)?.noteDao()?.insertNote(note)
                handler.post {
                    setResult(RESULT_OK, Intent())
                    finish()
                }
            }
        }
    }

    private fun setSubtitleIndicatorColor() {
        val gradientDrawable = binding.viewSubtitleIndicator.background as GradientDrawable
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor))
    }

    // 進階功能
    private fun initMiscellaneous() {
        // 定義下方彈出畫面功能
        val layoutMiscellaneous = binding.layoutNoteOption.layoutMiscellaneous
        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)

        // 點擊彈出
        binding.layoutNoteOption.tvMiscellaneous.setOnClickListener {
            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) bottomSheetBehavior.setState(
                BottomSheetBehavior.STATE_EXPANDED
            ) else bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }

        // 定義進階選項的 顏色按鈕 陣列
        val imgColor = arrayOf(
            binding.layoutNoteOption.imgColor1, binding.layoutNoteOption.imgColor2,
            binding.layoutNoteOption.imgColor3, binding.layoutNoteOption.imgColor4,
            binding.layoutNoteOption.imgColor5
        )

        // 設定點擊顏色按鈕，即可切換圖標顏色
        setNoteColorListener(imgColor, "#333333", 0)
        setNoteColorListener(imgColor, "#FDBE3B", 1)
        setNoteColorListener(imgColor, "#FF4842", 2)
        setNoteColorListener(imgColor, "#3A52FC", 3)
        setNoteColorListener(imgColor, "#018786", 4)

        // 如果不是新筆記，哲切換成顏色按鈕預設狀態
        if (alreadyAvailableNote.color != null && alreadyAvailableNote.color!!.trim().isNotEmpty()) {
            when (alreadyAvailableNote.color) {
                "#FDBE3B" -> binding.layoutNoteOption.imgColor2.performClick()
                "#FF4842" -> binding.layoutNoteOption.imgColor3.performClick()
                "#3A52FC" -> binding.layoutNoteOption.imgColor4.performClick()
                "#018786" -> binding.layoutNoteOption.imgColor5.performClick()
            }
        }

        binding.layoutNoteOption.layoutAddImage.setOnClickListener {
            // 收起
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            try { requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) }
            catch (e: Exception) { e.printStackTrace() }
            finally { selectImage() }
        }

        binding.layoutNoteOption.layoutAddUrl.setOnClickListener {
            // 收起
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            showAddURLDialog()
        }

        if (isViewOrUpdate) {
            // 顯示刪除項
            binding.layoutNoteOption.layoutDeleteNote.visibility = View.VISIBLE
            binding.layoutNoteOption.layoutDeleteNote.setOnClickListener {
                // 收起
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                showDeleteNoteDialog()
            }
        }
    }

    private fun setNoteColorListener(
        imgColor: Array<AppCompatImageView>,
        color: String,
        number: Int
    ) {
        imgColor[number].setOnClickListener {
            for (i in imgColor.indices) {
                if (i == number) imgColor[i]
                    .setImageResource(R.drawable.ic_done_24) else imgColor[i].setImageResource(0)
            }
            selectedNoteColor = color
            setSubtitleIndicatorColor()
        }
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

    private fun showAddURLDialog() {
        DialogManager.instance.showCustomDialog(this,
            DialogAddUrlBinding.inflate(LayoutInflater.from(this)).root, true
        )?.let { view ->
            val tvCancel = view.findViewById<AppCompatTextView>(R.id.tv_cancel)
            val tvAdd = view.findViewById<AppCompatTextView>(R.id.tv_add)
            val edAddURL = view.findViewById<AppCompatEditText>(R.id.ed_add_url)
            tvCancel.setOnClickListener { DialogManager.instance.cancelDialog() }
            tvAdd.setOnClickListener {
                if (edAddURL.text.toString().trim().isEmpty())
                    ToastManager.instance.showToast(this, "請輸入網址", true)
                else if (!Patterns.WEB_URL.matcher(edAddURL.text.toString()).matches())
                    ToastManager.instance.showToast(this, "請輸入正確的網址", true)
                else run {
                    binding.tvWebUrl.text = edAddURL.text.toString()
                    binding.layoutWebUrl.visibility = View.VISIBLE
                    DialogManager.instance.cancelDialog()
                }
            }
        }
    }

    private fun showDeleteNoteDialog() {
        DialogManager.instance.showCustomDialog(this,
            DialogDeleteNoteBinding.inflate(LayoutInflater.from(this)).root
        )?.let { view ->
            val tvCancel = view.findViewById<AppCompatTextView>(R.id.tv_cancel)
            val tvDeleteNote = view.findViewById<AppCompatTextView>(R.id.tv_delete_note)
            tvCancel.setOnClickListener { DialogManager.instance.cancelDialog() }
            tvDeleteNote.setOnClickListener {
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())

                executor.execute {
                    DataManger.instance.initDatabase(applicationContext)?.noteDao()?.deleteNote(alreadyAvailableNote)
                    handler.post {
                        val intent = Intent()
                        intent.putExtra("isDeleted", true)
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }
            }
        }
    }
}