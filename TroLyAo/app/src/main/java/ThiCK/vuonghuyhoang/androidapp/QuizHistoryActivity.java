package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class QuizHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private QuizHistoryAdapter adapter;
    private List<SavedQuiz> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_history); // Đảm bảo đúng tên file XML của bạn

        ImageView btnBack = findViewById(R.id.btn_back_quiz);
        btnBack.setOnClickListener(v -> finish());

        ExtendedFloatingActionButton btnCreateQuiz = findViewById(R.id.btn_create_quiz);
        btnCreateQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(QuizHistoryActivity.this, AddDocumentActivity.class);
            startActivity(intent);
        });

        recyclerHistory = findViewById(R.id.recycler_quiz_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();

        adapter = new QuizHistoryAdapter(historyList,
                // 1. SỰ KIỆN CLICK NGẮN -> Hiển thị hộp thoại xác nhận làm bài
                savedQuiz -> showStartQuizConfirmDialog(savedQuiz),

                // 2. SỰ KIỆN NHẤN GIỮ -> Hiển thị Menu Tùy chọn (Đổi tên / Xóa)
                savedQuiz -> showOptionsDialog(savedQuiz)
        );

        recyclerHistory.setAdapter(adapter);
        loadQuizHistoryFromFirebase();
    }

    // ================= CHỨC NĂNG 1: XÁC NHẬN LÀM BÀI =================
    private void showStartQuizConfirmDialog(SavedQuiz quiz) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Bắt đầu ôn tập?")
                .setMessage("Bạn có muốn mở lại đề thi: \"" + quiz.getTitle() + "\" không?")
                .setPositiveButton("Bắt đầu làm", (dialog, which) -> {
                    // Logic chuyển sang màn hình làm bài gốc của bạn
                    try {
                        Gson gson = new Gson();
                        String questionsArrayJson = gson.toJson(quiz.getQuestions());
                        String finalQuizJson = "{\"quiz\":" + questionsArrayJson + "}";

                        Intent intent = new Intent(QuizHistoryActivity.this, QuizActivity.class);
                        intent.putExtra("QUIZ_JSON", finalQuizJson);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Không thể tải lại đề thi này!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================= CHỨC NĂNG 2: MENU TÙY CHỌN (NHẤN GIỮ) =================
    private void showOptionsDialog(SavedQuiz quiz) {
        String[] options = {"✏️  Đổi tên đề thi", "🗑️  Xóa đề thi"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tùy chọn đề thi")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenameDialog(quiz);
                    } else if (which == 1) {
                        showDeleteConfirmDialog(quiz);
                    }
                })
                .show();
    }

    // ================= CHỨC NĂNG 3: ĐỔI TÊN ĐỀ THI =================
    private void showRenameDialog(SavedQuiz quiz) {
        // Tạo giao diện ô nhập liệu bằng code (không cần tạo thêm file XML)
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int paddingPx = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(paddingPx, paddingPx, paddingPx, 0);

        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setHint("Tên/Mô tả đề thi mới");
        textInputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText edtNewName = new TextInputEditText(textInputLayout.getContext());
        edtNewName.setText(quiz.getTitle()); // Đưa tên cũ vào ô để sửa
        edtNewName.setSingleLine(true);

        textInputLayout.addView(edtNewName);
        layout.addView(textInputLayout);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đổi tên đề thi")
                .setView(layout)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = edtNewName.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(quiz.getTitle())) {
                        executeUpdateQuizName(quiz, newName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeUpdateQuizName(SavedQuiz quiz, String newName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || quiz.getDocumentId() == null) return;

        String uid = auth.getCurrentUser().getUid();

        // Gửi lệnh update trường "title" lên Firebase
        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("saved_quizzes")
                .document(quiz.getDocumentId())
                .update("title", newName)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã đổi tên thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi đổi tên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================= CHỨC NĂNG 4: XÓA ĐỀ THI =================
    private void showDeleteConfirmDialog(SavedQuiz quizToDelete) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa đề thi này?")
                .setMessage("Bạn có chắc muốn xóa đề thi \"" + quizToDelete.getTitle() + "\" không? Thao tác này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> executeDeleteQuiz(quizToDelete))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void executeDeleteQuiz(SavedQuiz quiz) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || quiz.getDocumentId() == null) return;

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("saved_quizzes")
                .document(quiz.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa đề thi!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================= TẢI DỮ LIỆU TỪ FIREBASE =================
    private void loadQuizHistoryFromFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("saved_quizzes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        historyList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            SavedQuiz quiz = doc.toObject(SavedQuiz.class);
                            if (quiz != null) {
                                quiz.setDocumentId(doc.getId());
                                historyList.add(quiz);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}