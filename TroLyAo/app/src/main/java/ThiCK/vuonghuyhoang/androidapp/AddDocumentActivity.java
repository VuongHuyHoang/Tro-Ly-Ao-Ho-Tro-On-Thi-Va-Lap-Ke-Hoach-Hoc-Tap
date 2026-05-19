package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
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

                // Hiển thị màn hình chờ loading
                progressDialog = new android.app.ProgressDialog(AddDocumentActivity.this);
                progressDialog.setMessage("🤖 Trợ lý AI đang đọc tài liệu và biên soạn câu hỏi trắc nghiệm, vui lòng chờ...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                // Cấu trúc System Prompt để ép Gemini trả về chuỗi định dạng JSON chuẩn xác
                String systemInstruction = "\n\n[HƯỚNG DẪN HỆ THỐNG]: Bạn là một Giảng viên đại học chuyên nghiệp. Hãy đọc kỹ đoạn văn bản tài liệu do sinh viên cung cấp ở trên, trích xuất kiến thức cốt lõi và biên soạn đúng 3 câu hỏi trắc nghiệm khách quan để kiểm tra kiến thức bài học."
                        + " Bạn BẮT BUỘC phải trả về kết quả ở CUỐI bài viết của mình dưới dạng một chuỗi JSON chuẩn xác theo cấu trúc sau, không được sai một ký tự nào: "
                        + "---QUIZ_START--- {\"quiz\": [{\"question\": \"Nội dung câu hỏi hỏi gì?\", \"options\": [\"Đáp án A\", \"Đáp án B\", \"Đáp án C\", \"Đáp án D\"], \"correctIndex\": 0}]} ---QUIZ_END---. "
                        + "Trong đó, fields 'correctIndex' là số nguyên từ 0 đến 3 đại diện cho vị trí đáp án đúng trong mảng 'options' (0 ứng với đáp án đầu tiên). Hãy đặt nội dung các câu hỏi thật sát kiến thức.";

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

                                // Truyền chuỗi JSON thô sang cho QuizActivity bóc tách và hiển thị
                                Intent intent = new Intent(AddDocumentActivity.this, QuizActivity.class);
                                intent.putExtra("QUIZ_JSON", jsonResult);
                                startActivity(intent);
                            } else {
                                Toast.makeText(AddDocumentActivity.this, "AI phản hồi bận, vui lòng thử nhấn lại!", Toast.LENGTH_SHORT).show();
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
        });
    }
}