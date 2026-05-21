package ThiCK.vuonghuyhoang.androidapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WriteDiaryActivity extends AppCompatActivity {

    private EditText edtContent;
    private TextView btnSave, tvTitle;
    private ImageView btnClose;

    // Biến quản lý trạng thái Sửa hay Thêm mới
    private boolean isEditMode = false;
    private String editDiaryId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_diary);

        edtContent = findViewById(R.id.edt_diary_content);
        btnSave = findViewById(R.id.btn_save_diary);
        btnClose = findViewById(R.id.btn_close_diary);

        // Bạn hãy thêm id này vào TextView tiêu đề ở giữa trong file activity_write_diary.xml nhé
        tvTitle = findViewById(R.id.tv_diary_toolbar_title);

        btnClose.setOnClickListener(v -> finish());

        // KIỂM TRA XEM CÓ DỮ LIỆU CŨ TRUYỀN SANG HAY KHÔNG
        if (getIntent().hasExtra("DIARY_ID")) {
            isEditMode = true;
            editDiaryId = getIntent().getStringExtra("DIARY_ID");
            String oldContent = getIntent().getStringExtra("DIARY_CONTENT");

            // Đổ nội dung cũ lên màn hình và đổi tiêu đề thanh công cụ
            edtContent.setText(oldContent);
            if (tvTitle != null) tvTitle.setText("Chỉnh sửa nhật ký");

            // Đưa con trỏ chuột xuống cuối văn bản
            if (oldContent != null) edtContent.setSelection(oldContent.length());
        }

        btnSave.setOnClickListener(v -> saveDiaryToFirebase());
    }

    private void saveDiaryToFirebase() {
        String content = edtContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng viết gì đó trước khi lưu!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        btnSave.setEnabled(false);
        String uid = auth.getCurrentUser().getUid();

        if (isEditMode) {
            // --- KỊCH BẢN 1: ĐANG Ở CHẾ ĐỘ CHỈNH SỬA ---
            Map<String, Object> updates = new HashMap<>();
            updates.put("content", content); // Chỉ cập nhật lại chữ, giữ nguyên thời gian viết cũ

            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .collection("user_diaries")
                    .document(editDiaryId)
                    .update(updates) // Lệnh cập nhật dập đè nội dung cũ
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã cập nhật nhật ký!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });
        } else {
            // --- KỊCH BẢN 2: THÊM MỚI NHẬT KÝ (Logic cũ của bạn) ---
            long currentTime = System.currentTimeMillis();
            String diaryId = String.valueOf(currentTime);

            DiaryEntry newEntry = new DiaryEntry(diaryId, content, currentTime);

            FirebaseFirestore.getInstance().collection("users")
                    .document(uid)
                    .collection("user_diaries")
                    .document(diaryId)
                    .set(newEntry)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã lưu nhật ký!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                    });
        }
    }
}