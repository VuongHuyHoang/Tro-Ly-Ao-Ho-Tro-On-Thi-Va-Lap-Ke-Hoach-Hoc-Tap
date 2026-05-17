package ThiCK.vuonghuyhoang.androidapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

import ThiCK.vuonghuyhoang.androidapp.network.AiCallback;
import ThiCK.vuonghuyhoang.androidapp.network.GeminiClient;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChat;
    private EditText edtMessage;
    private FloatingActionButton btnSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private GeminiClient geminiClient;

    // 3 BIẾN NÀY ĐỂ LÀM HIỆU ỨNG
    private android.os.Handler typingHandler = new android.os.Handler();
    private Runnable typingRunnable;
    private int dotCount = 0;

    public ChatFragment() {
        // Constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ các view
        recyclerChat = view.findViewById(R.id.recycler_chat);
        edtMessage = view.findViewById(R.id.edt_message);
        btnSend = view.findViewById(R.id.btn_send);

        // 2. Cài đặt danh sách Chat
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Luôn đẩy danh sách từ dưới lên (giống Zalo/Messenger)
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);

        // 3. Khởi tạo cầu nối với AI
        geminiClient = new GeminiClient();

        // 4. Lời chào tự động khi vừa vào trang
        addMessageToChat("Xin chào! Tôi là Trợ lý học tập AI. Bạn muốn hỏi kiến thức chuyên môn hay cần phân rã đồ án hôm nay?", false);

        // 5. Bắt sự kiện nút Gửi
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = edtMessage.getText().toString().trim();
                if (question.isEmpty()) return; // Không cho gửi tin nhắn trống

                // 1. In câu hỏi của bạn lên màn hình
                addMessageToChat(question, true);
                edtMessage.setText(""); // Xóa trắng ô nhập liệu

                // Khóa nút Gửi để tránh người dùng bấm liên tục gây kẹt mạng
                btnSend.setEnabled(false);

                // Tạo tin nhắn ảo ban đầu
                ChatMessage thinkingMessage = new ChatMessage("⏳ AI đang suy nghĩ", false);
                messageList.add(thinkingMessage);
                int thinkingIndex = messageList.size() - 1;
                chatAdapter.notifyItemInserted(thinkingIndex);
                recyclerChat.scrollToPosition(thinkingIndex);

                // ==========================================
                // BẮT ĐẦU HIỆU ỨNG "SÓNG" DẤU CHẤM LỬNG
                // ==========================================
                dotCount = 0;
                typingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        dotCount++;
                        if (dotCount > 3) dotCount = 1; // Chỉ cho chạy từ 1 đến 3 dấu chấm

                        String dots = "";
                        for (int i = 0; i < dotCount; i++) {
                            dots += ".";
                        }

                        // Cập nhật lại chữ trên màn hình
                        messageList.set(thinkingIndex, new ChatMessage("⏳ AI đang suy nghĩ" + dots, false));
                        chatAdapter.notifyItemChanged(thinkingIndex);

                        // Lặp lại hiệu ứng này sau mỗi 400 mili-giây (0.4s)
                        typingHandler.postDelayed(this, 400);
                    }
                };
                // Kích hoạt vòng lặp chạy ngay lập tức
                typingHandler.post(typingRunnable);
                // ==========================================

                // Gửi câu hỏi lên Gemini AI
                geminiClient.sendPrompt(question, new AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                typingHandler.removeCallbacks(typingRunnable); // TẮT HIỆU ỨNG KHI CÓ KẾT QUẢ
                                btnSend.setEnabled(true);

                                messageList.set(thinkingIndex, new ChatMessage(response, false));
                                chatAdapter.notifyItemChanged(thinkingIndex);
                                recyclerChat.scrollToPosition(thinkingIndex);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                typingHandler.removeCallbacks(typingRunnable); // TẮT HIỆU ỨNG KHI BỊ LỖI MẠNG
                                btnSend.setEnabled(true);

                                messageList.set(thinkingIndex, new ChatMessage("❌ Lỗi kết nối: " + error, false));
                                chatAdapter.notifyItemChanged(thinkingIndex);
                            }
                        });
                    }
                });
            }
        });
    }

    // Hàm phụ trợ để thêm tin nhắn vào danh sách và tự động cuộn xuống cuối
    private void addMessageToChat(String message, boolean isUser) {
        messageList.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }
}

