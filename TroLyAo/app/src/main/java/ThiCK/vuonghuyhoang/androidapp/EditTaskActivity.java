package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtDescription, edtCategory, edtDeadline, edtTime, edtEstimated;
    private TextInputLayout layoutDeadline, layoutTime;
    private RadioGroup radioGroupPriority;
    private RadioButton rbHigh, rbMedium, rbLow;
    private MaterialButton btnSave, btnDelete;
    private ImageView btnBack;

    private StudyTask currentTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        initViews();
        getIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_edit);
        edtName = findViewById(R.id.edt_edit_name);
        edtDescription = findViewById(R.id.edt_edit_description);
        edtCategory = findViewById(R.id.edt_edit_category);
        layoutDeadline = findViewById(R.id.layout_edit_deadline);
        edtDeadline = findViewById(R.id.edt_edit_deadline);
        layoutTime = findViewById(R.id.layout_edit_time);
        edtTime = findViewById(R.id.edt_edit_time);
        edtEstimated = findViewById(R.id.edt_edit_estimated);
        radioGroupPriority = findViewById(R.id.radio_group_edit_priority);
        rbHigh = findViewById(R.id.rb_edit_high);
        rbMedium = findViewById(R.id.rb_edit_medium);
        rbLow = findViewById(R.id.rb_edit_low);
        btnSave = findViewById(R.id.btn_save_edit_task);
        btnDelete = findViewById(R.id.btn_delete_task);
    }

    private void getIntentData() {
        String taskJson = getIntent().getStringExtra("TASK_JSON");
        if (taskJson != null && !taskJson.isEmpty()) {
            currentTask = new Gson().fromJson(taskJson, StudyTask.class);
            populateUi();
        } else {
            Toast.makeText(this, "Không tìm thấy dữ liệu công việc!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void populateUi() {
        edtName.setText(currentTask.getTaskName());
        edtDescription.setText(currentTask.getDescription() != null ? currentTask.getDescription() : "");
        edtCategory.setText(currentTask.getCategory());
        edtDeadline.setText(currentTask.getDeadline());
        edtTime.setText(currentTask.getDueTime() != null ? currentTask.getDueTime() : "23:59");
        edtEstimated.setText(String.valueOf(currentTask.getEstimatedMinutes()));

        String prio = currentTask.getPriority() != null ? currentTask.getPriority() : "Trung bình";
        switch (prio) {
            case "Cao": rbHigh.setChecked(true); break;
            case "Thấp": rbLow.setChecked(true); break;
            default: rbMedium.setChecked(true); break;
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // --- 1. SỰ KIỆN CHỌN NGÀY DÙNG CHUNG ---
        View.OnClickListener dateClickListener = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Chọn ngày hạn chót")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                edtDeadline.setText(sdf.format(new Date(selection)));
            });
            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        };

        // Gắn sự kiện mở lịch cho cả Icon và Ô nhập chữ
        layoutDeadline.setEndIconOnClickListener(dateClickListener);
        edtDeadline.setOnClickListener(dateClickListener);


        // --- 2. SỰ KIỆN CHỌN GIỜ DÙNG CHUNG ---
        View.OnClickListener timeClickListener = v -> {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(12)
                    .setMinute(0)
                    .setTitleText("Chọn giờ hạn chót")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v1 -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                edtTime.setText(time);
            });
            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        };

        // Gắn sự kiện mở đồng hồ cho cả Icon và Ô nhập chữ
        layoutTime.setEndIconOnClickListener(timeClickListener);
        edtTime.setOnClickListener(timeClickListener);


        // --- 3. CÁC SỰ KIỆN LƯU VÀ XÓA ---
        // Sự kiện click nút Lưu thay đổi
        btnSave.setOnClickListener(v -> saveTaskToFirebase());

        // Sự kiện click nút Xóa tác vụ kèm xác nhận thoại
        btnDelete.setOnClickListener(v -> showConfirmDeleteDialog());
    }

    private void saveTaskToFirebase() {
        String name = edtName.getText().toString().trim();
        String desc = edtDescription.getText().toString().trim();
        String cat = edtCategory.getText().toString().trim();
        String deadline = edtDeadline.getText().toString().trim();
        String time = edtTime.getText().toString().trim();
        String estimatedStr = edtEstimated.getText().toString().trim();

        if (name.isEmpty() || deadline.isEmpty()) {
            Toast.makeText(this, "Tên công việc và hạn chót không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        int estimatedMins = 60;
        try { estimatedMins = Integer.parseInt(estimatedStr); } catch (Exception e) {}

        String priority = "Trung bình";
        int checkedId = radioGroupPriority.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_edit_high) priority = "Cao";
        else if (checkedId == R.id.rb_edit_low) priority = "Thấp";

        currentTask.setTaskName(name);
        currentTask.setDescription(desc);
        currentTask.setCategory(cat.isEmpty() ? "Công việc chung" : cat);
        currentTask.setDeadline(deadline);
        currentTask.setDueTime(time.isEmpty() ? "23:59" : time);
        currentTask.setEstimatedMinutes(estimatedMins);
        currentTask.setCategory(cat.isEmpty() ? "Công việc chung" : cat);
        // Tận dụng setter có sẵn để ép chuỗi độ ưu tiên nếu cần (hoặc tạo setter trong Model nếu thiếu)
        // Lưu ý: Đảm bảo Model StudyTask có hàm setPriority() nếu muốn thay đổi, ở đây ta cập nhật trực tiếp Map lên Firestore cho an toàn:

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        final String finalPriority = priority;
        // Tạo object cập nhật đồng bộ toàn bộ trường lên Firestore
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("user_tasks").document(String.valueOf(currentTask.getId()))
                .set(currentTask) // Ghi đè Object đã chỉnh sửa lên vị trí document ID cũ
                .addOnSuccessListener(aVoid -> {
                    // Cập nhật lại độ ưu tiên riêng biệt nếu constructor không gán
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .collection("user_tasks").document(String.valueOf(currentTask.getId()))
                            .update("priority", finalPriority)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Đã cập nhật thay đổi thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showConfirmDeleteDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa tác vụ")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn công việc \"" + currentTask.getTaskName() + "\" không? Thao tác này không thể khôi phục.")
                .setCancelable(false)
                .setPositiveButton("Xóa", (dialog, which) -> executeDelete())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeDelete() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("user_tasks").document(String.valueOf(currentTask.getId()))
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa tác vụ thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}