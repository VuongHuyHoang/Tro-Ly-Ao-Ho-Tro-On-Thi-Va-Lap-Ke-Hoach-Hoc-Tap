package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvProgress, tvQuestion, tvExplanation;
    private MaterialCardView cardExplanation;
    private MaterialButton btnNextQuestion;
    private MaterialButton[] btnOptions = new MaterialButton[4];

    // CÁC BIẾN MỚI CHO BỘ ĐẾM THỜI GIAN
    private ProgressBar progressTimer;
    private TextView tvTimer;
    private CountDownTimer countDownTimer;
    private final long TIME_PER_QUESTION = 30000; // 30 giây cho mỗi câu

    private List<QuizQuestion> quizList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;
    private boolean isAnswered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvProgress = findViewById(R.id.tv_quiz_progress);
        tvQuestion = findViewById(R.id.tv_quiz_question);
        tvExplanation = findViewById(R.id.tv_quiz_explanation);
        cardExplanation = findViewById(R.id.card_explanation);
        btnNextQuestion = findViewById(R.id.btn_next_question);

        // Ánh xạ 2 View thời gian mới thêm
        progressTimer = findViewById(R.id.progress_timer);
        tvTimer = findViewById(R.id.tv_timer);

        btnOptions[0] = findViewById(R.id.btn_option_0);
        btnOptions[1] = findViewById(R.id.btn_option_1);
        btnOptions[2] = findViewById(R.id.btn_option_2);
        btnOptions[3] = findViewById(R.id.btn_option_3);

        String rawJson = getIntent().getStringExtra("QUIZ_JSON");

        try {
            rawJson = rawJson.replace("```json", "").replace("```", "").trim();
            JSONObject mainObj = new JSONObject(rawJson);
            String arrayJson = mainObj.getJSONArray("quiz").toString();

            Gson gson = new Gson();
            Type listType = new TypeToken<List<QuizQuestion>>(){}.getType();
            quizList = gson.fromJson(arrayJson, listType);

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
            QuizQuestion currentQuestion = quizList.get(currentQuestionIndex);
            tvProgress.setText("Câu hỏi: " + (currentQuestionIndex + 1) + " / " + quizList.size());
            tvQuestion.setText(currentQuestion.getQuestion());

            List<String> options = currentQuestion.getOptions();
            for (int i = 0; i < 4; i++) {
                btnOptions[i].setText(options.get(i));
                btnOptions[i].setBackgroundColor(Color.WHITE);
                btnOptions[i].setTextColor(Color.parseColor("#212121"));
                btnOptions[i].setEnabled(true);
            }

            // Bắt đầu đếm ngược ngay khi câu hỏi vừa hiển thị xong
            startTimer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Thuật toán khởi chạy và quản lý bộ đếm ngược thời gian
     */
    private void startTimer() {
        // Hủy bộ đếm cũ nếu có để tránh chạy đè nhau
        if (countDownTimer != null) countDownTimer.cancel();

        progressTimer.setMax((int) (TIME_PER_QUESTION / 1000));
        progressTimer.setProgress((int) (TIME_PER_QUESTION / 1000));
        tvTimer.setTextColor(Color.parseColor("#FF9800")); // Đặt lại màu cam mặc định

        countDownTimer = new CountDownTimer(TIME_PER_QUESTION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvTimer.setText(secondsLeft + "s");
                progressTimer.setProgress(secondsLeft);

                // Hiệu ứng cảnh báo: Đổi chữ sang màu đỏ khi chỉ còn từ 5 giây trở xuống
                if (secondsLeft <= 5) {
                    tvTimer.setTextColor(Color.RED);
                }
            }

            @Override
            public void onFinish() {
                tvTimer.setText("0s");
                progressTimer.setProgress(0);

                // Nếu hết giờ mà chưa trả lời -> Coi như trả lời sai, tự động khóa và hiện đáp án
                if (!isAnswered) {
                    Toast.makeText(QuizActivity.this, "Hết giờ!", Toast.LENGTH_SHORT).show();
                    checkAnswer(-1); // Truyền -1 để đánh dấu là không chọn gì
                }
            }
        }.start();
    }

    private void checkAnswer(int selectedIndex) {
        isAnswered = true;

        // Hủy bộ đếm ngược ngay khi người dùng đã chọn đáp án hoặc hết giờ
        if (countDownTimer != null) countDownTimer.cancel();

        for (MaterialButton btn : btnOptions) {
            btn.setEnabled(false);
        }

        try {
            QuizQuestion currentQuestion = quizList.get(currentQuestionIndex);
            int correctIndex = currentQuestion.getCorrectIndex();
            String explanation = currentQuestion.getExplanation();

            if (selectedIndex != -1) {
                // Kịch bản 1: Người dùng có chọn đáp án
                if (selectedIndex == correctIndex) {
                    score++;
                    btnOptions[selectedIndex].setBackgroundColor(Color.parseColor("#4CAF50"));
                    btnOptions[selectedIndex].setTextColor(Color.WHITE);
                } else {
                    btnOptions[selectedIndex].setBackgroundColor(Color.parseColor("#F44336"));
                    btnOptions[selectedIndex].setTextColor(Color.WHITE);

                    btnOptions[correctIndex].setBackgroundColor(Color.parseColor("#4CAF50"));
                    btnOptions[correctIndex].setTextColor(Color.WHITE);
                }
            } else {
                // Kịch bản 2: Hết giờ (-1), chỉ hiện đáp án đúng lên cho xem, không cộng điểm
                btnOptions[correctIndex].setBackgroundColor(Color.parseColor("#4CAF50"));
                btnOptions[correctIndex].setTextColor(Color.WHITE);
            }

            tvExplanation.setText(explanation);
            cardExplanation.setVisibility(View.VISIBLE);
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

            // 1. Logic lưu điểm cao nhất (Giữ nguyên của bạn)
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
                    });

            // 2. BỔ SUNG LÕI BACKEND: Tự động lưu Ngân hàng đề thi (Quiz Bank)
            long currentTime = System.currentTimeMillis();
            String quizId = String.valueOf(currentTime);

            // Lấy ngày tháng để làm tiêu đề mặc định cho bộ đề
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy - HH:mm", java.util.Locale.getDefault());
            String quizTitle = "Bộ đề ôn tập: " + sdf.format(new java.util.Date(currentTime));

            // Đóng gói toàn bộ dữ liệu
            SavedQuiz savedQuiz = new SavedQuiz(quizId, quizTitle, currentTime, score, quizList.size(), quizList);

            // Đẩy lên Firebase vào thư mục "saved_quizzes" của user hiện tại
            db.collection("users")
                    .document(currentUid)
                    .collection("saved_quizzes")
                    .document(quizId)
                    .set(savedQuiz)
                    .addOnSuccessListener(aVoid -> {
                        // Lưu thành công thì hiện Dialog báo điểm
                        showFinalDialog();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi lưu bộ đề: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        showFinalDialog(); // Dù lỗi mạng vẫn phải hiện điểm cho người dùng xem
                    });
        } else {
            showFinalDialog();
        }
    }

    private void showFinalDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Kết quả ôn tập")
                .setMessage("Chúc mừng bạn đã hoàn thành bài thi!\n\nSố câu trả lời đúng: " + score + " / " + quizList.size() + " câu.")
                .setCancelable(false)
                .setPositiveButton("Hoàn tất", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    // BẮT BUỘC: Hủy bộ đếm khi người dùng đột ngột thoát app để giải phóng RAM bộ nhớ
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}