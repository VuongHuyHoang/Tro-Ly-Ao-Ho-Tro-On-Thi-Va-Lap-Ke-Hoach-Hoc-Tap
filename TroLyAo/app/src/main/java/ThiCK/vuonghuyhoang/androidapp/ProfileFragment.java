package ThiCK.vuonghuyhoang.androidapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private de.hdodenhof.circleimageview.CircleImageView imgMainAvatar;
    private TextView tvCountDone, tvCountPending;
    private TextView tvProfileName, tvProfileInfo, tvAvatarText;
    private MaterialButton btnLogout, btnEditProfile;

    private String currentName = "";
    private String currentMssv = "";
    private String currentClass = "";
    private String currentAvatarUri = ""; // Bổ sung biến lưu đường dẫn ảnh

    private PieChart pieChart;
    private MaterialButton btnQuizHistory;

    // --- BIẾN QUẢN LÝ ẢNH ĐẠI DIỆN TRONG BOTTOM SHEET ---
    private CircleImageView currentBottomSheetAvatar;
    private Uri tempSelectedImageUri = null; // Lưu tạm ảnh người dùng vừa chọn nhưng chưa bấm Lưu
    private ActivityResultLauncher<Intent> pickImageLauncher;

    public ProfileFragment() {
        // Constructor rỗng
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ĐĂNG KÝ BỘ ĐÓN KẾT QUẢ CHỌN ẢNH NGAY KHI FRAGMENT VỪA TẠO
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        tempSelectedImageUri = result.getData().getData();
                        // Đổ ảnh lên Avatar trong Bottom Sheet ngay lập tức
                        if (currentBottomSheetAvatar != null && tempSelectedImageUri != null) {
                            currentBottomSheetAvatar.setImageURI(tempSelectedImageUri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgMainAvatar = view.findViewById(R.id.img_main_avatar);
        tvCountDone = view.findViewById(R.id.tv_count_done);
        tvCountPending = view.findViewById(R.id.tv_count_pending);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileInfo = view.findViewById(R.id.tv_profile_info);
        tvAvatarText = view.findViewById(R.id.tv_avatar_text);
        btnQuizHistory = view.findViewById(R.id.btn_quiz_history);
        pieChart = view.findViewById(R.id.pieChart);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        btnQuizHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), QuizHistoryActivity.class);
            startActivity(intent);
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileBottomSheet());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
            Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
        });

        loadUserProfileData();
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
                        if (!isAdded() || getActivity() == null) return;

                        if (documentSnapshot.exists()) {
                            UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                            if (userProfile != null) {
                                currentName = userProfile.getFullName();
                                currentMssv = userProfile.getStudentId();
                                currentClass = userProfile.getClassName();

                                // Lấy đường dẫn ảnh cũ nếu có
                                if (documentSnapshot.contains("avatarUri")) {
                                    currentAvatarUri = documentSnapshot.getString("avatarUri");
                                }

                                updateProfileUi(currentName, currentMssv, currentClass);
                                // TODO (Tương lai): Đổ currentAvatarUri lên Avatar tròn ngoài trang chính (nếu bạn có thiết kế)
                            }

                            if (documentSnapshot.contains("highScore")) {
                                long highScore = documentSnapshot.getLong("highScore");
                                if (btnQuizHistory != null) {
                                    btnQuizHistory.setText("Lịch sử ôn tập (Điểm cao: " + highScore + ")");
                                }
                            }
                        }
                    });
        }
    }

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

        if (currentAvatarUri != null && !currentAvatarUri.isEmpty()) {
            imgMainAvatar.setVisibility(View.VISIBLE);

            com.bumptech.glide.Glide.with(this)
                    .load(android.net.Uri.parse(currentAvatarUri))
                    .into(imgMainAvatar);
        } else {
            imgMainAvatar.setVisibility(View.GONE);
        }
    }

    private void showEditProfileBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_edit_profile, null);

        TextInputEditText edtName = sheetView.findViewById(R.id.edt_edit_name);
        TextInputEditText edtMssv = sheetView.findViewById(R.id.edt_edit_mssv);
        TextInputEditText edtClass = sheetView.findViewById(R.id.edt_edit_class);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save_profile);

        // --- ÁNH XẠ AVATAR VÀ BẮT SỰ KIỆN CHỌN ẢNH ---
        currentBottomSheetAvatar = sheetView.findViewById(R.id.img_edit_avatar);
        tempSelectedImageUri = null; // Reset biến tạm mỗi lần mở Sheet

        if (currentAvatarUri != null && !currentAvatarUri.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(android.net.Uri.parse(currentAvatarUri))
                    .into(currentBottomSheetAvatar);
        }

        // Sự kiện: Bấm vào ảnh để đổi ảnh mới
        currentBottomSheetAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        edtName.setText(currentName);
        edtMssv.setText(currentMssv);
        edtClass.setText(currentClass);

        btnSave.setOnClickListener(v -> {
            String newName = edtName.getText().toString().trim();
            String newMssv = edtMssv.getText().toString().trim();
            String newClass = edtClass.getText().toString().trim();

            if (newName.isEmpty() || newMssv.isEmpty() || newClass.isEmpty()) {
                Toast.makeText(requireContext(), "Không được để trống thông tin chữ!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();

                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", newName);
                updates.put("studentId", newMssv);
                updates.put("className", newClass);

                // Nếu người dùng có chọn ảnh mới, lưu luôn đường dẫn Uri đó lên Firestore
                if (tempSelectedImageUri != null) {
                    updates.put("avatarUri", tempSelectedImageUri.toString());
                }

                FirebaseFirestore.getInstance()
                        .collection("profiles")
                        .document(currentUid)
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            currentName = newName;
                            currentMssv = newMssv;
                            currentClass = newClass;

                            // Cập nhật đường dẫn gốc
                            if (tempSelectedImageUri != null) {
                                currentAvatarUri = tempSelectedImageUri.toString();
                                // Bổ sung: Gọi hàm update avatar ngoài trang chủ ở đây nếu bạn muốn
                            }

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

        int[] colors = {Color.parseColor("#4CAF50"), Color.parseColor("#FF5722")};
        dataSet.setColors(colors);
        dataSet.setValueTextSize(18f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        com.github.mikephil.charting.components.Legend legend = pieChart.getLegend();
        legend.setTextSize(12f);
        legend.setFormSize(12f);
        legend.setXEntrySpace(25f);
        legend.setFormToTextSpace(10f);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }
}