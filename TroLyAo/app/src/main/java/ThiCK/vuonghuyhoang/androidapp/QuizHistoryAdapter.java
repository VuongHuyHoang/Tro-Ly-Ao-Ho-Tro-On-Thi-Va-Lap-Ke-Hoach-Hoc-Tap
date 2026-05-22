package ThiCK.vuonghuyhoang.androidapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuizHistoryAdapter extends RecyclerView.Adapter<QuizHistoryAdapter.HistoryViewHolder> {

    private List<SavedQuiz> historyList;
    private OnQuizItemClickListener clickListener;
    private OnQuizItemLongClickListener longClickListener; // Khai báo thêm

    public interface OnQuizItemClickListener {
        void onQuizClick(SavedQuiz savedQuiz);
    }

    // Giao diện cho sự kiện nhấn giữ
    public interface OnQuizItemLongClickListener {
        void onQuizLongClick(SavedQuiz savedQuiz);
    }

    // Cập nhật Constructor
    public QuizHistoryAdapter(List<SavedQuiz> historyList, OnQuizItemClickListener clickListener, OnQuizItemLongClickListener longClickListener) {
        this.historyList = historyList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_saved_quiz, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SavedQuiz quiz = historyList.get(position);

        holder.tvTitle.setText(quiz.getTitle()); // Đã có sẵn ngữ cảnh tên đề thi
        holder.tvScore.setText("Điểm: " + quiz.getScore() + "/" + quiz.getTotalQuestions());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvTime.setText("Ngày tạo: " + sdf.format(new Date(quiz.getTimestamp())));

        // Bắt sự kiện Click ngắn (Làm bài)
        holder.itemView.setOnClickListener(v -> clickListener.onQuizClick(quiz));

        // Bắt sự kiện Nhấn giữ (Xóa bài)
        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onQuizLongClick(quiz);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvScore;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_history_title);
            tvTime = itemView.findViewById(R.id.tv_history_time);
            tvScore = itemView.findViewById(R.id.tv_history_score);
        }
    }
}