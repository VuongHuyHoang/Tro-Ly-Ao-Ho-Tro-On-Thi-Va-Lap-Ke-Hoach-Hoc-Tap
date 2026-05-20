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
import com.google.android.material.button.MaterialButtonToggleGroup;
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

    private TextView tvMonthYearTitle, tvSelectedDateHeader;
    private RecyclerView recyclerCalendarGrid, recyclerCalendarTasks;
    // Đã thêm layoutEmptyState vào khai báo
    private LinearLayout layoutAgendaSheet, layoutWeekdayHeader, layoutEmptyState;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private MaterialButtonToggleGroup toggleGroupViewMode;
    private ImageButton btnPreviousMonth, btnNextMonth;

    private TaskAdapter agendaAdapter;
    private CalendarGridAdapter calendarGridAdapter;
    private GridLayoutManager gridLayoutManager;

    private List<StudyTask> allTasksList = new ArrayList<>();
    private List<StudyTask> displayTasksList = new ArrayList<>();
    private List<Calendar> daysInGridList = new ArrayList<>();

    private Calendar currentMonthCalendar = Calendar.getInstance();
    private Calendar currentSelectedDate = Calendar.getInstance();
    private String currentViewMode = "MONTH";

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
        layoutWeekdayHeader = view.findViewById(R.id.layout_weekday_header);
        toggleGroupViewMode = view.findViewById(R.id.toggle_group_view_mode);
        btnPreviousMonth = view.findViewById(R.id.btn_previous_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);

        // TÍNH NĂNG MỚI: Ánh xạ giao diện Trạng thái trống
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);

        bottomSheetBehavior = BottomSheetBehavior.from(layoutAgendaSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Khởi tạo danh sách chi tiết công việc
        agendaAdapter = new TaskAdapter(displayTasksList);
        recyclerCalendarTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerCalendarTasks.setAdapter(agendaAdapter);

        // TÍNH NĂNG MỚI: Vuốt sang trái để xóa công việc (Swipe to Delete)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                StudyTask taskToDelete = displayTasksList.get(position);

                // HIỂN THỊ HỘP THOẠI XÁC NHẬN TRƯỚC KHI XÓA
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Xác nhận xóa công việc")
                        .setMessage("Bạn có chắc chắn muốn xóa \"" + taskToDelete.getTaskName() + "\" không? Thao tác này không thể hoàn tác.")
                        .setCancelable(false) // Buộc người dùng phải chọn 1 trong 2 nút, không cho bấm ra ngoài để tắt
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            // LỰA CHỌN 1: Người dùng đồng ý xóa
                            displayTasksList.remove(position);
                            agendaAdapter.notifyItemRemoved(position);
                            checkEmptyState();

                            // Tiến hành xóa dữ liệu trên Firebase Firestore
                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            if (auth.getCurrentUser() != null) {
                                String currentUid = auth.getCurrentUser().getUid();
                                String documentId = String.valueOf(taskToDelete.getId());

                                if (documentId.equals("null") || documentId.equals("0")) {
                                    documentId = taskToDelete.getTaskName();
                                }

                                FirebaseFirestore.getInstance()
                                        .collection("users")
                                        .document(currentUid)
                                        .collection("user_tasks")
                                        .document(documentId)
                                        .delete()
                                        .addOnFailureListener(e -> {
                                            // Nếu lỗi mạng không xóa được -> Hoàn tác lại giao diện
                                            displayTasksList.add(position, taskToDelete);
                                            agendaAdapter.notifyItemInserted(position);
                                            checkEmptyState();
                                        });
                            }
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            // LỰA CHỌN 2: Người dùng lỡ tay vuốt nhầm và bấm Hủy
                            // Ra lệnh cho Adapter nạp lại dòng này để dòng chữ trượt đóng ngược trở lại vị trí cũ
                            agendaAdapter.notifyItemChanged(position);
                            dialog.dismiss();
                        })
                        .show();
            }
        }).attachToRecyclerView(recyclerCalendarTasks);

        // Quản lý Grid Layout linh hoạt
        gridLayoutManager = new GridLayoutManager(requireContext(), 7);
        recyclerCalendarGrid.setLayoutManager(gridLayoutManager);

        calendarGridAdapter = new CalendarGridAdapter(daysInGridList, allTasksList, currentMonthCalendar.get(Calendar.MONTH), date -> {
            currentSelectedDate = (Calendar) date.clone();
            if (currentViewMode.equals("DAY")) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            filterAndDisplayTasks();
        });
        recyclerCalendarGrid.setAdapter(calendarGridAdapter);

        btnPreviousMonth.setOnClickListener(v -> {
            if (currentViewMode.equals("MONTH")) {
                currentMonthCalendar.add(Calendar.MONTH, -1);
            } else if (currentViewMode.equals("WEEK")) {
                currentMonthCalendar.add(Calendar.WEEK_OF_YEAR, -1);
            } else {
                currentSelectedDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            refreshCalendarState();
        });

        btnNextMonth.setOnClickListener(v -> {
            if (currentViewMode.equals("MONTH")) {
                currentMonthCalendar.add(Calendar.MONTH, 1);
            } else if (currentViewMode.equals("WEEK")) {
                currentMonthCalendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                currentSelectedDate.add(Calendar.DAY_OF_MONTH, 1);
            }
            refreshCalendarState();
        });

        setupViewModeToggle();
        refreshCalendarState();
        loadTasksFromFirestore();
    }

    private void refreshCalendarState() {
        generateCalendarGrid();
        updateHeaderTitle();
        filterAndDisplayTasks();
    }

    private void setupViewModeToggle() {
        toggleGroupViewMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btn_view_month) {
                currentViewMode = "MONTH";
                gridLayoutManager.setSpanCount(7);
                layoutWeekdayHeader.setVisibility(View.VISIBLE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            } else if (checkedId == R.id.btn_view_week) {
                currentViewMode = "WEEK";
                gridLayoutManager.setSpanCount(7);
                layoutWeekdayHeader.setVisibility(View.VISIBLE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            } else if (checkedId == R.id.btn_view_day) {
                currentViewMode = "DAY";
                gridLayoutManager.setSpanCount(1);
                layoutWeekdayHeader.setVisibility(View.GONE);
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
            refreshCalendarState();
        });
    }

    private void updateHeaderTitle() {
        SimpleDateFormat sdfMonth = new SimpleDateFormat("'Tháng' MM 'năm' yyyy", new Locale("vi", "VN"));
        if (currentViewMode.equals("MONTH")) {
            tvMonthYearTitle.setText(sdfMonth.format(currentMonthCalendar.getTime()));
        } else if (currentViewMode.equals("WEEK")) {
            int weekOfYear = currentMonthCalendar.get(Calendar.WEEK_OF_YEAR);
            tvMonthYearTitle.setText("Tuần " + weekOfYear + " (" + (currentMonthCalendar.get(Calendar.MONTH) + 1) + "/" + currentMonthCalendar.get(Calendar.YEAR) + ")");
        } else {
            SimpleDateFormat sdfDay = new SimpleDateFormat("dd 'Tháng' MM, yyyy", new Locale("vi", "VN"));
            tvMonthYearTitle.setText("Ngày " + sdfDay.format(currentSelectedDate.getTime()));
        }
    }

    private void generateCalendarGrid() {
        daysInGridList.clear();
        Calendar calendar = (Calendar) currentMonthCalendar.clone();

        if (currentViewMode.equals("MONTH")) {
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int spacesBefore = firstDayOfWeek - 1;

            calendar.add(Calendar.DAY_OF_MONTH, -spacesBefore);
            while (daysInGridList.size() < 42) {
                daysInGridList.add((Calendar) calendar.clone());
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else if (currentViewMode.equals("WEEK")) {
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            calendar.add(Calendar.DAY_OF_MONTH, -(currentDayOfWeek - 1));
            for (int i = 0; i < 7; i++) {
                daysInGridList.add((Calendar) calendar.clone());
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else {
            daysInGridList.add((Calendar) currentSelectedDate.clone());
        }

        if (calendarGridAdapter != null) {
            calendarGridAdapter.notifyDataSetChanged();
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

        for (StudyTask task : allTasksList) {
            if (selectedDateStr.equals(task.getDeadline())) {
                displayTasksList.add(task);
            }
        }

        Collections.sort(displayTasksList, new Comparator<StudyTask>() {
            @Override
            public int compare(StudyTask t1, StudyTask t2) {
                int compComplete = Boolean.compare(t1.isCompleted(), t2.isCompleted());
                if (compComplete != 0) return compComplete;
                return getPriorityWeight(t2.getPriority()) - getPriorityWeight(t1.getPriority());
            }

            private int getPriorityWeight(String priority) {
                if (priority == null) return 1;
                switch (priority) {
                    case "Cao": return 3;
                    case "Trung bình": return 2;
                    default: return 1;
                }
            }
        });

        agendaAdapter.notifyDataSetChanged();
        checkEmptyState(); // TÍNH NĂNG MỚI: Cập nhật UI nếu không có việc
    }

    // TÍNH NĂNG MỚI: Hàm kiểm tra ẩn/hiện giao diện trống
    private void checkEmptyState() {
        if (displayTasksList.isEmpty()) {
            recyclerCalendarTasks.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerCalendarTasks.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }
}