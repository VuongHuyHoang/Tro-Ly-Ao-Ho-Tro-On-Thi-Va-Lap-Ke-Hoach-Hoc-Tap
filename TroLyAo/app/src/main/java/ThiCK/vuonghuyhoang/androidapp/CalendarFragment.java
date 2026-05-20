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

    private com.google.android.material.button.MaterialButton btnBackToToday;
    private TextView tvMonthYearTitle, tvSelectedDateHeader;
    private RecyclerView recyclerCalendarGrid, recyclerCalendarTasks;
    private LinearLayout layoutAgendaSheet, layoutEmptyState;
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;
    private ImageButton btnPreviousMonth, btnNextMonth;

    private TaskAdapter agendaAdapter;
    private CalendarGridAdapter calendarGridAdapter;

    private List<StudyTask> allTasksList = new ArrayList<>();
    private List<StudyTask> displayTasksList = new ArrayList<>();
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

        agendaAdapter = new TaskAdapter(displayTasksList);
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
}