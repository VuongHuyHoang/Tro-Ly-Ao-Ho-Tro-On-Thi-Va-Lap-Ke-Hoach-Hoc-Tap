package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private TextView tvProgress, tvQuestion;
    private MaterialButton[] btnOptions = new MaterialButton[4];

    // Cấu trúc cấu thành dữ liệu câu hỏi trong bộ nhớ RAM
    private List<JSONObject> quizList = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvProgress = findViewById(R.id.tv_quiz_progress);
        tvQuestion = findViewById(R.id.tv_quiz_question);
        btnOptions[0] = findViewById(R.id.btn_option_0);
        btnOptions[1] = findViewById(R.id.btn_option_1);
        btnOptions[2] = findViewById(R.id.btn_option_2);
        btnOptions[3] = findViewById(R.id.btn_option_3);

        // Nhận chuỗi JSON thô từ Intent phát từ AddDocumentActivity
        String rawJson = getIntent().getStringExtra("QUIZ_JSON");

        try {
            // Loại bỏ bọc định dạng markdown nếu có
            rawJson = rawJson.replace("```json", "").replace("```", "").trim();
            JSONObject mainObj = new JSONObject(rawJson);
            JSONArray array = mainObj.getJSONArray("quiz");
            for (int i = 0; i < array.length(); i++) {
                quizList.add(array.getJSONObject(i));
            }

            // Hiển thị câu hỏi đầu tiên
            displayQuestion();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi phân tích cú pháp câu hỏi AI!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Gán sự kiện click cho 4 nút đáp án
        for (int i = 0; i < 4; i++) {
            final int index = i;
            btnOptions[i].setOnClickListener(v -> checkAnswer(index));
        }
    }

    private void displayQuestion() {
        if (currentQuestionIndex >= quizList.size()) {
            showQuizResult();
            return;
        }

        try {
            JSONObject currentQuestion = quizList.get(currentQuestionIndex);
            tvProgress.setText("Câu hỏi: " + (currentQuestionIndex + 1) + " / " + quizList.size());
            tvQuestion.setText(currentQuestion.getString("question"));

            JSONArray optionsArray = currentQuestion.getJSONArray("options");
            for (int i = 0; i < 4; i++) {
                btnOptions[i].setText(optionsArray.getString(i));
                // Khôi phục màu nền mặc định ban đầu của các nút
                btnOptions[i].setBackgroundColor(Color.WHITE);
                btnOptions[i].setTextColor(Color.parseColor("#212121"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAnswer(int selectedIndex) {
        try {
            JSONObject currentQuestion = quizList.get(currentQuestionIndex);
            int correctIndex = currentQuestion.getInt("correctIndex");

            if (selectedIndex == correctIndex) {
                score++;
                Toast.makeText(this, "Chính xác! 🎉", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sai rồi! Chúc bạn may mắn lần sau 😢", Toast.LENGTH_SHORT).show();
            }

            // Đợi 1 giây để sinh viên nhìn lại đáp án rồi tự động nhảy sang câu tiếp theo
            new android.os.Handler().postDelayed(() -> {
                currentQuestionIndex++;
                displayQuestion();
            }, 1000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showQuizResult() {
        new AlertDialog.Builder(this)
                .setTitle("Kết quả ôn tập")
                .setMessage("Chúc mừng bạn đã hoàn thành bài trắc nghiệm nhanh!\n\nSố câu trả lời đúng của bạn: " + score + " / " + quizList.size() + " câu.")
                .setCancelable(false)
                .setPositiveButton("Hoàn tất", (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // Kết thúc màn hình làm quiz đưa về trang chính
                })
                .show();
    }
}