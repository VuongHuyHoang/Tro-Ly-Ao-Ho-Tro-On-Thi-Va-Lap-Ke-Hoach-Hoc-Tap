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

    public interface OnQuizItemClickListener {
        void onQuizClick(SavedQuiz savedQuiz);
    }

    public QuizHistoryAdapter(List<SavedQuiz> historyList, OnQuizItemClickListener clickListener) {
        this.historyList = historyList;
        this.clickListener = clickListener;
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

        holder.tvTitle.setText(quiz.getTitle());
        holder.tvScore.setText("Điểm: " + quiz.getScore() + "/" + quiz.getTotalQuestions());

        // Định dạng thời gian hiển thị thân thiện
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.tvTime.setText("Ngày tạo: " + sdf.format(new Date(quiz.getTimestamp())));

        // Bắt sự kiện khi click vào bộ đề cũ
        holder.itemView.setOnClickListener(v -> clickListener.onQuizClick(quiz));
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