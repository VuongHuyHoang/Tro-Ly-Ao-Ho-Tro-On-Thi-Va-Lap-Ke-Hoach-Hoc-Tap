package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<StudyTask> taskList;

    public TaskAdapter(List<StudyTask> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        StudyTask task = taskList.get(position);

        holder.tvTaskName.setText(task.getTaskName());
        holder.tvDeadline.setText("Hạn chót: " + task.getDeadline());
        holder.checkBox.setChecked(task.isCompleted());
        holder.chipPriority.setText(task.getPriority());

        // Xử lý màu sắc của Nhãn độ ưu tiên (Priority)
        switch (task.getPriority()) {
            case "Cao":
                holder.chipPriority.setTextColor(Color.parseColor("#D32F2F")); // Đỏ
                // holder.chipPriority.setChipBackgroundColorResource(R.color.primaryLightColor);
                break;
            case "Trung bình":
                holder.chipPriority.setTextColor(Color.parseColor("#F57F17")); // Cam
                break;
            default:
                holder.chipPriority.setTextColor(Color.parseColor("#388E3C")); // Xanh lá
                break;
        }
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName;
        TextView tvDeadline;
        Chip chipPriority;
        CheckBox checkBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tv_task_name);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            chipPriority = itemView.findViewById(R.id.chip_priority);
            checkBox = itemView.findViewById(R.id.checkbox_task);
        }
    }
}
