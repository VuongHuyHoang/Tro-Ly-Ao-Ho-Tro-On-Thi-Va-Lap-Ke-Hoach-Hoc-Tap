package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
        setContentView(R.layout.activity_quiz_history); // Đảm bảo tên file XML đúng

        // 1. Cài đặt Nút Back
        ImageView btnBack = findViewById(R.id.btn_back_quiz);
        btnBack.setOnClickListener(v -> finish());

        // 2. Cài đặt Nút Tạo đề thi (Mở Fragment)
        ExtendedFloatingActionButton btnCreateQuiz = findViewById(R.id.btn_create_quiz);
        btnCreateQuiz.setOnClickListener(v -> {
            Intent intent = new Intent(QuizHistoryActivity.this, AddDocumentActivity.class);
            startActivity(intent);
        });

        // 3. Cài đặt RecyclerView và Adapter
        recyclerHistory = findViewById(R.id.recycler_quiz_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        historyList = new ArrayList<>();

        adapter = new QuizHistoryAdapter(historyList,
                // Sự kiện 1: Click ngắn -> Làm lại đề thi
                savedQuiz -> {
                    try {
                        Gson gson = new Gson();
                        String questionsArrayJson = gson.toJson(savedQuiz.getQuestions());
                        String finalQuizJson = "{\"quiz\":" + questionsArrayJson + "}";

                        Intent intent = new Intent(QuizHistoryActivity.this, QuizActivity.class);
                        intent.putExtra("QUIZ_JSON", finalQuizJson);
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Không thể tải lại đề thi này!", Toast.LENGTH_SHORT).show();
                    }
                },
                // Sự kiện 2: Nhấn giữ -> Gọi hộp thoại Xóa
                savedQuiz -> showDeleteConfirmDialog(savedQuiz)
        );

        recyclerHistory.setAdapter(adapter);

        // 4. Tải dữ liệu từ Firebase
        loadQuizHistoryFromFirebase();
    }

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
                                quiz.setDocumentId(doc.getId()); // Gắn ID để lát xóa
                                historyList.add(quiz);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // --- LOGIC XÓA ĐỀ THI ---
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
}