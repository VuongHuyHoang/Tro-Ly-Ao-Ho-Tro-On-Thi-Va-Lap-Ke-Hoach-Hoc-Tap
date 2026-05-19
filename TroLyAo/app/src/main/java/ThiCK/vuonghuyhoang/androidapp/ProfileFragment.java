package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileFragment extends Fragment {

    private TextView tvCountDone, tvCountPending;
    private TextView tvProfileName, tvProfileInfo, tvAvatarText;
    private MaterialButton btnLogout;

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

        // Ánh xạ các view thống kê và điều khiển
        tvCountDone = view.findViewById(R.id.tv_count_done);
        tvCountPending = view.findViewById(R.id.tv_count_pending);
        btnLogout = view.findViewById(R.id.btn_logout);

        // Ánh xạ các view hiển thị thông tin sinh viên có dấu mới thêm
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileInfo = view.findViewById(R.id.tv_profile_info);
        tvAvatarText = view.findViewById(R.id.tv_avatar_text);

        // Xử lý sự kiện đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Đăng xuất khỏi hệ thống Firebase đám mây
                FirebaseAuth.getInstance().signOut();

                // Trả người dùng về lại màn hình Login và xóa sạch các activity trước đó
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
                Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        // 1. Kiểm tra trạng thái xác thực và lấy UID cá nhân
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 2. ĐỌC THÔNG TIN TIẾNG VIỆT CÓ DẤU (Đọc 1 lần duy nhất bằng hàm get())
            db.collection("profiles")
                    .document(currentUid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        // Kiểm tra an toàn xem Fragment còn hiển thị trên màn hình hay không
                        if (!isAdded() || getActivity() == null) return;

                        if (documentSnapshot.exists()) {
                            UserProfile userProfile = documentSnapshot.toObject(UserProfile.class);
                            if (userProfile != null) {
                                // Gán họ tên có dấu lên giao diện
                                tvProfileName.setText(userProfile.getFullName());

                                // Gán chuỗi MSSV và Lớp vào dòng mô tả phụ
                                tvProfileInfo.setText("MSSV: " + userProfile.getStudentId() + " | Lớp: " + userProfile.getClassName());

                                // Thuật toán bóc tách từ cuối cùng để lấy chữ cái đầu làm ký tự Avatar (Ví dụ: "Huy Hoàng" -> lấy chữ "H")
                                String fullName = userProfile.getFullName().trim();
                                if (!fullName.isEmpty()) {
                                    String[] nameParts = fullName.split("\\s+");
                                    String lastName = nameParts[nameParts.length - 1];
                                    if (!lastName.isEmpty()) {
                                        tvAvatarText.setText(lastName.substring(0, 1).toUpperCase());
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Lỗi tải thông tin cá nhân: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

            // 3. ĐẾM SỐ LƯỢNG TASK REALTIME (Lắng nghe liên tục bằng addSnapshotListener)
            db.collection("users")
                    .document(currentUid)
                    .collection("user_tasks")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                            if (error != null || value == null) return;

                            int doneCount = 0;
                            int pendingCount = 0;

                            for (DocumentSnapshot doc : value.getDocuments()) {
                                StudyTask task = doc.toObject(StudyTask.class);
                                if (task != null) {
                                    if (task.isCompleted()) {
                                        doneCount++;
                                    } else {
                                        pendingCount++;
                                    }
                                }
                            }

                            // Cập nhật số lượng thống kê cá nhân lên màn hình
                            tvCountDone.setText(String.valueOf(doneCount));
                            tvCountPending.setText(String.valueOf(pendingCount));
                        }
                    });
        } else {
            tvCountDone.setText("0");
            tvCountPending.setText("0");
        }
    }
}