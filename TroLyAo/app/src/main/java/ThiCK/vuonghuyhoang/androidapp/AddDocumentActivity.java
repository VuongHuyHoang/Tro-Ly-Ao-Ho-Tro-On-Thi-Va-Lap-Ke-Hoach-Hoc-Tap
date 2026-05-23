package ThiCK.vuonghuyhoang.androidapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import ThiCK.vuonghuyhoang.androidapp.network.AiCallback;
import ThiCK.vuonghuyhoang.androidapp.network.GeminiClient;
import com.google.android.material.button.MaterialButtonToggleGroup;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AddDocumentActivity extends AppCompatActivity {

    private TextInputEditText edtDocumentContent;
    private MaterialButton btnGenerateQuiz, btnPickFile;
    private GeminiClient geminiClient;
    private android.app.ProgressDialog progressDialog;
    private MaterialButtonToggleGroup toggleDifficulty;

    // Bộ phóng trình chọn file hệ thống (Modern Storage API)
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        // Khởi tạo thư viện PDFBox (Bắt buộc)
        PDFBoxResourceLoader.init(getApplicationContext());

        android.widget.ImageView btnBackAddDoc = findViewById(R.id.btn_back_add_doc);
        toggleDifficulty = findViewById(R.id.toggle_difficulty);
        edtDocumentContent = findViewById(R.id.edt_document_content);
        btnGenerateQuiz = findViewById(R.id.btn_generate_quiz);
        btnPickFile = findViewById(R.id.btn_pick_file);
        geminiClient = new GeminiClient();

        btnBackAddDoc.setOnClickListener(v -> finish());

        // 1. ĐĂNG KÝ BỘ ĐÓN KẾT QUẢ CHỌN FILE
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (fileUri != null) {
                            handleSelectedFile(fileUri);
                        }
                    }
                }
        );

        // 2. SỰ KIỆN BẤM NÚT CHỌN FILE
        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Cho phép chọn mọi file, lọc đuôi ở code sau
            String[] mimetypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            filePickerLauncher.launch(intent);
        });

        btnGenerateQuiz.setOnClickListener(v -> {
            String content = edtDocumentContent.getText().toString().trim();
            if (content.isEmpty() || content.length() < 50) {
                Toast.makeText(AddDocumentActivity.this, "Nội dung tài liệu quá ngắn, vui lòng nhập chi tiết hơn!", Toast.LENGTH_SHORT).show();
                return;
            }

            int checkedId = toggleDifficulty.getCheckedButtonId();
            String difficultyPrompt = "mức độ TRUNG BÌNH. Câu hỏi yêu cầu sự thông hiểu, kết nối các dữ kiện cơ bản.";
            if (checkedId == R.id.btn_diff_easy) {
                difficultyPrompt = "mức độ CƠ BẢN. Câu hỏi chỉ tập trung vào ghi nhớ định nghĩa, nhận biết khái niệm đơn giản. Đáp án rõ ràng, không có bẫy.";
            } else if (checkedId == R.id.btn_diff_hard) {
                difficultyPrompt = "mức độ NÂNG CAO. Câu hỏi mang tính suy luận logic, vận dụng cao. Các đáp án gây nhiễu (bẫy) phải cực kỳ tinh vi.";
            }

            showQuantitySelectionDialog(content, difficultyPrompt);
        });
    }

    // ================= XỬ LÝ ĐỌC VÀ TRÍCH XUẤT CHỮ TỪ FILE =================
    private void handleSelectedFile(Uri uri) {
        // Hiện màn hình chờ đọc file (Kiểm tra tránh crash)
        if (!isFinishing()) {
            android.app.ProgressDialog readDialog = new android.app.ProgressDialog(this);
            readDialog.setMessage("⏳ Đang trích xuất văn bản từ file, vui lòng chờ...");
            readDialog.setCancelable(false);
            readDialog.show();

            // Chạy ngầm tránh lag giao diện
            new Thread(() -> {
                String extractedText = "";
                // SỬ DỤNG TRY-WITH-RESOURCES ĐỂ TỰ ĐỘNG ĐÓNG FILE NGAY CẢ KHI CÓ LỖI
                try (InputStream is = getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        String mimeType = getContentResolver().getType(uri);

                        if (mimeType != null && mimeType.contains("pdf")) {
                            // ĐỌC FILE PDF
                            PDDocument document = PDDocument.load(is);
                            PDFTextStripper stripper = new PDFTextStripper();
                            extractedText = stripper.getText(document);
                            document.close();
                        } else {
                            // ĐỌC FILE DOCX TỐI ƯU HƠN
                            try (ZipInputStream zipIs = new ZipInputStream(is)) {
                                ZipEntry entry;
                                StringBuilder docxContent = new StringBuilder();
                                while ((entry = zipIs.getNextEntry()) != null) {
                                    if (entry.getName().equals("word/document.xml")) {
                                        int byteRead;
                                        StringBuilder xmlContent = new StringBuilder();
                                        byte[] buffer = new byte[1024];
                                        while ((byteRead = zipIs.read(buffer)) != -1) {
                                            xmlContent.append(new String(buffer, 0, byteRead, "UTF-8"));
                                        }

                                        // BƯỚC CẢI TIẾN: Thay thẻ kết thúc đoạn văn bằng dấu xuống dòng trước khi xóa thẻ
                                        String xmlString = xmlContent.toString();
                                        xmlString = xmlString.replaceAll("</w:p>", "\n");

                                        // Xóa các thẻ XML còn lại
                                        String textOnly = xmlString.replaceAll("<[^>]*>", "");

                                        // Xóa khoảng trắng thừa nhưng vẫn giữ lại dấu xuống dòng
                                        docxContent.append(textOnly.replaceAll("[ \\t]+", " ").trim());
                                        break;
                                    }
                                }
                                extractedText = docxContent.toString();
                            }
                        }
                    }
                } catch (Exception e) {
                    extractedText = null;
                }

                // Trả kết quả về giao diện chính
                final String resultText = extractedText;
                runOnUiThread(() -> {
                    if (readDialog.isShowing() && !isFinishing()) {
                        readDialog.dismiss();
                    }
                    if (resultText != null && !resultText.isEmpty()) {
                        edtDocumentContent.setText(resultText);
                        Toast.makeText(this, "Đã tải nội dung file thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không thể đọc được file này hoặc file trống!", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        }
    }

    private void showQuantitySelectionDialog(String content, String difficultyPrompt) {
        String[] options = {"Tạo nhanh (3 câu hỏi)", "Tiêu chuẩn (5 câu hỏi)", "Thử thách (10 câu hỏi)", "Chuyên sâu (15 câu hỏi)"};
        int[] counts = {3, 5, 10, 15};

        new AlertDialog.Builder(this)
                .setTitle("Cấu hình số lượng câu hỏi")
                .setItems(options, (dialog, which) -> {
                    int selectedCount = counts[which];
                    generateQuizWithAi(content, selectedCount, difficultyPrompt);
                })
                .show();
    }

    private void generateQuizWithAi(String content, int questionCount, String difficultyPrompt) {
        if (isFinishing()) return;

        progressDialog = new android.app.ProgressDialog(AddDocumentActivity.this);
        progressDialog.setMessage("🤖 Trợ lý AI đang phân tích tài liệu và biên soạn bộ " + questionCount + " câu hỏi trắc nghiệm, vui lòng chờ...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String systemInstruction = "\n\n[HƯỚNG DẪN HỆ THỐNG RIGID]: Bạn là một Giảng viên đại học chuyên nghiệp. "
                + "Hãy đọc kỹ đoạn văn bản tài liệu do sinh viên cung cấp ở trên, trích xuất kiến thức cốt lõi và biên soạn đúng CHÍNH XÁC " + questionCount + " câu hỏi trắc nghiệm khách quan.\n"
                + "YÊU CẦU ĐỘ KHÓ: Bộ đề này phải ở " + difficultyPrompt + "\n"
                + "YÊU CẦU ĐA DẠNG HÓA: Hãy trộn ngẫu nhiên giữa 2 loại câu hỏi:\n"
                + "1. Loại 'MULTIPLE_CHOICE': Câu hỏi có 4 đáp án.\n"
                + "2. Loại 'TRUE_FALSE': Câu hỏi Đúng hoặc Sai (Mảng 'options' bắt buộc chỉ có đúng 2 phần tử là [\"Đúng\", \"Sai\"]).\n"
                + "Bạn BẮT BUỘC phải trả về kết quả dưới dạng một chuỗi JSON thuần túy, bọc đúng trong cặp thẻ đánh dấu cấu trúc như sau:\n"
                + "---QUIZ_START--- {\"quiz\": [{\"type\": \"MULTIPLE_CHOICE hoặc TRUE_FALSE\", \"question\": \"Nội dung câu hỏi?\", \"options\": [\"đáp án...\"], \"correctIndex\": 0, \"explanation\": \"Lời giải thích\"}]} ---QUIZ_END---\n"
                + "Lưu ý nghiêm ngặt: 'correctIndex' đại diện cho vị trí câu trả lời chính xác trong mảng 'options'. Tuyệt đối không kèm text ngoài lề.";

        String finalPrompt = content + systemInstruction;

        geminiClient.sendPrompt(finalPrompt, new AiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    // KIỂM TRA CHỐNG CRASH KHI TẮT DIALOG
                    if (progressDialog != null && progressDialog.isShowing() && !isFinishing()) {
                        progressDialog.dismiss();
                    }

                    if (response.contains("---QUIZ_START---") && response.contains("---QUIZ_END---")) {
                        int startIndex = response.indexOf("---QUIZ_START---") + "---QUIZ_START---".length();
                        int endIndex = response.indexOf("---QUIZ_END---");
                        String jsonResult = response.substring(startIndex, endIndex).trim();

                        Intent intent = new Intent(AddDocumentActivity.this, QuizPreviewActivity.class);
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
                    if (progressDialog != null && progressDialog.isShowing() && !isFinishing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(AddDocumentActivity.this, "Lỗi kết nối AI: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}