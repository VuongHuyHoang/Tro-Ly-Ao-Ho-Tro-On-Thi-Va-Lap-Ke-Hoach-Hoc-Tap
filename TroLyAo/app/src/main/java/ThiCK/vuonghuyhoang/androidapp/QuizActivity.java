package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvProgress, tvQuestion, tvExplanation;
    private MaterialCardView cardExplanation;
    private MaterialButton btnNextQuestion;
    private MaterialButton[] btnOptions = new MaterialButton[4];

    private List<JSONObject> quizList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean isAnswered = false; // Chặn bấm nhiều lần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvProgress = findViewById(R.id.tv_quiz_progress);
        tvQuestion = findViewById(R.id.tv_quiz_question);
        tvExplanation = findViewById(R.id.tv_quiz_explanation);
        cardExplanation = findViewById(R.id.card_explanation);
        btnNextQuestion = findViewById(R.id.btn_next_question);

        btnOptions[0] = findViewById(R.id.btn_option_0);
        btnOptions[1] = findViewById(R.id.btn_option_1);
        btnOptions[2] = findViewById(R.id.btn_option_2);
        btnOptions[3] = findViewById(R.id.btn_option_3);

        String rawJson = getIntent().getStringExtra("QUIZ_JSON");

        try {
            rawJson = rawJson.replace("```json", "").replace("```", "").trim();
            JSONObject mainObj = new JSONObject(rawJson);
            JSONArray array = mainObj.getJSONArray("quiz");
            for (int i = 0; i < array.length(); i++) {
                quizList.add(array.getJSONObject(i));
            }
            displayQuestion();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi nạp cấu trúc câu hỏi!", Toast.LENGTH_SHORT).show();
            finish();
        }

        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnOptions[i].setOnClickListener(v -> {
                if (!isAnswered) {
                    checkAnswer(index);
                }
            });
        }

        // Xử lý sự kiện nút "Câu Tiếp Theo"
        btnNextQuestion.setOnClickListener(v -> {
            currentQuestionIndex++;
            displayQuestion();
        });
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= quizList.size()) {
            saveResultAndShowDialog();
            return;
        }

        isAnswered = false;
        cardExplanation.setVisibility(View.GONE);
        btnNextQuestion.setVisibility(View.GONE);

        try {
            JSONObject currentQuestion = quizList.get(currentQuestionIndex);
            tvProgress.setText("Câu hỏi: " + (currentQuestionIndex + 1) + " / " + quizList.size());
            tvQuestion.setText(currentQuestion.getString("question"));

            JSONArray optionsArray = currentQuestion.getJSONArray("options");
            for (int i = 0; i < 4; i++) {
                btnOptions[i].setText(optionsArray.getString(i));
                btnOptions[i].setBackgroundColor(Color.WHITE);
                btnOptions[i].setTextColor(Color.parseColor("#212121"));
                btnOptions[i].setEnabled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAnswer(int selectedIndex) {
        isAnswered = true;
        // Vô hiệu hóa 4 nút bấm ngay lập tức
        for (MaterialButton btn : btnOptions) {
            btn.setEnabled(false);
        }

        try {
            JSONObject currentQuestion = quizList.get(currentQuestionIndex);
            int correctIndex = currentQuestion.getInt("correctIndex");
            String explanation = currentQuestion.getString("explanation");

            // Tô màu trực quan phản hồi đáp án
            if (selectedIndex == correctIndex) {
                score++;
                btnOptions[selectedIndex].setBackgroundColor(Color.parseColor("#4CAF50")); // Hiện xanh lá câu đúng
                btnOptions[selectedIndex].setTextColor(Color.WHITE);
            } else {
                btnOptions[selectedIndex].setBackgroundColor(Color.parseColor("#F44336")); // Hiện đỏ câu người dùng chọn sai
                btnOptions[selectedIndex].setTextColor(Color.WHITE);

                btnOptions[correctIndex].setBackgroundColor(Color.parseColor("#4CAF50")); // Vẫn lá câu đúng để đối chiếu
                btnOptions[correctIndex].setTextColor(Color.WHITE);
            }

            // Đổ dữ liệu lời giải và bật vùng hiển thị lên
            tvExplanation.setText(explanation);
            cardExplanation.setVisibility(View.generateViewId());
            btnNextQuestion.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveResultAndShowDialog() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Thực hiện kiểm tra so sánh điểm cao nhất cũ để ghi đè điểm cao mới lên Firestore
            db.collection("profiles")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        long currentHighScore = 0;
                        if (documentSnapshot.exists() && documentSnapshot.contains("highScore")) {
                            currentHighScore = documentSnapshot.getLong("highScore");
                        }

                        if (score > currentHighScore) {
                            db.collection("profiles")
                                    .document(currentUid)
                                    .update("highScore", score);
                        }

                        showFinalDialog();
                    })
                    .addOnFailureListener(e -> showFinalDialog());
        } else {
            showFinalDialog();
        }
    }

    private void showFinalDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Kết quả ôn tập")
                .setMessage("Chúc mừng bạn đã hoàn thành bài trắc nghiệm nhanh!\n\nSố câu trả lời đúng: " + score + " / " + quizList.size() + " câu.")
                .setCancelable(false)
                .setPositiveButton("Hoàn tất", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }
}