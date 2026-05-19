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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
        holder.chipPriority.setText(task.getPriority());

        // 1. TẮT bộ lắng nghe cũ trước khi set trạng thái để tránh lỗi lặp CheckBox khi cuộn RecyclerView
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());

        // Xử lý màu sắc của Nhãn độ ưu tiên (Priority)
        switch (task.getPriority()) {
            case "Cao":
                holder.chipPriority.setTextColor(Color.parseColor("#D32F2F")); // Đỏ
                break;
            case "Trung bình":
                holder.chipPriority.setTextColor(Color.parseColor("#F57F17")); // Cam
                break;
            default:
                holder.chipPriority.setTextColor(Color.parseColor("#388E3C")); // Xanh lá
                break;
        }

        // 2. BẬT bộ lắng nghe sự kiện thay đổi CheckBox và đồng bộ Realtime lên Cloud Firestore
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked); // Cập nhật trạng thái tức thì trên RAM cho đối tượng

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();

                // Firestore nhận diện documentID bằng chuỗi String, vì vậy dùng String.valueOf(task.getId()) để ép kiểu
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUid)
                        .collection("user_tasks")
                        .document(String.valueOf(task.getId()))
                        .update("completed", isChecked) // Từ khóa "completed" phải khớp với thuộc tính boolean trong Firestore
                        .addOnFailureListener(e -> {
                            // Nếu lỗi mạng, hoàn tác (rollback) lại giao diện để không bị lệch thông tin với DB
                            holder.checkBox.setChecked(!isChecked);
                            task.setCompleted(!isChecked);
                        });
            }
        });
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
            checkBox = itemView.findViewById(R.id.checkbox_task); // Đã đồng bộ đúng theo ID xml của bạn
        }
    }
}