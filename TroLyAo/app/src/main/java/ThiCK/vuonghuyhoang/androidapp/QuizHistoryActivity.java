package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        setContentView(R.layout.activity_quiz_history);

        recyclerHistory = findViewById(R.id.recycler_quiz_history);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();

        // Cấu hình hành động click: Tái chế lại màn hình QuizActivity cũ bằng cách biến đổi Object thành JSON
        adapter = new QuizHistoryAdapter(historyList, savedQuiz -> {
            try {
                Gson gson = new Gson();
                // Chuyển mảng câu hỏi thành chuỗi text JSON
                String questionsArrayJson = gson.toJson(savedQuiz.getQuestions());

                // Bọc lại đúng cấu trúc {"quiz": [...]} mà QuizActivity cũ đang yêu cầu
                String finalQuizJson = "{\"quiz\":" + questionsArrayJson + "}";

                Intent intent = new Intent(QuizHistoryActivity.this, QuizActivity.class);
                intent.putExtra("QUIZ_JSON", finalQuizJson);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Không thể tải lại đề thi này!", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerHistory.setAdapter(adapter);

        // Gọi hàm tải dữ liệu từ mây xuống
        loadQuizHistoryFromFirebase();
    }

    private void loadQuizHistoryFromFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .collection("saved_quizzes")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Đề thi mới làm xếp lên trên cùng
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        historyList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            SavedQuiz quiz = doc.toObject(SavedQuiz.class);
                            if (quiz != null) historyList.add(quiz);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}