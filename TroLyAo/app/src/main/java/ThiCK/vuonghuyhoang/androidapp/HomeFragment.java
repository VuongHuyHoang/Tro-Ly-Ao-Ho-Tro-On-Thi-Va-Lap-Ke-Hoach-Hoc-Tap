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
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private List<TaskWrapper> wrapperList = new ArrayList<>();
    // Set dùng để ghi nhớ những danh mục nào đang được người dùng bấm mở ra
    private java.util.HashSet<String> expandedCategories = new java.util.HashSet<>();

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

        // --- THAY THẾ ĐOẠN KHỞI TẠO ADAPTER CŨ BẰNG ĐOẠN NÀY ---
        firebaseTasks = new ArrayList<>();

        taskAdapter = new TaskAdapter(wrapperList, new TaskAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(String categoryName, boolean isCurrentlyExpanded) {
                // Sự kiện đóng mở danh mục (Giữ nguyên logic cũ của bạn)
                if (isCurrentlyExpanded) {
                    expandedCategories.remove(categoryName);
                } else {
                    expandedCategories.add(categoryName);
                }
                rebuildWrapperList();
            }

            @Override
            public void onHeaderLongClick(String categoryName) {
                // Kích hoạt hộp thoại hỏi xóa toàn bộ danh mục khi nhấn giữ
                showDeleteCategoryDialog(categoryName);
            }
        });

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

                                // THAY VÌ SORT VÀ NOTIFY Ở ĐÂY, TA GỌI HÀM GOM NHÓM
                                rebuildWrapperList();
                            }
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
    // Bộ xử lý vuốt chạm
    private void configureSwipeActions() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            // --- THÊM HÀM NÀY ĐỂ CHẶN VUỐT HEADER ---
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                if (viewHolder instanceof TaskAdapter.HeaderViewHolder) {
                    return 0; // Trả về 0 nghĩa là không cho phép vuốt dòng Header này
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Không cho phép thả công việc đè lên thanh Tiêu đề
                if (target.getItemViewType() == TaskWrapper.TYPE_HEADER) {
                    return false;
            }
                // Đổi chỗ 2 phần tử trong danh sách hiển thị
                java.util.Collections.swap(wrapperList, fromPosition, toPosition);

                // Báo cho Adapter biết để tạo hiệu ứng animation trượt mượt mà
                taskAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // Khi người dùng thả tay ra, lập tức kích hoạt hàm lưu vị trí mới lên Firebase
                updateOrderInFirebase();
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Lấy item từ wrapperList thay vì firebaseTasks
                TaskWrapper wrapperItem = wrapperList.get(position);

                // Đảm bảo chỉ xử lý vuốt nếu nó là một Task
                if (wrapperItem.type == TaskWrapper.TYPE_TASK) {
                    StudyTask selectedTask = wrapperItem.task;

                    if (direction == ItemTouchHelper.RIGHT) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Xác nhận xóa tác vụ")
                                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn công việc \"" + selectedTask.getTaskName() + "\" không?")
                                .setCancelable(false)
                                .setPositiveButton("Xóa", (dialog, which) -> {
                                    executeDeleteTask(selectedTask, position);
                                })
                                .setNegativeButton("Hủy", (dialog, which) -> {
                                    taskAdapter.notifyItemChanged(position);
                                    dialog.dismiss();
                                })
                                .show();

                    } else if (direction == ItemTouchHelper.LEFT) {
                        showTaskDetailBottomSheet(selectedTask);
                        taskAdapter.notifyItemChanged(position);
                    }
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

        TextView tvDetailDescription = sheetView.findViewById(R.id.tv_detail_description);
        TextView tvDetailTitle = sheetView.findViewById(R.id.tv_detail_title);
        TextView tvDetailDeadline = sheetView.findViewById(R.id.tv_detail_deadline);
        TextView tvDetailEstimatedTime = sheetView.findViewById(R.id.tv_detail_estimated_time);
        TextView tvDetailStatus = sheetView.findViewById(R.id.tv_detail_status);
        Chip chipDetailPriority = sheetView.findViewById(R.id.chip_detail_priority);
        MaterialButton btnCloseSheet = sheetView.findViewById(R.id.btn_close_sheet);

        tvDetailTitle.setText(task.getTaskName());
        // Lấy giờ an toàn (nếu null thì mặc định 23:59)
        String timeStr = (task.getDueTime() != null && !task.getDueTime().isEmpty()) ? task.getDueTime() : "23:59";

        if (tvDetailEstimatedTime != null) {
            tvDetailEstimatedTime.setText("Thời gian dự kiến hoàn thành: " + task.getEstimatedMinutes() + " phút");
        }

        // Hiển thị chuẩn form: Hạn chót: 14:30 - 25/05/2026
        tvDetailDeadline.setText("Hạn chót: " + timeStr + " - " + task.getDeadline());
        chipDetailPriority.setText("Độ ưu tiên: " + task.getPriority());

        // Đổ dữ liệu Mô tả chi tiết
        if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
            tvDetailDescription.setText(task.getDescription());
            tvDetailDescription.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            tvDetailDescription.setText("Không có ghi chú hoặc mô tả.");
            tvDetailDescription.setTypeface(null, android.graphics.Typeface.ITALIC); // In nghiêng nếu không có mô tả
        }
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

    // TÍCH HỢP: Hàm hiển thị Bottom Sheet để thêm Task thủ công (ĐÃ NÂNG CẤP THÊM GIỜ & THỜI LƯỢNG)
    // TÍCH HỢP: Hàm hiển thị Bottom Sheet để thêm Task thủ công (ĐÃ NÂNG CẤP THÊM MÔ TẢ)
    private void showAddTaskBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.layout_bottom_sheet_add_task, null);

        android.widget.AutoCompleteTextView edtCategory = sheetView.findViewById(R.id.edt_task_category);
        TextInputEditText edtName = sheetView.findViewById(R.id.edt_task_name);

        // 1. [BỔ SUNG] ÁNH XẠ Ô NHẬP MÔ TẢ MỚI TỪ XML
        TextInputEditText edtDescription = sheetView.findViewById(R.id.edt_task_description);

        com.google.android.material.textfield.TextInputLayout layoutDeadline = sheetView.findViewById(R.id.layout_task_deadline);
        TextInputEditText edtDeadline = sheetView.findViewById(R.id.edt_task_deadline);
        RadioGroup radioGroupPriority = sheetView.findViewById(R.id.radio_group_priority);
        MaterialButton btnSave = sheetView.findViewById(R.id.btn_save_new_task);

        if (edtCategory != null) {
            java.util.Set<String> uniqueCategories = new java.util.HashSet<>();
            for (StudyTask task : firebaseTasks) {
                if (task.getCategory() != null && !task.getCategory().trim().isEmpty()) {
                    uniqueCategories.add(task.getCategory());
                }
            }
            List<String> categoryList = new ArrayList<>(uniqueCategories);
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryList
            );
            edtCategory.setAdapter(adapter);

            edtCategory.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) edtCategory.showDropDown();
            });
            edtCategory.setOnClickListener(v -> edtCategory.showDropDown());
        }

        if (layoutDeadline != null) {
            layoutDeadline.setEndIconOnClickListener(v -> showDatePicker(edtDeadline));
        }

        TextInputEditText edtTime = sheetView.findViewById(R.id.edt_task_time);
        TextInputEditText edtEstimated = sheetView.findViewById(R.id.edt_task_estimated_minutes);

        if (edtTime != null) {
            edtTime.setFocusable(false);
            edtTime.setClickable(true);
            edtTime.setOnClickListener(v -> showTimePicker(edtTime));
        }

        btnSave.setOnClickListener(v -> {
            String taskName = edtName.getText().toString().trim();
            String deadline = edtDeadline.getText().toString().trim();

            // 2. [BỔ SUNG] LẤY CHUỖI DỮ LIỆU TỪ Ô MÔ TẢ
            String taskDescription = "";
            if (edtDescription != null && edtDescription.getText() != null) {
                taskDescription = edtDescription.getText().toString().trim();
            }

            String category = "";
            if (edtCategory != null && edtCategory.getText() != null) {
                category = edtCategory.getText().toString().trim();
            }
            if (category.isEmpty()) {
                category = "Công việc chung";
            }

            String dueTime = (edtTime != null && edtTime.getText() != null && !edtTime.getText().toString().isEmpty())
                    ? edtTime.getText().toString().trim() : "23:59";

            String estimatedStr = (edtEstimated != null && edtEstimated.getText() != null && !edtEstimated.getText().toString().isEmpty())
                    ? edtEstimated.getText().toString().trim() : "60";

            if (taskName.isEmpty() || deadline.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ tên và hạn chót!", Toast.LENGTH_SHORT).show();
                return;
            }

            int estimatedMinutes = 60;
            try {
                estimatedMinutes = Integer.parseInt(estimatedStr);
            } catch (NumberFormatException e) {
            }

            String priority = "Trung bình";
            int checkedId = radioGroupPriority.getCheckedRadioButtonId();
            if (checkedId == R.id.rb_high) priority = "Cao";
            else if (checkedId == R.id.rb_low) priority = "Thấp";

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();
                int newTaskId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);

                // 3. [BỔ SUNG] TRUYỀN BIẾN taskDescription VÀO CONSTRUCTOR
                StudyTask newTask = new StudyTask(category, newTaskId, taskName, taskDescription, deadline, priority, dueTime, estimatedMinutes, false);

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

    private void showTimePicker(TextView tvTarget) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Chọn giờ hạn chót")
                .build();

        picker.show(getChildFragmentManager(), "TIME_PICKER");

        picker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            tvTarget.setText(time);
        });
    }
    // HÀM HIỂN THỊ LỊCH CHỌN NGÀY
    private void showDatePicker(TextView tvTarget) {
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Chọn ngày hạn chót")
                        .setSelection(com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Chuyển đổi mili-giây sang định dạng dd/MM/yyyy
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            tvTarget.setText(sdf.format(new java.util.Date(selection)));
        });

        datePicker.show(getChildFragmentManager(), "DATE_PICKER");
    }

    // THUẬT TOÁN GOM NHÓM DỮ LIỆU (DATA FLATTENING)
    private void rebuildWrapperList() {
        wrapperList.clear();

        // 1. Phân loại Task vào các nhóm (Map)
        java.util.Map<String, List<StudyTask>> groupedTasks = new java.util.HashMap<>();
        for (StudyTask task : firebaseTasks) {
            // Nếu task chưa có danh mục, gán vào "Công việc chung"
            String cat = (task.getCategory() != null && !task.getCategory().isEmpty()) ? task.getCategory() : "Công việc chung";

            if (!groupedTasks.containsKey(cat)) {
                groupedTasks.put(cat, new ArrayList<>());
            }
            groupedTasks.get(cat).add(task);
        }

        // 2. Chuyển đổi Map thành List<TaskWrapper> phẳng
        for (String categoryName : groupedTasks.keySet()) {
            boolean isExpanded = expandedCategories.contains(categoryName);

            // Luôn luôn thêm Thanh Tiêu Đề vào danh sách
            wrapperList.add(new TaskWrapper(categoryName, isExpanded));

            // Chỉ thêm các Task con vào danh sách NẾU thư mục đang mở
            if (isExpanded) {
                List<StudyTask> tasksInCat = groupedTasks.get(categoryName);

                // Sắp xếp Task con bên trong danh mục (theo độ ưu tiên và trạng thái)
                // Sắp xếp Task con bên trong danh mục
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    tasksInCat.sort((t1, t2) -> {
                        // 1. Đẩy các công việc đã tick Xong xuống dưới cùng
                        if (t1.isCompleted() != t2.isCompleted()) return t1.isCompleted() ? 1 : -1;

                        // 2. Sắp xếp theo vị trí người dùng đã kéo thả (orderIndex)
                        return Integer.compare(t1.getOrderIndex(), t2.getOrderIndex());
                    });
                }

                // Gói các task con lại và nhét xuống dưới tiêu đề
                for (StudyTask t : tasksInCat) {
                    wrapperList.add(new TaskWrapper(t));
                }
            }
        }

        // 3. Yêu cầu Adapter vẽ lại màn hình
        taskAdapter.notifyDataSetChanged();
    }
    // HÀM HIỂN THỊ HỘP THOẠI XÁC NHẬN XÓA SẠCH DANH MỤC
    private void showDeleteCategoryDialog(String categoryName) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("⚠️ Xóa toàn bộ danh sách")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn danh mục \"" + categoryName + "\" và TẤT CẢ các công việc con bên trong không? Thao tác này không thể khôi phục.")
                .setCancelable(false)
                .setPositiveButton("Xóa sạch", (dialog, which) -> {
                    executeDeleteCategory(categoryName);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // THUẬT TOÁN XÓA BATCH (WRITE BATCH) TRÊN FIREBASE
    private void executeDeleteCategory(String categoryName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();

            // Tìm tất cả các task có trường "category" khớp với tên danh mục được chọn
            FirebaseFirestore.getInstance()
                    .collection("users").document(currentUid)
                    .collection("user_tasks")
                    .whereEqualTo("category", categoryName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Khởi tạo một phiên Batch để gộp nhiều lệnh xóa thành một
                            com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();

                            // Duyệt qua tất cả documents tìm được và đưa lệnh xóa vào hàng đợi của Batch
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                batch.delete(doc.getReference());
                            }

                            // Thực thi gửi toàn bộ lệnh xóa lên Firebase cùng một lúc
                            batch.commit().addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "Đã xóa sạch danh mục \"" + categoryName + "\"!", Toast.LENGTH_SHORT).show();
                                // Xóa tên danh mục này khỏi bộ nhớ mở rộng (nếu có)
                                expandedCategories.remove(categoryName);
                            }).addOnFailureListener(e -> {
                                Toast.makeText(requireContext(), "Lỗi khi xóa hàng loạt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Lỗi kết nối dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
    private void updateOrderInFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String currentUid = auth.getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        int indexCounter = 0;

        // Duyệt qua toàn bộ danh sách Accordion đang hiển thị trên giao diện
        for (TaskWrapper wrapper : wrapperList) {
            // Chỉ cập nhật cho các item là Công việc con (Child), bỏ qua thanh tiêu đề (Header)
            if (wrapper.type == TaskWrapper.TYPE_TASK && wrapper.task != null) {

                // Lấy ID document Firestore của task này (Bạn thay bằng hàm getId() thực tế của bạn)
                String taskId = String.valueOf(wrapper.task.getId());

                if (taskId != null) {
                    com.google.firebase.firestore.DocumentReference docRef = db
                            .collection("users").document(currentUid)
                            .collection("user_tasks").document(taskId);

                    // Đưa lệnh cập nhật orderIndex mới vào hàng đợi của Batch
                    batch.update(docRef, "orderIndex", indexCounter);
                    indexCounter++; // Tăng số thứ tự lên 1 cho task tiếp theo
                }
            }
        }

        // Thực thi gửi tất cả lệnh cập nhật lên Firebase cùng lúc
        batch.commit().addOnSuccessListener(aVoid -> {
            // Cập nhật ngầm thành công, không cần Toast để tránh làm phiền người dùng
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(), "Lỗi đồng bộ vị trí: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}