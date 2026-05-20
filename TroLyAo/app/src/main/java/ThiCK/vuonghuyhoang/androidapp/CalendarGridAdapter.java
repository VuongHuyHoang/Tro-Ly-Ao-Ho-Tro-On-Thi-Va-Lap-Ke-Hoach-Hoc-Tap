package ThiCK.vuonghuyhoang.androidapp;

import java.util.Collections;
import java.util.Comparator;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarGridAdapter extends RecyclerView.Adapter<CalendarGridAdapter.CalendarViewHolder> {

    private Calendar selectedDateGlobal;
    private final List<Calendar> daysOfMonth;
    private final List<StudyTask> allTasks;
    private final int currentMonth;
    private final OnItemClickListener listener;

    // TÍNH NĂNG MỚI: Biến lưu trạng thái hiển thị thu gọn (mặc định là false -> Lịch to)
    private boolean isCompactMode = false;

    public interface OnItemClickListener {
        void onItemClick(Calendar date);
    }

    public void setSelectedDate(Calendar selectedDate) {
        this.selectedDateGlobal = selectedDate;
        notifyDataSetChanged();
    }

    // TÍNH NĂNG MỚI: Hàm bật/tắt chế độ thu nhỏ lịch
    public void setCompactMode(boolean compact) {
        if (this.isCompactMode != compact) {
            this.isCompactMode = compact;
            notifyDataSetChanged();
        }
    }

    public CalendarGridAdapter(List<Calendar> daysOfMonth, List<StudyTask> allTasks, int currentMonth, OnItemClickListener listener) {
        this.daysOfMonth = daysOfMonth;
        this.allTasks = allTasks;
        this.currentMonth = currentMonth;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_cell, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        Calendar day = daysOfMonth.get(position);

        if (day == null) {
            holder.tvCellDay.setText("");
            holder.tvTaskOverflow.setVisibility(View.GONE);
            holder.tvMiniTask1.setVisibility(View.GONE);
            holder.tvMiniTask2.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setBackgroundResource(R.drawable.calendar_cell_border);
            return;
        }

        holder.tvCellDay.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

        // --- 1. XỬ LÝ MÀU SẮC (Giữ nguyên) ---
        Calendar today = Calendar.getInstance();
        boolean isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

        boolean isSelected = selectedDateGlobal != null &&
                day.get(Calendar.YEAR) == selectedDateGlobal.get(Calendar.YEAR) &&
                day.get(Calendar.MONTH) == selectedDateGlobal.get(Calendar.MONTH) &&
                day.get(Calendar.DAY_OF_MONTH) == selectedDateGlobal.get(Calendar.DAY_OF_MONTH);

        if (day.get(Calendar.MONTH) != currentMonth) {
            holder.tvCellDay.setTextColor(Color.parseColor("#C4C7C9"));
        } else {
            holder.tvCellDay.setTextColor(Color.parseColor("#1A1C1E"));
        }

        if (isToday) {
            holder.tvCellDay.setBackgroundResource(R.drawable.bg_today_circle);
            holder.tvCellDay.setTextColor(Color.WHITE);
            holder.itemView.setBackgroundResource(R.drawable.calendar_cell_border);
        } else if (isSelected) {
            holder.tvCellDay.setBackground(null);
            holder.itemView.setBackgroundResource(R.drawable.calendar_cell_selected);
        } else {
            holder.tvCellDay.setBackground(null);
            holder.itemView.setBackgroundResource(R.drawable.calendar_cell_border);
        }

        // --- 2. QUÉT VÀ SẮP XẾP TASK (Giữ nguyên) ---
        List<StudyTask> dayTasks = new ArrayList<>();
        String cellDateStr = String.format("%02d/%02d/%d", day.get(Calendar.DAY_OF_MONTH), day.get(Calendar.MONTH) + 1, day.get(Calendar.YEAR));

        for (StudyTask task : allTasks) {
            if (cellDateStr.equals(task.getDeadline())) {
                dayTasks.add(task);
            }
        }

        Collections.sort(dayTasks, new Comparator<StudyTask>() {
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

        // --- 3. ĐIỀU CHỈNH CHIỀU CAO Ô LỊCH THEO TRẠNG THÁI (TÍNH NĂNG MỚI) ---
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();

        if (isCompactMode) {
            // NẾU ĐANG BẬT DANH SÁCH: Co rút chiều cao ô lịch lại còn 50dp và ẩn các dòng text công việc
            layoutParams.height = dpToPx(holder.itemView.getContext(), 80);
        }
        else
            layoutParams.height = dpToPx(holder.itemView.getContext(), 95);

            if (dayTasks.size() > 2) {
                holder.tvTaskOverflow.setVisibility(View.VISIBLE);
                holder.tvTaskOverflow.setText("+" + (dayTasks.size() - 2) + " việc");
            } else {
                holder.tvTaskOverflow.setVisibility(View.GONE);
            }

            holder.tvMiniTask1.setVisibility(View.GONE);
            holder.tvMiniTask2.setVisibility(View.GONE);

            int colorHigh = holder.itemView.getContext().getResources().getColor(R.color.pastel_high);
            int colorMedium = holder.itemView.getContext().getResources().getColor(R.color.pastel_medium);
            int colorLow = holder.itemView.getContext().getResources().getColor(R.color.pastel_low);
            int colorDone = holder.itemView.getContext().getResources().getColor(R.color.pastel_done);

            if (dayTasks.size() > 0) setMiniTaskStyle(holder.tvMiniTask1, dayTasks.get(0), colorHigh, colorMedium, colorLow, colorDone);
            if (dayTasks.size() > 1) setMiniTaskStyle(holder.tvMiniTask2, dayTasks.get(1), colorHigh, colorMedium, colorLow, colorDone);

        holder.itemView.setLayoutParams(layoutParams); // Áp dụng kích thước mới

        holder.itemView.setOnClickListener(v -> listener.onItemClick(day));
    }

    // Hàm phụ trợ đổi chuẩn DP sang Pixel màn hình
    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void setMiniTaskStyle(TextView tv, StudyTask task, int cHigh, int cMed, int cLow, int cDone) {
        tv.setVisibility(View.VISIBLE);
        tv.setText(task.getTaskName());

        if (task.isCompleted()) {
            tv.setBackgroundColor(cDone);
            tv.setTextColor(Color.parseColor("#757575"));
        } else {
            tv.setTextColor(Color.parseColor("#1A1C1E"));
            if ("Cao".equals(task.getPriority())) tv.setBackgroundColor(cHigh);
            else if ("Thấp".equals(task.getPriority())) tv.setBackgroundColor(cLow);
            else tv.setBackgroundColor(cMed);
        }
    }

    @Override
    public int getItemCount() { return daysOfMonth.size(); }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvCellDay, tvMiniTask1, tvMiniTask2, tvTaskOverflow;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCellDay = itemView.findViewById(R.id.tv_cell_day);
            tvMiniTask1 = itemView.findViewById(R.id.tv_mini_task1);
            tvMiniTask2 = itemView.findViewById(R.id.tv_mini_task2);
            tvTaskOverflow = itemView.findViewById(R.id.tv_task_overflow);
        }
    }
}