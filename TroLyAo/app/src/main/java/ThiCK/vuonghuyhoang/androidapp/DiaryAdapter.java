package ThiCK.vuonghuyhoang.androidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    private List<DiaryEntry> diaryList;
    private OnDiaryItemClickListener deleteListener;

    public interface OnDiaryItemClickListener {
        void onDeleteClick(DiaryEntry diary);
        void onItemClick(DiaryEntry diary);
    }

    public DiaryAdapter(List<DiaryEntry> diaryList, OnDiaryItemClickListener deleteListener) {
        this.diaryList = diaryList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_diary, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder holder, int position) {
        DiaryEntry diary = diaryList.get(position);

        holder.tvContent.setText(diary.getContent());

        // Chuyển đổi timestamp (số) thành dạng "HH:mm - dd/MM/yyyy"
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        String formattedTime = sdf.format(new Date(diary.getTimestamp()));
        holder.tvTime.setText(formattedTime);

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        String currentMonthStr = monthYearFormat.format(new Date(diary.getTimestamp()));

        boolean shouldShowHeader = false;

        if (position == 0) {
            // Nếu là bản nhật ký đầu tiên trong danh sách -> Luôn hiển thị tiêu đề tháng
            shouldShowHeader = true;
        } else {
            // Lấy bản nhật ký đứng ngay phía trước nó để đối chiếu
            DiaryEntry previousDiary = diaryList.get(position - 1);
            String previousMonthStr = monthYearFormat.format(new Date(previousDiary.getTimestamp()));

            // Nếu tháng của bản ghi hiện tại KHÁC với tháng của bản ghi trước đó -> Hiện tiêu đề tháng mới
            if (!currentMonthStr.equals(previousMonthStr)) {
                shouldShowHeader = true;
            }
        }

        // Thực thi ẩn hiện dựa trên kết quả thuật toán
        if (shouldShowHeader) {
            holder.tvMonthHeader.setVisibility(View.VISIBLE);
            holder.tvMonthHeader.setText("Tháng " + currentMonthStr);
        } else {
            holder.tvMonthHeader.setVisibility(View.GONE);
        }
        // --- KẾT THÚC THUẬT TOÁN TIÊU ĐỀ THÁNG ---

        // Sự kiện bấm nút xóa thùng rác
        holder.btnDelete.setOnClickListener(v -> deleteListener.onDeleteClick(diary));

        holder.itemView.setOnClickListener(v -> deleteListener.onItemClick(diary));
    }

    @Override
    public int getItemCount() {
        return diaryList.size();
    }

    public static class DiaryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvContent;
        ImageView btnDelete;
        TextView tvMonthHeader;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_diary_time);
            tvContent = itemView.findViewById(R.id.tv_diary_content);
            btnDelete = itemView.findViewById(R.id.btn_delete_diary);
            tvMonthHeader = itemView.findViewById(R.id.tv_month_header);
        }
    }
}