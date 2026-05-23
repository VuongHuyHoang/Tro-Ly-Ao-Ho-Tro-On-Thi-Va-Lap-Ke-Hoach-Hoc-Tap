package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QuizPreviewActivity extends AppCompatActivity {

    private RecyclerView recyclerPreview;
    private PreviewAdapter adapter;
    private List<PreviewQuestion> questionList;
    private String originalJson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_preview);

        ImageView btnBack = findViewById(R.id.btn_back_preview);
        recyclerPreview = findViewById(R.id.recycler_preview);
        MaterialButton btnConfirm = findViewById(R.id.btn_confirm_quiz);

        questionList = new ArrayList<>();
        adapter = new PreviewAdapter(questionList);
        recyclerPreview.setLayoutManager(new LinearLayoutManager(this));
        recyclerPreview.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        // 1. Nhận JSON từ màn hình Tạo Đề
        if (getIntent() != null && getIntent().hasExtra("QUIZ_JSON")) {
            originalJson = getIntent().getStringExtra("QUIZ_JSON");
            parseJsonToList(originalJson);
        } else {
            Toast.makeText(this, "Không tìm thấy dữ liệu đề thi!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 2. Xử lý khi ấn nút "Lưu và Làm bài"
        btnConfirm.setOnClickListener(v -> {
            String updatedJson = convertListToJson(questionList);
            if (updatedJson != null) {
                Intent intent = new Intent(QuizPreviewActivity.this, QuizActivity.class);
                intent.putExtra("QUIZ_JSON", updatedJson);
                startActivity(intent);
                finish(); // Đóng màn hình duyệt
            }
        });
    }

    // ================= PHẦN 1: XỬ LÝ DỮ LIỆU JSON =================
    private void parseJsonToList(String jsonString) {
        try {
            JSONObject root = new JSONObject(jsonString);
            JSONArray quizArray = root.getJSONArray("quiz");

            questionList.clear();
            for (int i = 0; i < quizArray.length(); i++) {
                JSONObject qObj = quizArray.getJSONObject(i);
                PreviewQuestion pq = new PreviewQuestion();
                pq.type = qObj.optString("type", "MULTIPLE_CHOICE");
                pq.question = qObj.optString("question", "");
                pq.correctIndex = qObj.optInt("correctIndex", 0);
                pq.explanation = qObj.optString("explanation", "");

                JSONArray opts = qObj.optJSONArray("options");
                pq.options = new ArrayList<>();
                if (opts != null) {
                    for (int j = 0; j < opts.length(); j++) {
                        pq.options.add(opts.getString(j));
                    }
                }
                questionList.add(pq);
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc dữ liệu AI: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String convertListToJson(List<PreviewQuestion> list) {
        try {
            JSONObject root = new JSONObject();
            JSONArray quizArray = new JSONArray();

            for (PreviewQuestion pq : list) {
                JSONObject qObj = new JSONObject();
                qObj.put("type", pq.type);
                qObj.put("question", pq.question);
                qObj.put("correctIndex", pq.correctIndex);
                qObj.put("explanation", pq.explanation);

                JSONArray opts = new JSONArray();
                for (String opt : pq.options) {
                    opts.put(opt);
                }
                qObj.put("options", opts);
                quizArray.put(qObj);
            }
            root.put("quiz", quizArray);
            return root.toString();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đóng gói dữ liệu!", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // ================= PHẦN 2: HIỂN THỊ HỘP THOẠI CHỈNH SỬA =================
    private void showEditQuestionDialog(int position) {
        PreviewQuestion pq = questionList.get(position);

        // Tạo giao diện Cuộn chứa các ô nhập liệu
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        // 1. Ô sửa câu hỏi
        TextInputLayout tipQuestion = new TextInputLayout(this);
        tipQuestion.setHint("Nội dung câu hỏi");
        tipQuestion.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        TextInputEditText edtQuestion = new TextInputEditText(this);
        edtQuestion.setText(pq.question);
        tipQuestion.addView(edtQuestion);
        layout.addView(tipQuestion);

        // Mẹo nhỏ cho người dùng
        TextView tvHint = new TextView(this);
        tvHint.setText("Mẹo: Để đổi đáp án đúng, hãy sửa trực tiếp text của đáp án đó.");
        tvHint.setTextSize(12);
        tvHint.setTextColor(0xFF64748B); // Màu xám nhạt
        tvHint.setPadding(0, 16, 0, 16);
        layout.addView(tvHint);

        // 2. Các ô sửa đáp án (Tự động sinh ra 2 hoặc 4 ô)
        List<TextInputEditText> optEdits = new ArrayList<>();
        for (int i = 0; i < pq.options.size(); i++) {
            TextInputLayout tipOpt = new TextInputLayout(this);
            tipOpt.setHint("Đáp án " + (i == pq.correctIndex ? "(ĐÚNG)" : (i + 1)));
            tipOpt.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            tipOpt.setPadding(0, 0, 0, 16); // Cách lề dưới

            TextInputEditText edtOpt = new TextInputEditText(this);
            edtOpt.setText(pq.options.get(i));

            // Highlight đáp án đúng bằng text màu xanh
            if (i == pq.correctIndex) edtOpt.setTextColor(0xFF4CAF50);

            tipOpt.addView(edtOpt);
            layout.addView(tipOpt);
            optEdits.add(edtOpt);
        }

        scrollView.addView(layout);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Sửa câu hỏi số " + (position + 1))
                .setView(scrollView)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    // Cập nhật lại dữ liệu vào Object
                    pq.question = edtQuestion.getText().toString().trim();
                    for (int i = 0; i < optEdits.size(); i++) {
                        pq.options.set(i, optEdits.get(i).getText().toString().trim());
                    }
                    adapter.notifyItemChanged(position); // Báo cho danh sách load lại dòng này
                    Toast.makeText(this, "Đã cập nhật câu hỏi!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // ================= PHẦN 3: LỚP BỘ CHUYỂN ĐỔI (ADAPTER & MODEL) =================
    private static class PreviewQuestion {
        String type;
        String question;
        List<String> options;
        int correctIndex;
        String explanation;
    }

    private class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.ViewHolder> {
        private List<PreviewQuestion> list;

        public PreviewAdapter(List<PreviewQuestion> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PreviewQuestion pq = list.get(position);
            holder.tvQuestion.setText("Câu " + (position + 1) + ": " + pq.question);

            StringBuilder answersStr = new StringBuilder();
            char bullet = 'A';
            for (int i = 0; i < pq.options.size(); i++) {
                if (i == pq.correctIndex) {
                    answersStr.append("✓ ").append(bullet).append(". ").append(pq.options.get(i)).append("\n");
                } else {
                    answersStr.append("   ").append(bullet).append(". ").append(pq.options.get(i)).append("\n");
                }
                bullet++;
            }
            holder.tvAnswers.setText(answersStr.toString().trim());

            // Bắt sự kiện chạm vào để sửa
            holder.itemView.setOnClickListener(v -> showEditQuestionDialog(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvQuestion, tvAnswers;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvQuestion = itemView.findViewById(R.id.tv_preview_question);
                tvAnswers = itemView.findViewById(R.id.tv_preview_answers);
            }
        }
    }
}