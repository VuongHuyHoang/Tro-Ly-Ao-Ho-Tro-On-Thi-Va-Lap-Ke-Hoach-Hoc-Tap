package ThiCK.vuonghuyhoang.androidapp;

import java.util.Collections;
import java.util.Comparator;
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

    private final List<Calendar> daysOfMonth;
    private final List<StudyTask> allTasks;
    private final int currentMonth;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Calendar date);
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

        // Lấy ngày hệ thống hiện tại
        Calendar today = Calendar.getInstance();
        boolean isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                day.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                day.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH);

        if (isToday) {
            // Nếu là hôm nay: Đổ nền tròn xanh, chữ trắng
            holder.tvCellDay.setBackgroundResource(R.drawable.bg_today_circle);
            holder.tvCellDay.setTextColor(Color.WHITE);
        } else {
            // Nếu là ngày bình thường: Xóa nền tròn
            holder.tvCellDay.setBackground(null);

            // Làm mờ những ngày không thuộc tháng hiện tại
            if (day.get(Calendar.MONTH) != currentMonth) {
                holder.tvCellDay.setTextColor(Color.parseColor("#C4C7C9"));
            } else {
                holder.tvCellDay.setTextColor(Color.parseColor("#1A1C1E"));
            }
        }

        if (day == null) {
            holder.tvCellDay.setText("");
            holder.itemView.setOnClickListener(null);
            return;
        }

        holder.tvCellDay.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

        // Làm mờ những ngày không thuộc tháng hiện tại
        if (day.get(Calendar.MONTH) != currentMonth) {
            holder.tvCellDay.setTextColor(Color.parseColor("#C4C7C9"));
        } else {
            holder.tvCellDay.setTextColor(Color.parseColor("#1A1C1E"));
        }

        // 1. Quét tìm tất cả các công việc có trong ngày này
        List<StudyTask> dayTasks = new ArrayList<>();
        String cellDateStr = String.format("%02d/%02d/%d", day.get(Calendar.DAY_OF_MONTH), day.get(Calendar.MONTH) + 1, day.get(Calendar.YEAR));

        for (StudyTask task : allTasks) {
            if (cellDateStr.equals(task.getDeadline())) {
                dayTasks.add(task);
            }
        }

        // 2. THUẬT TOÁN SẮP XẾP ƯU TIÊN KÉP (ĐÃ BỔ SUNG ĐỂ GIẢI QUYẾT LỖI ẨN CÔNG VIỆC)
        Collections.sort(dayTasks, new Comparator<StudyTask>() {
            @Override
            public int compare(StudyTask t1, StudyTask t2) {
                // Tiêu chí 1: Việc chưa xong (false) phải xếp TRƯỚC việc đã xong (true)
                int compComplete = Boolean.compare(t1.isCompleted(), t2.isCompleted());
                if (compComplete != 0) return compComplete;

                // Tiêu chí 2: Nếu cùng trạng thái chưa xong, việc "Cao" phải xếp TRƯỚC việc khác
                return getPriorityWeight(t2.getPriority()) - getPriorityWeight(t1.getPriority());
            }

            // Hàm phụ tính trọng số độ khẩn cấp để đảo vị trí
            private int getPriorityWeight(String priority) {
                if (priority == null) return 1;
                switch (priority) {
                    case "Cao": return 3;
                    case "Trung bình": return 2;
                    default: return 1; // "Thấp"
                }
            }
        });

        // 3. Tiến hành hiển thị tối đa 2 mini task đã được sàng lọc chất lượng lên ô lịch
        holder.tvMiniTask1.setVisibility(View.GONE);
        holder.tvMiniTask2.setVisibility(View.GONE);

        int colorHigh = holder.itemView.getContext().getResources().getColor(R.color.pastel_high);
        int colorMedium = holder.itemView.getContext().getResources().getColor(R.color.pastel_medium);
        int colorLow = holder.itemView.getContext().getResources().getColor(R.color.pastel_low);
        int colorDone = holder.itemView.getContext().getResources().getColor(R.color.pastel_done);

        if (dayTasks.size() > 0) {
            setMiniTaskStyle(holder.tvMiniTask1, dayTasks.get(0), colorHigh, colorMedium, colorLow, colorDone);
        }
        if (dayTasks.size() > 1) {
            setMiniTaskStyle(holder.tvMiniTask2, dayTasks.get(1), colorHigh, colorMedium, colorLow, colorDone);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(day));
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
        TextView tvCellDay, tvMiniTask1, tvMiniTask2;
        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCellDay = itemView.findViewById(R.id.tv_cell_day);
            tvMiniTask1 = itemView.findViewById(R.id.tv_mini_task1);
            tvMiniTask2 = itemView.findViewById(R.id.tv_mini_task2);
        }
    }
}