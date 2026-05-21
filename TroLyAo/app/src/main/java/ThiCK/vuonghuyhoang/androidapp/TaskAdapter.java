package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<TaskWrapper> visibleList;
    private OnHeaderClickListener headerClickListener;

    // Interface để báo cho Fragment biết khi người dùng bấm mở/đóng danh mục
    public interface OnHeaderClickListener {
        void onHeaderClick(String categoryName, boolean isCurrentlyExpanded);
        void onHeaderLongClick(String categoryName);
    }

    public TaskAdapter(List<TaskWrapper> visibleList, OnHeaderClickListener headerClickListener) {
        this.visibleList = visibleList;
        this.headerClickListener = headerClickListener;
    }

    // 1. QUYẾT ĐỊNH LOẠI VIEW CHO TỪNG DÒNG
    @Override
    public int getItemViewType(int position) {
        return visibleList.get(position).type;
    }

    // 2. TẠO GIAO DIỆN TƯƠNG ỨNG VỚI VIEW TYPE
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TaskWrapper.TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }
    }

    // 3. ĐỔ DỮ LIỆU VÀO GIAO DIỆN
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TaskWrapper item = visibleList.get(position);

        if (holder.getItemViewType() == TaskWrapper.TYPE_HEADER) {
            // XỬ LÝ THANH TIÊU ĐỀ
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.tvCategoryName.setText(item.headerTitle);

            // Xoay mũi tên lên/xuống tùy trạng thái mở rộng
            headerHolder.imgArrow.setRotation(item.isExpanded ? 180f : 0f);

            // Bắt sự kiện bấm vào thanh tiêu đề
            headerHolder.itemView.setOnClickListener(v -> {
                if (headerClickListener != null) {
                    headerClickListener.onHeaderClick(item.headerTitle, item.isExpanded);
                }
            });

            headerHolder.itemView.setOnLongClickListener(v -> {
                if (headerClickListener != null) {
                    headerClickListener.onHeaderLongClick(item.headerTitle);
                }
                return true; // Trả về true để hệ thống biết sự kiện đã được xử lý, không kích hoạt click thường
            });

        } else {
            // XỬ LÝ THẺ CÔNG VIỆC (Đoạn code cũ của bạn đưa vào đây)
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            StudyTask task = item.task;

            taskHolder.tvTaskName.setText(task.getTaskName());
            taskHolder.checkBox.setOnCheckedChangeListener(null);
            taskHolder.checkBox.setChecked(task.isCompleted());

            // Logic thời gian và cảnh báo động
            String timeStr = (task.getDueTime() != null && !task.getDueTime().isEmpty()) ? task.getDueTime() : "23:59";
            String baseDeadlineText = timeStr + " - " + task.getDeadline();

            if (task.isCompleted()) {
                taskHolder.tvDeadline.setText(baseDeadlineText);
                taskHolder.tvDeadline.setTextColor(Color.parseColor("#757575"));
            } else {
                try {
                    java.util.Calendar currentCal = java.util.Calendar.getInstance();
                    java.util.Calendar taskCal = java.util.Calendar.getInstance();
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                    taskCal.setTime(sdf.parse(task.getDeadline() + " " + timeStr));

                    long diffInMillis = taskCal.getTimeInMillis() - currentCal.getTimeInMillis();
                    long estimatedInMillis = task.getEstimatedMinutes() * 60 * 1000L;

                    if (diffInMillis < 0) {
                        taskHolder.tvDeadline.setTextColor(Color.parseColor("#C62828"));
                        taskHolder.tvDeadline.setText("⚠️ ĐÃ QUÁ HẠN: " + baseDeadlineText);
                    } else if (diffInMillis < estimatedInMillis) {
                        taskHolder.tvDeadline.setTextColor(Color.parseColor("#F4511E"));
                        taskHolder.tvDeadline.setText("⏳ SẮP HẾT GIỜ: " + baseDeadlineText);
                    } else {
                        taskHolder.tvDeadline.setTextColor(Color.parseColor("#757575"));
                        taskHolder.tvDeadline.setText(baseDeadlineText);
                    }
                } catch (Exception e) {
                    taskHolder.tvDeadline.setTextColor(Color.parseColor("#757575"));
                    taskHolder.tvDeadline.setText(baseDeadlineText);
                }
            }

            // Logic nhuộm màu Pastel
            int colorHigh = taskHolder.itemView.getContext().getResources().getColor(R.color.pastel_high);
            int colorMedium = taskHolder.itemView.getContext().getResources().getColor(R.color.pastel_medium);
            int colorLow = taskHolder.itemView.getContext().getResources().getColor(R.color.pastel_low);
            int colorDone = taskHolder.itemView.getContext().getResources().getColor(R.color.pastel_done);

            if (task.isCompleted()) {
                taskHolder.cardTaskBg.setCardBackgroundColor(colorDone);
                taskHolder.chipPriority.setVisibility(View.GONE);
                taskHolder.tvTaskName.setPaintFlags(taskHolder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskHolder.tvTaskName.setTextColor(Color.parseColor("#757575"));
            } else {
                taskHolder.tvTaskName.setPaintFlags(taskHolder.tvTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskHolder.tvTaskName.setTextColor(taskHolder.itemView.getContext().getResources().getColor(R.color.textColorPrimary));
                taskHolder.chipPriority.setVisibility(View.GONE);

                String prio = task.getPriority() != null ? task.getPriority() : "Trung bình";
                switch (prio) {
                    case "Cao": taskHolder.cardTaskBg.setCardBackgroundColor(colorHigh); break;
                    case "Thấp": taskHolder.cardTaskBg.setCardBackgroundColor(colorLow); break;
                    default: taskHolder.cardTaskBg.setCardBackgroundColor(colorMedium); break;
                }
            }

            // Xử lý sự kiện CheckBox lưu Firebase
            taskHolder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    FirebaseFirestore.getInstance()
                            .collection("users").document(auth.getCurrentUser().getUid())
                            .collection("user_tasks").document(String.valueOf(task.getId()))
                            .update("completed", isChecked);
                    // Dùng notifyDataSetChanged thay cho notifyItemChanged để an toàn hơn với Accordion
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return visibleList != null ? visibleList.size() : 0;
    }

    // 4. KHAI BÁO 2 LOẠI VIEWHOLDER
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName;
        ImageView imgArrow;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            imgArrow = itemView.findViewById(R.id.img_expand_arrow); // Đảm bảo ID này khớp với file item_category_header.xml
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTaskBg;
        TextView tvTaskName;
        TextView tvDeadline;
        Chip chipPriority;
        CheckBox checkBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTaskBg = itemView.findViewById(R.id.card_task_bg);
            tvTaskName = itemView.findViewById(R.id.tv_task_name);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            chipPriority = itemView.findViewById(R.id.chip_priority);
            checkBox = itemView.findViewById(R.id.checkbox_task);
        }
    }
}