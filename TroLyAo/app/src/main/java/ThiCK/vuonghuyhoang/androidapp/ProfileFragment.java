package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvCountDone, tvCountPending;
    private TextView tvProfileName, tvProfileInfo, tvAvatarText;
    private MaterialButton btnLogout, btnEditProfile;

    // Các biến lưu thông tin hiện tại để truyền vào Bottom Sheet
    private String currentName = "";
    private String currentMssv = "";
    private PieChart pieChart;
    private com.google.android.material.button.MaterialButton btnQuizHistory;
    private String currentClass = "";

    public ProfileFragment() {
        // Constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các view cũ
        tvCountDone = view.findViewById(R.id.tv_count_done);
        tvCountPending = view.findViewById(R.id.tv_count_pending);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileInfo = view.findViewById(R.id.tv_profile_info);
        tvAvatarText = view.findViewById(R.id.tv_avatar_text);
        btnQuizHistory = view.findViewById(R.id.btn_quiz_history);
        pieChart = view.findViewById(R.id.pieChart);

        // Ánh xạ nút sửa mới thêm
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        btnQuizHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizHistoryActivity.class);
            startActivity(intent);
        });

        // 1. Sự kiện nút Chỉnh sửa hồ sơ
        btnEditProfile.setOnClickListener(v -> showEditProfileBottomSheet());

        // Sự kiện nút Đăng xuất (Giữ nguyên)
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
            Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
        });

        // 2. Đọc thông tin và gán dữ liệu ban đầu từ Firestore
        loadUserProfileData();

        // 3. Đếm số lượng task realtime bằng SnapshotListener (Giữ nguyên đoạn cũ của bạn)
        loadRealtimeTaskCounts();
    }


    private void loadUserProfileData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("profiles")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Kiểm tra an toàn: Đảm bảo Fragment vẫn đang gắn với Activity
                        if (!isAdded() || getActivity() == null) return;

                        if (documentSnapshot.exists()) {
                            UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                            if (userProfile != null) {
                                // Lưu trữ lại vào biến toàn cục để phục vụ sửa đổi
                                currentName = userProfile.getFullName();
                                currentMssv = userProfile.getStudentId();
                                currentClass = userProfile.getClassName();

                                // Đổ lên giao diện TextViews
                                updateProfileUi(currentName, currentMssv, currentClass);
                            }

                            // CẬP NHẬT GIAO DIỆN NÚT BẤM LỊCH SỬ THAY VÌ TEXTVIEW
                            if (documentSnapshot.contains("highScore")) {
                                long highScore = documentSnapshot.getLong("highScore");
                                // Kiểm tra biến btnQuizHistory đã được ánh xạ chưa trước khi set chữ
                                if (btnQuizHistory != null) {
                                    btnQuizHistory.setText("Lịch sử ôn tập (Điểm cao: " + highScore + ")");
                                }
                            }
                        }
                    });
        }
    }

    // Hàm cập nhật chữ và Avatar ký tự lên UI mảnh nhỏ
    private void updateProfileUi(String name, String mssv, String className) {
        tvProfileName.setText(name);
        tvProfileInfo.setText("MSSV: " + mssv + " | Lớp: " + className);

        String trimmedName = name.trim();
        if (!trimmedName.isEmpty()) {
            String[] nameParts = trimmedName.split("\\s+");
            String lastName = nameParts[nameParts.length - 1];
            if (!lastName.isEmpty()) {
                tvAvatarText.setText(lastName.substring(0, 1).toUpperCase());
            }
        }
    }

    // HÀM HIỂN THỊ BOTTOM SHEET SỬA THÔNG TIN CÁ NHÂN
    private void showEditProfileBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_edit_profile, null);

        TextInputEditText edtName = sheetView.findViewById(R.id.edt_edit_name);
        TextInputEditText edtMssv = sheetView.findViewById(R.id.edt_edit_mssv);
        TextInputEditText edtClass = sheetView.findViewById(R.id.edt_edit_class);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save_profile);

        // Tự động điền dữ liệu cũ đang hiển thị vào ô nhập liệu
        edtName.setText(currentName);
        edtMssv.setText(currentMssv);
        edtClass.setText(currentClass);

        btnSave.setOnClickListener(v -> {
            String newName = edtName.getText().toString().trim();
            String newMssv = edtMssv.getText().toString().trim();
            String newClass = edtClass.getText().toString().trim();

            if (newName.isEmpty() || newMssv.isEmpty() || newClass.isEmpty()) {
                Toast.makeText(requireContext(), "Không được để trống bất kỳ trường nào!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();

                // Tạo map đóng gói các trường cần chỉnh sửa cập nhật dập đè
                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", newName);
                updates.put("studentId", newMssv);
                updates.put("className", newClass);

                FirebaseFirestore.getInstance()
                        .collection("profiles")
                        .document(currentUid)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            // Cập nhật lại biến RAM hiện tại
                            currentName = newName;
                            currentMssv = newMssv;
                            currentClass = newClass;

                            // Cập nhật ngay lập tức lên màn hình TextView mà không cần tải lại app
                            updateProfileUi(newName, newMssv, newClass);

                            Toast.makeText(requireContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void loadRealtimeTaskCounts() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .collection("user_tasks")
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null) return;
                        int doneCount = 0;
                        int pendingCount = 0;
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            StudyTask task = doc.toObject(StudyTask.class);
                            if (task != null) {
                                if (task.isCompleted()) doneCount++;
                                else pendingCount++;
                            }
                        }
                        tvCountDone.setText(String.valueOf(doneCount));
                        tvCountPending.setText(String.valueOf(pendingCount));

                        setupPieChart(doneCount, pendingCount);
                    });
        }
    }
    private void setupPieChart(int completed, int pending) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(completed, "Hoàn thành"));
        entries.add(new PieEntry(pending, "Đang xử lý"));

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Thiết lập màu sắc (Xanh lá cho hoàn thành, Đỏ cam cho đang xử lý)
        int[] colors = {Color.parseColor("#4CAF50"), Color.parseColor("#FF5722")};
        dataSet.setColors(colors);
        dataSet.setValueTextSize(18f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Cấu hình thẩm mỹ
        com.github.mikephil.charting.components.Legend legend = pieChart.getLegend();
        legend.setTextSize(12f); // Tăng kích thước chữ chú thích
        legend.setFormSize(12f); // Tăng kích thước ô vuông màu bên cạnh chữ chú thích
        legend.setXEntrySpace(25f);       // Tăng khoảng cách ngang giữa mục "Hoàn thành" và "Đang xử lý"
        legend.setFormToTextSpace(10f);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f); // Tạo lỗ ở giữa (biểu đồ vòng)
        pieChart.setTransparentCircleRadius(45f);
        pieChart.animateY(1000); // Hiệu ứng xoay khi hiện ra
        pieChart.invalidate(); // Vẽ lại
    }
}