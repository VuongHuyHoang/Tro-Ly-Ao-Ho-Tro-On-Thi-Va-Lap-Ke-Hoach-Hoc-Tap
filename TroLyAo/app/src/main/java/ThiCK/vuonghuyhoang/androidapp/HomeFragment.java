package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter taskAdapter;
    private List<StudyTask> firebaseTasks;

    // Khai báo biến quản lý giao diện trống
    private View layoutEmptyState;

    public HomeFragment() {
        // Yêu cầu một public constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnAddDocument = view.findViewById(R.id.btn_add_document);
        MaterialButton btnAddTaskManual = view.findViewById(R.id.btn_add_task_manual); // Ánh xạ nút thêm thủ công mới
        recyclerTasks = view.findViewById(R.id.recycler_tasks);

        // Ánh xạ vùng Empty State từ layout XML
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        // Sự kiện thêm tài liệu AI
        btnAddDocument.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
            startActivity(intent);
        });

        // TÍCH HỢP: Sự kiện gọi Bottom Sheet thêm Task thủ công
        btnAddTaskManual.setOnClickListener(v -> {
            showAddTaskBottomSheet();
        });

        firebaseTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(firebaseTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);
        recyclerTasks.setNestedScrollingEnabled(false);

        // Bộ công cụ cấu hình vuốt chạm (ItemTouchHelper)
        configureSwipeActions();

        // Lắng nghe dữ liệu thời gian thực và áp dụng thuật toán tối ưu hiển thị
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();

            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .collection("user_tasks")
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;

                        if (value != null) {
                            firebaseTasks.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                StudyTask task = doc.toObject(StudyTask.class);
                                if (task != null) firebaseTasks.add(task);
                            }

                            // LUỒNG KIỂM TRA EMPTY STATE
                            if (firebaseTasks.isEmpty()) {
                                layoutEmptyState.setVisibility(View.VISIBLE);
                                recyclerTasks.setVisibility(View.GONE);
                            } else {
                                layoutEmptyState.setVisibility(View.GONE);
                                recyclerTasks.setVisibility(View.VISIBLE);

                                // THUẬT TOÁN SẮP XẾP TỰ ĐỘNG (SORTING LOGIC)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    firebaseTasks.sort((t1, t2) -> {
                                        // So sánh trạng thái hoàn thành trước
                                        if (t1.isCompleted() != t2.isCompleted()) {
                                            return t1.isCompleted() ? 1 : -1;
                                        }
                                        // Nếu cùng trạng thái hoàn thành, so sánh theo trọng số độ ưu tiên
                                        return getPriorityWeight(t2.getPriority()) - getPriorityWeight(t1.getPriority());
                                    });
                                }
                            }

                            // Yêu cầu Adapter vẽ lại danh sách sau khi đã sắp xếp hoàn chỉnh
                            taskAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // Hàm phụ trợ tính toán trọng số độ ưu tiên phục vụ giải thuật sắp xếp
    private int getPriorityWeight(String priority) {
        if (priority == null) return 0;
        switch (priority) {
            case "Cao": return 3;
            case "Trung bình": return 2;
            case "Thấp": return 1;
            default: return 0;
        }
    }

    // Bộ xử lý vuốt chạm
    private void configureSwipeActions() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudyTask selectedTask = firebaseTasks.get(position);

                // Khi vuốt sang PHẢI: Hiển thị hộp thoại xác nhận xóa chuẩn Material giống bên Lịch
                if (direction == ItemTouchHelper.RIGHT) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Xác nhận xóa tác vụ")
                            .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn công việc \"" + selectedTask.getTaskName() + "\" không? Thao tác này không thể hoàn tác.")
                            .setCancelable(false) // Buộc người dùng phải chọn nút, không cho bấm ra ngoài màn hình để tắt
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                // Gọi hàm thực thi xóa Firebase gốc của bạn
                                executeDeleteTask(selectedTask, position);
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> {
                                // Hoàn tác hoạt ảnh trượt, đưa thanh công việc về vị trí cũ
                                taskAdapter.notifyItemChanged(position);
                                dialog.dismiss();
                            })
                            .show();

                    // Khi vuốt sang TRÁI: Mở khung xem chi tiết (Giữ nguyên logic cũ)
                } else if (direction == ItemTouchHelper.LEFT) {
                    showTaskDetailBottomSheet(selectedTask);
                    taskAdapter.notifyItemChanged(position);
                }
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerTasks);
    }

    private void executeDeleteTask(StudyTask task, int position) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("users").document(currentUid)
                    .collection("user_tasks").document(String.valueOf(task.getId()))
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Đã xóa tác vụ thành công!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        taskAdapter.notifyItemChanged(position);
                    });
        }
    }

    private void showTaskDetailBottomSheet(StudyTask task) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_detail, null);

        TextView tvDetailTitle = sheetView.findViewById(R.id.tv_detail_title);
        TextView tvDetailDeadline = sheetView.findViewById(R.id.tv_detail_deadline);
        TextView tvDetailStatus = sheetView.findViewById(R.id.tv_detail_status);
        Chip chipDetailPriority = sheetView.findViewById(R.id.chip_detail_priority);
        MaterialButton btnCloseSheet = sheetView.findViewById(R.id.btn_close_sheet);

        tvDetailTitle.setText(task.getTaskName());
        tvDetailDeadline.setText("Hạn định hoàn thành: " + task.getDeadline());
        chipDetailPriority.setText("Độ ưu tiên: " + task.getPriority());

        if (task.isCompleted()) {
            tvDetailStatus.setText("Trạng thái: Đã hoàn thành hoàn tất 🎉");
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
        } else {
            tvDetailStatus.setText("Trạng thái: Đang trong hàng đợi xử lý ⏳");
            tvDetailStatus.setTextColor(android.graphics.Color.parseColor("#C62828"));
        }

        btnCloseSheet.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    // TÍCH HỢP: Hàm hiển thị Bottom Sheet để thêm Task thủ công
    private void showAddTaskBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_add_task, null);

        com.google.android.material.textfield.TextInputEditText edtName = sheetView.findViewById(R.id.edt_task_name);
        com.google.android.material.textfield.TextInputEditText edtDeadline = sheetView.findViewById(R.id.edt_task_deadline);
        android.widget.RadioGroup radioGroupPriority = sheetView.findViewById(R.id.radio_group_priority);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save_new_task);

        btnSave.setOnClickListener(v -> {
            String taskName = edtName.getText().toString().trim();
            String deadline = edtDeadline.getText().toString().trim();

            if (taskName.isEmpty() || deadline.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ tên và hạn chót!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lấy giá trị độ ưu tiên từ RadioButton
            String priority = "Trung bình";
            int checkedId = radioGroupPriority.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_high) {
                priority = "Cao";
            } else if (checkedId == R.id.rb_low) {
                priority = "Thấp";
            }

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();

                // Sinh ID duy nhất bằng timestamp
                int newTaskId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

                // ĐÃ SỬA CHỖ NÀY: Dùng Constructor đầy đủ tham số cho khớp với StudyTask của bạn
                StudyTask newTask = new StudyTask(newTaskId, taskName, deadline, priority, false);

                // Lưu lên Firestore
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUid)
                        .collection("user_tasks")
                        .document(String.valueOf(newTaskId))
                        .set(newTask)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Đã thêm công việc thành công!", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

}