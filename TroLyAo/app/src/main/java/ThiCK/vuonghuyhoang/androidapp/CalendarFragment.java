package ThiCK.vuonghuyhoang.androidapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private com.google.android.material.button.MaterialButton btnBackToToday, btnSmartSuggest;
    private TextView tvMonthYearTitle, tvSelectedDateHeader;
    private RecyclerView recyclerCalendarGrid, recyclerCalendarTasks;
    private LinearLayout layoutAgendaSheet, layoutEmptyState;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private ImageButton btnPreviousMonth, btnNextMonth;

    private TaskAdapter agendaAdapter;
    private CalendarGridAdapter calendarGridAdapter;

    private List<StudyTask> allTasksList = new ArrayList<>();
    private List<StudyTask> displayTasksList = new ArrayList<>();
    private List<TaskWrapper> agendaWrappers = new ArrayList<>();
    private List<Calendar> daysInGridList = new ArrayList<>();

    private Calendar currentMonthCalendar = Calendar.getInstance();
    private Calendar currentSelectedDate = Calendar.getInstance();

    public CalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Locale.setDefault(new Locale("vi", "VN"));
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMonthYearTitle = view.findViewById(R.id.tv_month_year_title);
        tvSelectedDateHeader = view.findViewById(R.id.tv_selected_date_header);
        recyclerCalendarGrid = view.findViewById(R.id.recycler_calendar_grid);
        recyclerCalendarTasks = view.findViewById(R.id.recycler_calendar_tasks);
        layoutAgendaSheet = view.findViewById(R.id.layout_agenda_sheet);
        btnPreviousMonth = view.findViewById(R.id.btn_previous_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        btnSmartSuggest = view.findViewById(R.id.btn_smart_suggest);
        btnSmartSuggest.setOnClickListener(v -> showOptimizationDialog());

        btnBackToToday = view.findViewById(R.id.btn_back_to_today);


        bottomSheetBehavior = BottomSheetBehavior.from(layoutAgendaSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // TÍNH NĂNG MỚI: Lắng nghe chuyển động của khung Bottom Sheet
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Khi người dùng vuốt ẩn khung đi -> Trả lịch về dạng lưới to
                    if (calendarGridAdapter != null) calendarGridAdapter.setCompactMode(false);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    // Khi khung công việc hiện lên -> Ép lịch co rút lại
                    if (calendarGridAdapter != null) calendarGridAdapter.setCompactMode(true);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Không cần xử lý hiệu ứng trượt giữa chừng
            }
        });

        // --- THAY THẾ ĐOẠN LỖI BẰNG ĐOẠN CODE NÀY ---
        agendaAdapter = new TaskAdapter(agendaWrappers, new TaskAdapter.OnHeaderClickListener() {
            @Override
            public void onHeaderClick(String categoryName, boolean isCurrentlyExpanded) {
                // Không làm gì cả (Bên lịch hiển thị phẳng, không có Accordion)
            }

            @Override
            public void onHeaderLongClick(String categoryName) {
                // Không làm gì cả (Bên lịch không hỗ trợ nhấn giữ để xóa cụm)
            }
        });
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCalendarTasks.setAdapter(agendaAdapter);

        // Chức năng vuốt xóa công việc
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudyTask taskToDelete = displayTasksList.get(position);

                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận xóa công việc")
                        .setMessage("Bạn có chắc chắn muốn xóa \"" + taskToDelete.getTaskName() + "\" không? Thao tác này không thể hoàn tác.")
                        .setCancelable(false)
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            displayTasksList.remove(position);
                            agendaAdapter.notifyItemRemoved(position);
                            checkEmptyState();

                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            if (auth.getCurrentUser() != null) {
                                String currentUid = auth.getCurrentUser().getUid();
                                String documentId = String.valueOf(taskToDelete.getId());
                                if (documentId.equals("null") || documentId.equals("0")) {
                                    documentId = taskToDelete.getTaskName();
                                }
                                FirebaseFirestore.getInstance().collection("users").document(currentUid)
                                        .collection("user_tasks").document(documentId).delete()
                                        .addOnFailureListener(e -> {
                                            displayTasksList.add(position, taskToDelete);
                                            agendaAdapter.notifyItemInserted(position);
                                            checkEmptyState();
                                        });
                            }
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            agendaAdapter.notifyItemChanged(position);
                            dialog.dismiss();
                        }).show();
            }
        }).attachToRecyclerView(recyclerCalendarTasks);

        // Lưới lịch mặc định luôn là 7 cột
        recyclerCalendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));

        calendarGridAdapter = new CalendarGridAdapter(daysInGridList, allTasksList, currentMonthCalendar.get(Calendar.MONTH), date -> {
            currentSelectedDate = (Calendar) date.clone();
            calendarGridAdapter.setSelectedDate(currentSelectedDate);

            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            filterAndDisplayTasks();
        });
        recyclerCalendarGrid.setAdapter(calendarGridAdapter);

        calendarGridAdapter.setSelectedDate(currentSelectedDate);

        // Nút trở về Hôm nay
        btnBackToToday.setOnClickListener(v -> {
            currentMonthCalendar = Calendar.getInstance();
            currentSelectedDate = Calendar.getInstance();
            calendarGridAdapter.setSelectedDate(currentSelectedDate);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            refreshCalendarState();
        });

        // Nút qua Tháng trước
        btnPreviousMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, -1);
            refreshCalendarState();
        });

        // Nút sang Tháng sau
        btnNextMonth.setOnClickListener(v -> {
            currentMonthCalendar.add(Calendar.MONTH, 1);
            refreshCalendarState();
        });

        refreshCalendarState();
        loadTasksFromFirestore();
    }

    private void refreshCalendarState() {
        generateCalendarGrid();
        updateHeaderTitle();
        filterAndDisplayTasks();
    }

    // Hiển thị chuẩn chỉ chữ "Tháng MM năm YYYY"
    private void updateHeaderTitle() {
        SimpleDateFormat sdfMonth = new SimpleDateFormat("'Tháng' MM 'năm' yyyy", new Locale("vi", "VN"));
        tvMonthYearTitle.setText(sdfMonth.format(currentMonthCalendar.getTime()));
    }

    // Sinh luôn 42 ô lịch cố định cho Tháng
    private void generateCalendarGrid() {
        daysInGridList.clear();
        Calendar calendar = (Calendar) currentMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int spacesBefore = firstDayOfWeek - 1;

        calendar.add(Calendar.DAY_OF_MONTH, -spacesBefore);
        while (daysInGridList.size() < 42) {
            daysInGridList.add((Calendar) calendar.clone());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (calendarGridAdapter != null) {
            // Cập nhật lại thuộc tính tháng hiện tại để mờ bớt ngày tháng trước/sau
            calendarGridAdapter = new CalendarGridAdapter(daysInGridList, allTasksList, currentMonthCalendar.get(Calendar.MONTH), date -> {
                currentSelectedDate = (Calendar) date.clone();
                calendarGridAdapter.setSelectedDate(currentSelectedDate);
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                filterAndDisplayTasks();
            });
            calendarGridAdapter.setSelectedDate(currentSelectedDate);
            recyclerCalendarGrid.setAdapter(calendarGridAdapter);
        }
    }

    private void loadTasksFromFirestore() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String currentUid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUid)
                .collection("user_tasks")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    allTasksList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        StudyTask task = doc.toObject(StudyTask.class);
                        if (task != null) allTasksList.add(task);
                    }
                    if (calendarGridAdapter != null) calendarGridAdapter.notifyDataSetChanged();
                    filterAndDisplayTasks();
                });
    }

    private void filterAndDisplayTasks() {
        displayTasksList.clear();
        SimpleDateFormat sdfVietnamese = new SimpleDateFormat("EEEE, 'Ngày' dd 'Tháng' MM, yyyy", new Locale("vi", "VN"));
        tvSelectedDateHeader.setText(sdfVietnamese.format(currentSelectedDate.getTime()));

        String selectedDateStr = String.format("%02d/%02d/%d", currentSelectedDate.get(Calendar.DAY_OF_MONTH), currentSelectedDate.get(Calendar.MONTH) + 1, currentSelectedDate.get(Calendar.YEAR));

        // 1. Lọc ra các công việc có ngày bằng với ngày đang chọn trên lịch
        for (StudyTask task : allTasksList) {
            if (selectedDateStr.equals(task.getDeadline())) {
                displayTasksList.add(task);
            }
        }

        Collections.sort(displayTasksList, new Comparator<StudyTask>() {
            @Override
            public int compare(StudyTask t1, StudyTask t2) {
                // Tiêu chí 1: Nếu 1 cái đã làm xong, đẩy nó xuống dưới cùng
                int compComplete = Boolean.compare(t1.isCompleted(), t2.isCompleted());
                if (compComplete != 0) return compComplete;

                // Tiêu chí 2: Sắp xếp theo Thời gian (Due Time) - Từ sáng đến tối
                String time1 = t1.getDueTime() != null ? t1.getDueTime() : "23:59";
                String time2 = t2.getDueTime() != null ? t2.getDueTime() : "23:59";
                int compTime = time1.compareTo(time2);

                // NẾU KHÁC GIỜ: Sắp xếp theo giờ bình thường
                if (compTime != 0) {
                    return compTime;
                }

                // NẾU TRÙNG GIỜ (compTime == 0): Tiêu chí 3 là Độ ưu tiên (Cao lên trước)
                return getPriorityWeight(t2.getPriority()) - getPriorityWeight(t1.getPriority());
            }

            // Hàm phụ trợ tính điểm ưu tiên (Tái sử dụng lại logic của bạn)
            private int getPriorityWeight(String priority) {
                if (priority == null) return 1;
                switch (priority) {
                    case "Cao": return 3;
                    case "Trung bình": return 2;
                    default: return 1; // Thấp
                }
            }
        });

        // 3. Đóng gói lại vào Wrapper và cập nhật giao diện
        agendaWrappers.clear();
        for (StudyTask task : displayTasksList) {
            agendaWrappers.add(new TaskWrapper(task));
        }
        agendaAdapter.notifyDataSetChanged();
        checkEmptyState();
    }

    private void checkEmptyState() {
        if (displayTasksList.isEmpty()) {
            recyclerCalendarTasks.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerCalendarTasks.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
    // TÍNH NĂNG MỚI: Hộp thoại và Logic Tối ưu hóa bằng AI / Giải thuật
    private void showOptimizationDialog() {
        // Lọc ra danh sách task chưa hoàn thành trong ngày đang chọn
        List<StudyTask> pendingTasks = new ArrayList<>();
        for (StudyTask t : displayTasksList) {
            if (!t.isCompleted()) pendingTasks.add(t);
        }

        if (pendingTasks.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "Không có công việc nào cần tối ưu hôm nay!", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo ô nhập liệu để hỏi thời gian rảnh
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Nhập số phút (VD: 120)");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setPadding(60, 20, 60, 0);
        layout.addView(input, new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Bạn rảnh bao nhiêu phút?")
                .setMessage("Thuật toán Knapsack sẽ tính toán tổ hợp công việc mang lại hiệu suất điểm số cao nhất trong khoảng thời gian này.")
                .setView(layout)
                .setPositiveButton("Bắt đầu tối ưu", (dialog, which) -> {
                    String timeStr = input.getText().toString().trim();
                    if (timeStr.isEmpty()) return;
                    int freeTime = Integer.parseInt(timeStr);

                    // GỌI THUẬT TOÁN ĐIỂM NHẤN
                    List<StudyTask> result = TaskOptimizer.getOptimalTasks(pendingTasks, freeTime);

                    if (result.isEmpty()) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Kết quả tối ưu")
                                .setMessage("Rất tiếc, thời gian của bạn quá ngắn, không đủ để hoàn thành trọn vẹn bất kỳ công việc nào trong danh sách.")
                                .setPositiveButton("Đóng", null)
                                .show();
                    } else {
                        StringBuilder sb = new StringBuilder("Để đạt hiệu quả cao nhất, hãy làm theo lộ trình sau:\n\n");
                        int totalTime = 0;
                        int totalScore = 0;
                        for (StudyTask t : result) {
                            sb.append("✅ ").append(t.getTaskName())
                                    .append(" (").append(t.getEstimatedMinutes()).append(" phút)\n");
                            totalTime += t.getEstimatedMinutes();
                            totalScore += t.getPriority().equals("Cao") ? 3 : (t.getPriority().equals("Trung bình") ? 2 : 1);
                        }
                        sb.append("\n⏱ Tổng thời gian chiếm dụng: ").append(totalTime).append("/").append(freeTime).append(" phút");
                        sb.append("\n⭐ Tổng điểm ưu tiên đạt được: ").append(totalScore);

                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Lộ trình tối ưu của bạn")
                                .setMessage(sb.toString())
                                .setPositiveButton("Tuyệt vời", null)
                                .show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}