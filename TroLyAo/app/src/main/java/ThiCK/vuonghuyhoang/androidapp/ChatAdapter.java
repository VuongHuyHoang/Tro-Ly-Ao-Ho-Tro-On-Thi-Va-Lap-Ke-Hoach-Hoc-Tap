package ThiCK.vuonghuyhoang.androidapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Import thư viện Markwon
import io.noties.markwon.Markwon;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messageList;
    private Context context;
    private Markwon markwon; // 1. Khai báo biến khởi tạo Markwon

    // 2. Thêm tham số Context vào Constructor để khởi tạo thư viện
    public ChatAdapter(List<ChatMessage> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
        this.markwon = Markwon.create(context); // Khởi tạo Markwon
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        // Nếu là người dùng gửi, hiện TextView bên phải, ẩn TextView bên trái
        if (message.isUser()) {
            holder.tvUser.setVisibility(View.VISIBLE);
            holder.tvAi.setVisibility(View.GONE);
            // Tin nhắn người dùng thì in ra text phẳng bình thường
            holder.tvUser.setText(message.getContent());
        } else {
            // Ngược lại, nếu là AI gửi
            holder.tvAi.setVisibility(View.VISIBLE);
            holder.tvUser.setVisibility(View.GONE);

            // 3. DÙNG MARKWON ĐỂ RENDER MARKDOWN CHO TIN NHẮN CỦA AI
            markwon.setMarkdown(holder.tvAi, message.getContent());
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvAi;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tv_user_message);
            tvAi = itemView.findViewById(R.id.tv_ai_message);
        }
    }
}