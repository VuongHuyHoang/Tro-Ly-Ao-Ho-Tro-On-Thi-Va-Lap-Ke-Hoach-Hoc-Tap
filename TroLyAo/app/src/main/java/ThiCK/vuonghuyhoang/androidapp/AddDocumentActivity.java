package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import ThiCK.vuonghuyhoang.androidapp.network.AiCallback;
import ThiCK.vuonghuyhoang.androidapp.network.GeminiClient;

public class AddDocumentActivity extends AppCompatActivity {

    private TextInputEditText edtDocumentContent;
    private MaterialButton btnGenerateQuiz;
    private GeminiClient geminiClient;
    private android.app.ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        edtDocumentContent = findViewById(R.id.edt_document_content);
        btnGenerateQuiz = findViewById(R.id.btn_generate_quiz);
        geminiClient = new GeminiClient();

        btnGenerateQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = edtDocumentContent.getText().toString().trim();
                if (content.isEmpty() || content.length() < 50) {
                    Toast.makeText(AddDocumentActivity.this, "Nội dung tài liệu quá ngắn, vui lòng nhập chi tiết hơn!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // THAY ĐỔI: Thay vì gọi AI ngay, hiển thị hộp thoại chọn số lượng câu hỏi
                showQuantitySelectionDialog(content);
            }
        });
    }

    /**
     * Hộp thoại trực quan cho phép người dùng lựa chọn số lượng câu hỏi Quiz muốn tạo
     */
    private void showQuantitySelectionDialog(String content) {
        String[] options = {"Tạo nhanh (3 câu hỏi)", "Tiêu chuẩn (5 câu hỏi)", "Thử thách (10 câu hỏi)", "Chuyên sâu (15 câu hỏi)"};
        int[] counts = {3, 5, 10, 15};

        new AlertDialog.Builder(this)
                .setTitle("Cấu hình số lượng câu hỏi")
                .setItems(options, (dialog, which) -> {
                    int selectedCount = counts[which];
                    // Tiến hành gọi AI sinh câu hỏi theo số lượng đã chọn
                    generateQuizWithAi(content, selectedCount);
                })
                .show();
    }

    /**
     * Gửi dữ liệu tới Gemini với số lượng câu hỏi được cấu hình động
     */
    private void generateQuizWithAi(String content, int questionCount) {
        // Hiển thị màn hình chờ loading thông minh kèm số câu hỏi đang tạo
        progressDialog = new android.app.ProgressDialog(AddDocumentActivity.this);
        progressDialog.setMessage("🤖 Trợ lý AI đang phân tích tài liệu và biên soạn bộ " + questionCount + " câu hỏi trắc nghiệm, vui lòng chờ...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // TỐI ƯU PROMPT: Chèn biến số lượng câu hỏi dynamic và thắt chặt cấu trúc JSON đầu ra khi tăng số câu
        String systemInstruction = "\n\n[HƯỚNG DẪN HỆ THỐNG RIGID]: Bạn là một Giảng viên đại học chuyên nghiệp. "
                + "Hãy đọc kỹ đoạn văn bản tài liệu do sinh viên cung cấp ở trên, trích xuất toàn bộ kiến thức cốt lõi và biên soạn đúng CHÍNH XÁC " + questionCount + " câu hỏi trắc nghiệm khách quan.\n"
                + "Bạn BẮT BUỘC phải trả về kết quả ở CUỐI bài viết của mình dưới dạng một chuỗi JSON thuần túy, tuyệt đối không kèm text giải thích ngoài lề, bọc đúng trong cặp thẻ đánh dấu cấu trúc như sau:\n"
                + "---QUIZ_START--- {\"quiz\": [{\"question\": \"Nội dung câu hỏi?\", \"options\": [\"Đáp án A\", \"Đáp án B\", \"Đáp án C\", \"Đáp án D\"], \"correctIndex\": 0, \"explanation\": \"Lời giải thích ngắn gọn bằng tiếng Việt\"}]} ---QUIZ_END---\n"
                + "Lưu ý nghiêm ngặt: 'correctIndex' bắt đầu từ 0 đến 3 ứng với vị trí câu trả lời chính xác trong mảng 'options'. Đảm bảo phân bổ đều đáp án đúng và câu hỏi bám sát nội dung chuyên ngành.";

        String finalPrompt = content + systemInstruction;

        geminiClient.sendPrompt(finalPrompt, new AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();

                    if (response.contains("---QUIZ_START---") && response.contains("---QUIZ_END---")) {
                        int startIndex = response.indexOf("---QUIZ_START---") + "---QUIZ_START---".length();
                        int endIndex = response.indexOf("---QUIZ_END---");
                        String jsonResult = response.substring(startIndex, endIndex).trim();

                        // Truyền chuỗi JSON đã bóc tách mượt mà sang cho QuizActivity (đã cấu hình Gson đón nhận ở bước trước)
                        Intent intent = new Intent(AddDocumentActivity.this, QuizActivity.class);
                        intent.putExtra("QUIZ_JSON", jsonResult);
                        startActivity(intent);
                    } else {
                        Toast.makeText(AddDocumentActivity.this, "Đường truyền AI bị nghẽn hoặc cấu trúc phản hồi không hợp lệ, vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(AddDocumentActivity.this, "Lỗi kết nối AI: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}