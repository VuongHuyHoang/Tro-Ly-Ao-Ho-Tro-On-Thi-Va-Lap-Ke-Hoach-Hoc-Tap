package ThiCK.vuonghuyhoang.androidapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
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

        // TẮT bộ lắng nghe cũ trước khi gán trạng thái để tránh lỗi lặp CheckBox khi cuộn
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());

        // --- CẬP NHẬT GÁN TEXT HẠN CHÓT ---
        String timeStr = (task.getDueTime() != null && !task.getDueTime().isEmpty()) ? task.getDueTime() : "23:59";
        holder.tvDeadline.setText(timeStr + " - " + task.getDeadline());

        // THỰC HIỆN: NHUỘM MÀU "DEADLINE DÍ" ---
        try {
            // Lấy thời gian hiện tại của hệ thống
            java.util.Calendar currentCal = java.util.Calendar.getInstance();

            // Tạo đối tượng Calendar đại diện cho thời gian Deadline của Task
            java.util.Calendar taskCal = java.util.Calendar.getInstance();

            // Định dạng để phân tích chuỗi Ngày và Giờ
            // Lưu ý: Định dạng phải khớp chính xác với cách bạn lưu (dd/MM/yyyy HH:mm)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());

            // Ghép Ngày và Giờ lại để parse
            String combinedDateTimeStr = task.getDeadline() + " " + timeStr;
            taskCal.setTime(sdf.parse(combinedDateTimeStr));

            // Tính toán khoảng cách thời gian (tính bằng mili-giây)
            long diffInMillis = taskCal.getTimeInMillis() - currentCal.getTimeInMillis();

            // Định nghĩa ngưỡng 2 tiếng (2 * 60 phút * 60 giây * 1000 mili-giây)
            long twoHoursInMillis = 2 * 60 * 60 * 1000;

            // KIỂM TRA ĐIỀU KIỆN
            if (diffInMillis < 0) {
                // Trường hợp 1: Đã quá hạn rồi! -> Nhuộm màu Đỏ đậm sắc nét
                holder.tvDeadline.setTextColor(android.graphics.Color.parseColor("#C62828"));
                holder.tvDeadline.setText("⚠️ ĐÃ QUÁ HẠN: " + timeStr + " - " + task.getDeadline());

            } else if (diffInMillis <= twoHoursInMillis) {
                // Trường hợp 2: Deadline dí! (Trong vòng 2 tiếng tới) -> Nhuộm màu Cam đỏ khẩn cấp
                holder.tvDeadline.setTextColor(android.graphics.Color.parseColor("#F4511E"));
                // Bạn có thể thêm icon đồng hồ cát nhỏ nếu muốn

            } else {
                // Trường hợp 3: Còn dư dả thời gian -> Trả về màu xám nhạt cơ bản
                holder.tvDeadline.setTextColor(android.graphics.Color.parseColor("#757575"));
            }

        } catch (Exception e) {
            // Nếu parse bị lỗi (do dữ liệu ngày tháng sai định dạng), giữ nguyên màu xám
            holder.tvDeadline.setTextColor(android.graphics.Color.parseColor("#757575"));
            e.printStackTrace();
        }

        // Đọc mã màu Pastel trực tiếp từ resources để đảm bảo hiển thị đồng bộ
        int colorHigh = holder.itemView.getContext().getResources().getColor(R.color.pastel_high);
        int colorMedium = holder.itemView.getContext().getResources().getColor(R.color.pastel_medium);
        int colorLow = holder.itemView.getContext().getResources().getColor(R.color.pastel_low);
        int colorDone = holder.itemView.getContext().getResources().getColor(R.color.pastel_done);

        // THUẬT TOÁN NHUỘM MÀN HÌNH THEO MÀU PASTEL (GIỐNG ẢNH MẪU)
        if (task.isCompleted()) {
            // Khi đã hoàn thành: Chuyển Card sang màu Xám nhạt, ẩn chip, gạch ngang chữ
            holder.cardTaskBg.setCardBackgroundColor(colorDone);
            holder.chipPriority.setVisibility(View.GONE);
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTaskName.setTextColor(Color.parseColor("#757575")); // Chữ xám mờ đi
        } else {
            // Khi chưa hoàn thành: Bỏ gạch ngang chữ, khôi phục màu đen đậm
            holder.tvTaskName.setPaintFlags(holder.tvTaskName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTaskName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.textColorPrimary));

            // Ẩn chip ưu tiên vì bản thân chiếc Card đã mang màu sắc đại diện cho độ ưu tiên đó
            holder.chipPriority.setVisibility(View.GONE);

            if (task.getPriority() != null) {
                switch (task.getPriority()) {
                    case "Cao":
                        holder.cardTaskBg.setCardBackgroundColor(colorHigh);
                        break;
                    case "Thấp":
                        holder.cardTaskBg.setCardBackgroundColor(colorLow);
                        break;
                    default: // Trung bình
                        holder.cardTaskBg.setCardBackgroundColor(colorMedium);
                        break;
                }
            } else {
                holder.cardTaskBg.setCardBackgroundColor(colorMedium);
            }
        }

        // ĐOẠN CODE ĐÃ ĐƯỢC SỬA SẠCH LỖI CÚ PHÁP ĐÓNG NGOẶC TRONG TASKADAPTER.JAVA
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked); // Cập nhật RAM tức thì để đổi màu Pastel

            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                String currentUid = auth.getCurrentUser().getUid();
                String documentId = String.valueOf(task.getId());

                // LỚP 1: Thử cập nhật trực tiếp bằng ID dạng số (int)
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUid)
                        .collection("user_tasks")
                        .document(documentId)
                        .update("completed", isChecked)
                        .addOnSuccessListener(aVoid -> {
                            // Cập nhật thành công lớp 1 -> Làm mới màu dòng này
                            notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> {
                            // LỚP 2 (PHÒNG THỦ): Nếu lớp 1 lỗi do lệch ID, tìm Document ID thực tế bằng trường taskName
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(currentUid)
                                    .collection("user_tasks")
                                    .whereEqualTo("taskName", task.getTaskName())
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            // Lấy được mã chuỗi ID thực tế trên Cloud Firestore
                                            String realDocId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                            // Tiến hành cập nhật chuẩn xác vào đúng Document
                                            FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(currentUid)
                                                    .collection("user_tasks")
                                                    .document(realDocId)
                                                    .update("completed", isChecked)
                                                    .addOnSuccessListener(aVoid2 -> {
                                                        notifyItemChanged(position);
                                                    });
                                        }
                                    })
                                    .addOnFailureListener(err -> {
                                        // Hoàn tác (Rollback) giao diện nếu lỗi mạng thực sự xảy ra
                                        task.setCompleted(!isChecked);
                                        holder.checkBox.setOnCheckedChangeListener(null);
                                        holder.checkBox.setChecked(!isChecked);
                                        notifyItemChanged(position);
                                    });
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTaskBg; // Khai báo CardView bao ngoài để đổi màu nền
        TextView tvTaskName;
        TextView tvDeadline;
        Chip chipPriority;
        CheckBox checkBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTaskBg = itemView.findViewById(R.id.card_task_bg); // Ánh xạ id đã thêm ở item_task.xml
            tvTaskName = itemView.findViewById(R.id.tv_task_name);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            chipPriority = itemView.findViewById(R.id.chip_priority);
            checkBox = itemView.findViewById(R.id.checkbox_task);
        }
    }
}