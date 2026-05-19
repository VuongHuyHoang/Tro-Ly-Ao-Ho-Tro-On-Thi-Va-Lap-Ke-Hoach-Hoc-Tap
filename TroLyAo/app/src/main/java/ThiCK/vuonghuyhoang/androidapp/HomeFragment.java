package ThiCK.vuonghuyhoang.androidapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerTasks;
    private TaskAdapter taskAdapter;
    private List<StudyTask> firebaseTasks;

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
        super.onViewCreated(super.getView(), savedInstanceState);

        MaterialButton btnAddDocument = view.findViewById(R.id.btn_add_document);
        recyclerTasks = view.findViewById(R.id.recycler_tasks);

        // 2. Xử lý sự kiện click nút Thêm tài liệu
        btnAddDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ĐÃ SỬA: Chuyển hướng từ trang chủ sang màn hình tạo Quiz của AI
                Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
                startActivity(intent);
            }
        });

        firebaseTasks = new ArrayList<>();
        taskAdapter = new TaskAdapter(firebaseTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);
        recyclerTasks.setNestedScrollingEnabled(false);

        // ĐỊNH NGHĨA BỘ ĐIỀU KHIỂN VUỐT CHUYÊN BIỆT THEO HƯỚNG
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudyTask selectedTask = firebaseTasks.get(position);

                if (direction == ItemTouchHelper.RIGHT) {
                    // 1. VUỐT SANG PHẢI: HIỂN THỊ DIALOG XÁC NHẬN XÓA
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Xác nhận xóa tác vụ")
                            .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn công việc \"" + selectedTask.getTaskName() + "\" không?")
                            .setCancelable(false) // Không cho tắt dialog bằng cách bấm ra ngoài
                            .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    executeDeleteTask(selectedTask, position);
                                }
                            })
                            .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hoàn tác lại item trên giao diện RecyclerView nếu bấm Hủy
                                    taskAdapter.notifyItemChanged(position);
                                    dialog.dismiss();
                                }
                            })
                            .show();

                } else if (direction == ItemTouchHelper.LEFT) {
                    // 2. VUỐT SANG TRÁI: HIỂN THỊ BOTTOM SHEET CHI TIẾT TÁC VỤ
                    showTaskDetailBottomSheet(selectedTask);
                    // Sau khi mở Bottom Sheet, đưa item của RecyclerView về lại trạng thái bình thường
                    taskAdapter.notifyItemChanged(position);
                }
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerTasks);

        // Luồng lắng nghe tự động đồng bộ (Giữ nguyên)
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
                            taskAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    // HÀM XỬ LÝ LỆNH XÓA DỮ LIỆU TRÊN FIRESTORE
    private void executeDeleteTask(StudyTask task, int position) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUid)
                    .collection("user_tasks")
                    .document(String.valueOf(task.getId()))
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Đã xóa tác vụ thành công!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        taskAdapter.notifyItemChanged(position);
                    });
        }
    }

    // HÀM HIỂN THỊ BOTTOM SHEET CHI TIẾT CÔNG VIỆC
    private void showTaskDetailBottomSheet(StudyTask task) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Nạp giao diện XML của Bottom Sheet (Chúng ta tận dụng layout sạch đẹp thiết kế nhanh)
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_detail, null);

        TextView tvDetailTitle = sheetView.findViewById(R.id.tv_detail_title);
        TextView tvDetailDeadline = sheetView.findViewById(R.id.tv_detail_deadline);
        TextView tvDetailStatus = sheetView.findViewById(R.id.tv_detail_status);
        Chip chipDetailPriority = sheetView.findViewById(R.id.chip_detail_priority);
        MaterialButton btnCloseSheet = sheetView.findViewById(R.id.btn_close_sheet);

        // Đổ dữ liệu của Task được chọn vào giao diện Bottom Sheet
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
}