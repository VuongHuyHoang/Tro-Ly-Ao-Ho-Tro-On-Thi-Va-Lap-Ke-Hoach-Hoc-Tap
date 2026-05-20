package ThiCK.vuonghuyhoang.androidapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

import ThiCK.vuonghuyhoang.androidapp.network.AiCallback;
import ThiCK.vuonghuyhoang.androidapp.network.GeminiClient;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerChat;
    private EditText edtMessage;
    private FloatingActionButton btnSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private GeminiClient geminiClient;

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

        recyclerChat = view.findViewById(R.id.recycler_chat);
        edtMessage = view.findViewById(R.id.edt_message);
        btnSend = view.findViewById(R.id.btn_send);

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, requireContext());
        recyclerChat.setAdapter(chatAdapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerChat.setLayoutManager(layoutManager);
        recyclerChat.setAdapter(chatAdapter);

        geminiClient = new GeminiClient();

        addMessageToChat("Xin chào! Tôi là Trợ lý học tập AI. Bạn muốn hỏi kiến thức chuyên môn hay cần phân rã đồ án hôm nay?", false);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = edtMessage.getText().toString().trim();
                if (question.isEmpty()) return;

                addMessageToChat(question, true);
                edtMessage.setText("");
                btnSend.setEnabled(false);

                ChatMessage thinkingMessage = new ChatMessage("⏳ AI đang suy nghĩ", false);
                messageList.add(thinkingMessage);
                int thinkingIndex = messageList.size() - 1;
                chatAdapter.notifyItemInserted(thinkingIndex);
                recyclerChat.scrollToPosition(thinkingIndex);

                dotCount = 0;
                typingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        dotCount++;
                        if (dotCount > 3) dotCount = 1;
                        StringBuilder dots = new StringBuilder();
                        for (int i = 0; i < dotCount; i++) dots.append(".");
                        messageList.set(thinkingIndex, new ChatMessage("⏳ AI đang suy nghĩ" + dots, false));
                        chatAdapter.notifyItemChanged(thinkingIndex);
                        typingHandler.postDelayed(this, 400);
                    }
                };
                typingHandler.post(typingRunnable);

                String systemInstruction = "\n\n[HƯỚNG DẪN HỆ THỐNG]: Nếu người dùng yêu cầu phân rã đồ án, lên lịch trình, hoặc tạo danh sách công việc, bạn PHẢI ĐÍNH KÈM một chuỗi JSON ở CUỐI bài viết of bạn theo đúng cấu trúc chính xác sau: "
                        + "---TASK_START--- {\"tasks\": [{\"taskName\": \"Tên công việc cụ thể bằng tiếng Việt\", \"deadline\": \"dd/MM/yyyy - HH:mm\", \"priority\": \"Cao/Trung bình/Thấp\"}]} ---TASK_END---. "
                        + "Hãy tính toán deadline hợp lý bắt đầu từ mốc thời gian thực tế hiện tại là tháng 05 năm 2026. Phần nội dung giải thích bằng văn bản ở trên hãy viết thật thân thiện và chi tiết để hiển thị cho người dùng.";

                String finalPrompt = question + systemInstruction;

                geminiClient.sendPrompt(finalPrompt, new AiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        if (getActivity() == null || !isAdded()) return;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                typingHandler.removeCallbacks(typingRunnable);
                                btnSend.setEnabled(true);

                                String cleanDisplayResponse = response;

                                try {
                                    if (response.contains("---TASK_START---") && response.contains("---TASK_END---")) {
                                        int startIndex = response.indexOf("---TASK_START---") + "---TASK_START---".length();
                                        int endIndex = response.indexOf("---TASK_END---");

                                        String jsonString = response.substring(startIndex, endIndex).trim();
                                        jsonString = jsonString.replace("```json", "").replace("```", "").trim();

                                        JSONObject jsonObject = new JSONObject(jsonString);
                                        JSONArray jsonArray = jsonObject.getJSONArray("tasks");

                                        // 1. Kiểm tra tài khoản hiện tại để lấy UID trước khi ghi đè dữ liệu lên Firestore
                                        FirebaseAuth auth = FirebaseAuth.getInstance();
                                        if (auth.getCurrentUser() != null) {
                                            String currentUid = auth.getCurrentUser().getUid();
                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                            int tasksCreatedCount = 0;

                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                JSONObject item = jsonArray.getJSONObject(i);
                                                String name = item.getString("taskName");
                                                String deadline = item.getString("deadline");
                                                String priority = item.getString("priority");

                                                int newId = (int) (System.currentTimeMillis() / 1000) + i;
                                                StudyTask newTask = new StudyTask(newId, name, deadline, priority, false);

                                                // PHÂN TÁCH CẤU TRÚC: users -> {uid} -> user_tasks -> {task_id}
                                                db.collection("users")
                                                        .document(currentUid)
                                                        .collection("user_tasks")
                                                        .document(String.valueOf(newId))
                                                        .set(newTask);

                                                tasksCreatedCount++;
                                            }

                                            cleanDisplayResponse = response.substring(0, response.indexOf("---TASK_START---")).trim();
                                            cleanDisplayResponse += "\n\n🤖 *[Hệ thống]: Đã tự động phân tích và lưu thành công " + tasksCreatedCount + " nhiệm vụ mới vào không gian lưu trữ cá nhân của bạn trên Cloud Firestore!*";
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                messageList.set(thinkingIndex, new ChatMessage(cleanDisplayResponse, false));
                                chatAdapter.notifyItemChanged(thinkingIndex);
                                recyclerChat.scrollToPosition(thinkingIndex);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (getActivity() == null || !isAdded()) return;

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                typingHandler.removeCallbacks(typingRunnable);
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

    private void addMessageToChat(String message, boolean isUser) {
        messageList.add(new ChatMessage(message, isUser));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        recyclerChat.scrollToPosition(messageList.size() - 1);
    }
}