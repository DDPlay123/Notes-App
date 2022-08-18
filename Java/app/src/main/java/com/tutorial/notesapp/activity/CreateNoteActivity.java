package com.tutorial.notesapp.activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.LinearLayoutCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.tutorial.notesapp.R;
import com.tutorial.notesapp.databinding.ActivityCreateNoteBinding;
import com.tutorial.notesapp.databinding.DialogAddUrlBinding;
import com.tutorial.notesapp.databinding.DialogDeleteNoteBinding;
import com.tutorial.notesapp.manager.ToastManager;
import com.tutorial.notesapp.room.database.NotesDatabase;
import com.tutorial.notesapp.room.entities.Note;
import com.tutorial.notesapp.tools.Method;
import com.tutorial.notesapp.tools.Reference;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends BaseActivity {
    private ActivityCreateNoteBinding binding;

    private String selectedNoteColor;
    private String selectedImageFilePath;

    private Boolean isViewOrUpdate;
    private Note alreadyAvailableNote;

    private Boolean isFromQuickActions;
    private String quickActionType;
    private String imagePath;
    private String URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());
        // 設定當前時間
        binding.tvDateTime.setText(new SimpleDateFormat(
                "yyyy MMMM dd, EEEE a hh:mm",
                Locale.TAIWAN
        ).format(new Date()));
        // 進階選項的預設顏色及預設圖徑
        selectedNoteColor = "#333333"; // Default note color
        selectedImageFilePath = ""; // Default file uri is empty
        // 取得 Arg
        getArgument();
        // 筆記初始化
        setViewOrUpdateNote();
        // 按鈕功能
        setListener();
        // 預設圖標顏色
        setSubtitleIndicatorColor();
        // 進階功能
        initMiscellaneous();
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

    private void getArgument() {
        isViewOrUpdate = getIntent().getBooleanExtra("isViewOrUpdate", false);
        alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");

        isFromQuickActions = getIntent().getBooleanExtra("isFromQuickActions", false);
        quickActionType = getIntent().getStringExtra("quickActionType");
        imagePath = getIntent().getStringExtra("imagePath");
        URL = getIntent().getStringExtra("URL");
    }

    private void setViewOrUpdateNote() {
        if (isViewOrUpdate) {
            // 如果不是新筆記，填入筆記內容
            binding.edNoteTitle.setText(alreadyAvailableNote.getTitle());
            binding.edNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
            binding.edNote.setText(alreadyAvailableNote.getNoteText());
            binding.tvDateTime.setText(alreadyAvailableNote.getDateTime());

            if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
                binding.imgNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
                binding.imgNote.setVisibility(View.VISIBLE);
                binding.imgRemoveImage.setVisibility(View.VISIBLE);
                selectedImageFilePath = alreadyAvailableNote.getImagePath();
            }

            if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
                binding.tvWebUrl.setText(alreadyAvailableNote.getWebLink());
                binding.layoutWebUrl.setVisibility(View.VISIBLE);
            }
        } else if (isFromQuickActions && quickActionType != null && quickActionType.equals("image")) {
            selectedImageFilePath = imagePath;
            binding.imgNote.setImageBitmap(BitmapFactory.decodeFile(selectedImageFilePath));
            binding.imgNote.setVisibility(View.VISIBLE);
            binding.imgRemoveImage.setVisibility(View.VISIBLE);
        } else if (isFromQuickActions && quickActionType != null && quickActionType.equals("URL")) {
            binding.tvWebUrl.setText(URL);
            binding.layoutWebUrl.setVisibility(View.VISIBLE);
        }
    }

    private void setListener() {
        binding.imgBack.setOnClickListener(view -> {
            hideKeyboard();
            onBackPressed();
        });
        binding.imgSave.setOnClickListener(view -> {
            hideKeyboard();
            saveNote();
        });
        binding.imgRemoveImage.setOnClickListener(view -> {
            binding.imgNote.setImageBitmap(null);
            binding.imgNote.setVisibility(View.GONE);
            view.setVisibility(View.GONE);
            selectedImageFilePath = "";
            hideKeyboard();
        });
        binding.imgRemoveWebUrl.setOnClickListener(view -> {
            binding.tvWebUrl.setText("");
            binding.layoutWebUrl.setVisibility(View.GONE);
            hideKeyboard();
        });
    }

    private void saveNote() {
        if (binding.edNoteTitle.getText().toString().trim().isEmpty()) {
            ToastManager.getInstance().showToast(this, "請輸入筆記標題!", true);
            return;
        } else if (binding.edNoteSubtitle.getText().toString().trim().isEmpty()){
            ToastManager.getInstance().showToast(this, "請輸入筆記子標題!", true);
            return;
        }
        // 儲存筆記內容
        final Note note = new Note();
        note.setTitle(binding.edNoteTitle.getText().toString());
        note.setSubtitle(binding.edNoteSubtitle.getText().toString());
        note.setNoteText(binding.edNote.getText().toString());
        note.setDateTime(binding.tvDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImageFilePath);
        if (binding.layoutWebUrl.getVisibility() == View.VISIBLE)
            note.setWebLink(binding.tvWebUrl.getText().toString());

        // 如果要修改筆記內容，將筆記的Id替換為原先的Id，筆記才不會跑到最上面
        if (alreadyAvailableNote != null) {
            note.setId(alreadyAvailableNote.getId());
        }

        // 儲存筆記並回傳
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

    // 進階功能
    private void initMiscellaneous() {
        // 定義下方彈出畫面功能
        final LinearLayoutCompat layoutMiscellaneous = binding.layoutNoteOption.layoutMiscellaneous;
        final BottomSheetBehavior<LinearLayoutCompat> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);

        // 點擊彈出
        binding.layoutNoteOption.tvMiscellaneous.setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        // 定義進階選項的 顏色按鈕 陣列
        AppCompatImageView[] imgColor = {binding.layoutNoteOption.imgColor1, binding.layoutNoteOption.imgColor2,
                                         binding.layoutNoteOption.imgColor3, binding.layoutNoteOption.imgColor4,
                                         binding.layoutNoteOption.imgColor5};

        // 設定點擊顏色按鈕，即可切換圖標顏色
        setNoteColorListener(imgColor, "#333333", 0);
        setNoteColorListener(imgColor, "#FDBE3B", 1);
        setNoteColorListener(imgColor, "#FF4842", 2);
        setNoteColorListener(imgColor, "#3A52FC", 3);
        setNoteColorListener(imgColor, "#018786", 4);

        // 如果不是新筆記，哲切換成顏色按鈕預設狀態
        if (alreadyAvailableNote != null &&
                alreadyAvailableNote.getColor() != null &&
                !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#FDBE3B":
                    binding.layoutNoteOption.imgColor2.performClick();
                    break;
                case "#FF4842":
                    binding.layoutNoteOption.imgColor3.performClick();
                    break;
                case "#3A52FC":
                    binding.layoutNoteOption.imgColor4.performClick();
                    break;
                case "#018786":
                    binding.layoutNoteOption.imgColor5.performClick();
                    break;
            }
        }

        binding.layoutNoteOption.layoutAddImage.setOnClickListener(view -> {
            // 收起
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            if (!Method.hasPermissions(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE))
                Method.requestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            else
                selectImage();
        });

        binding.layoutNoteOption.layoutAddUrl.setOnClickListener(view -> {
            // 收起
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            showAddURLDialog();
        });

        // 顯示刪除項
        if (alreadyAvailableNote != null) {
            binding.layoutNoteOption.layoutDeleteNote.setVisibility(View.VISIBLE);
            binding.layoutNoteOption.layoutDeleteNote.setOnClickListener(view -> {
                // 收起
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                showDeleteNoteDialog();
            });
        }
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

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            mGetImage.launch(intent);
        }
    }

    ActivityResultLauncher<Intent> mGetImage = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(result.getData().getData());
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                binding.imgNote.setImageBitmap(bitmap);
                binding.imgNote.setVisibility(View.VISIBLE);

                binding.imgRemoveImage.setVisibility(View.VISIBLE);

                selectedImageFilePath = getPathFromUri(result.getData().getData());
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
                binding.tvWebUrl.setText(_binding.edAddUrl.getText().toString());
                binding.layoutWebUrl.setVisibility(View.VISIBLE);
                dialogAddURL.dismiss();
            }
        });

        _binding.tvCancel.setOnClickListener(view -> dialogAddURL.dismiss());
    }

    private void showDeleteNoteDialog() {
        AlertDialog dialogDeleteNote;

        DialogDeleteNoteBinding _binding = DialogDeleteNoteBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(_binding.getRoot());

        dialogDeleteNote = builder.create();
        dialogDeleteNote.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialogDeleteNote.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialogDeleteNote.show();

        _binding.tvDeleteNote.setOnClickListener(view -> {
            class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                @Override
                protected Void doInBackground(Void... voids) {
                    NotesDatabase.getDatabase(getApplicationContext()).noteDao()
                            .deleteNote(alreadyAvailableNote);
                    return null;
                }

                @Override
                protected void onPostExecute(Void unused) {
                    super.onPostExecute(unused);
                    Intent intent = new Intent();
                    intent.putExtra("isDeleted", true);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
            new DeleteNoteTask().execute();
        });

        _binding.tvCancel.setOnClickListener(view -> dialogDeleteNote.dismiss());
    }
}