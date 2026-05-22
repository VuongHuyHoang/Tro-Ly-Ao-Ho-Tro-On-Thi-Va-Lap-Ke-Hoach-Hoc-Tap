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

                java.text.SimpleDateFormat promptSdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                String currentTime = promptSdf.format(new java.util.Date());

                String systemInstruction = "Bạn là một trợ lý ảo thông minh giúp sinh viên lập kế hoạch học tập. " +
                        "Dựa vào yêu cầu của người dùng, hãy phân tích và tạo ra danh sách các công việc cần làm. " +
                        "TRẢ VỀ KẾT QUẢ DƯỚI DẠNG CHUỖI JSON ARRAY CHUẨN XÁC, KHÔNG CÓ MARKDOWN HAY CHỮ NÀO KHÁC BÊN NGOÀI. " +
                        "Mỗi công việc trong JSON phải có đầy đủ 7 trường sau:\n" +
                        "- \"taskName\": Tên công việc cụ thể (ngắn gọn).\n" +
                        "- \"description\": Mô tả chi tiết (Các yêu cầu phụ, ghi chú, người làm chung... Nếu không có thì để rỗng \"\").\n" + // <-- BỔ SUNG TRƯỜNG NÀY
                        "- \"category\": Tên Dự án hoặc Danh mục của công việc này (Ví dụ: 'Đồ án Java', 'Học tiếng Anh'). Hãy tự suy luận tên danh mục ngắn gọn.\n" +
                        "- \"deadline\": Ngày hạn chót định dạng dd/MM/yyyy.\n" +
                        "- \"dueTime\": Giờ hạn chót định dạng HH:mm (VD: 14:30). Nếu không rõ, để '23:59'.\n" +
                        "- \"estimatedMinutes\": Số phút dự kiến hoàn thành (số nguyên).\n" +
                        "- \"priority\": Chỉ chọn 1 trong 3 mức: 'Cao', 'Trung bình', 'Thấp'.\n" +
                        "\n" +
                        "[QUY TẮC THỜI GIAN NGHIÊM NGẶT - BẮT BUỘC TUÂN THỦ]:\n" +
                        "Thời gian hiện tại của người dùng đang là: " + currentTime + ".\n" +
                        "1. TUYỆT ĐỐI KHÔNG lên lịch (deadline hoặc dueTime) vào các ngày/giờ trong quá khứ (trước " + currentTime + ").\n" +
                        "2. Hãy tính toán khoảng thời gian từ " + currentTime + " cho đến Hạn chót mà người dùng yêu cầu, sau đó CHIA ĐỀU các công việc ra các ngày/giờ khác nhau một cách logic.\n" +
                        "3. Tránh việc dồn tất cả các công việc vào lúc 23:59. Hãy phân bổ dueTime rải rác trong ngày (Ví dụ: 09:00, 14:30, 20:00) để tạo thành một lịch trình có thể thực hiện được.\n" +
                        "\n" +
                        "[HƯỚNG DẪN HỆ THỐNG]: Nếu người dùng yêu cầu phân rã đồ án, lên lịch trình, bạn PHẢI ĐÍNH KÈM một chuỗi JSON ở CUỐI bài viết theo cấu trúc chính xác sau: " +
                        "---TASK_START--- {\"tasks\": [{\"taskName\": \"...\", \"description\": \"...\", \"category\": \"...\", \"deadline\": \"dd/MM/yyyy\", \"dueTime\": \"HH:mm\", \"estimatedMinutes\": 60, \"priority\": \"Cao\"}]} ---TASK_END---. " + // <-- CẬP NHẬT ĐỊNH DẠNG MẪU
                        "Phần nội dung văn bản ở trên hãy viết thật thân thiện, động viên người dùng và giải thích qua về lộ trình vừa lập.";

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

                                        FirebaseAuth auth = FirebaseAuth.getInstance();
                                        if (auth.getCurrentUser() != null) {
                                            String currentUid = auth.getCurrentUser().getUid();
                                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                                            int tasksCreatedCount = 0;

                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                JSONObject item = jsonArray.getJSONObject(i);

                                                // --- BƯỚC 2: BÓC TÁCH JSON MỚI & BẮT LỖI PHÒNG THỦ ---
                                                String name = item.getString("taskName");
                                                String deadline = item.getString("deadline");
                                                String priority = item.getString("priority");

                                                // Dùng has() để kiểm tra an toàn tránh crash
                                                String dueTime = item.has("dueTime") ? item.getString("dueTime") : "23:59";
                                                int estimatedMinutes = item.has("estimatedMinutes") ? item.getInt("estimatedMinutes") : 60;
                                                String category = item.has("category") ? item.getString("category") : "Gợi ý từ AI";

                                                // --- HỨNG TRƯỜNG DESCRIPTION TỪ AI VỀ ---
                                                String description = item.has("description") ? item.getString("description") : "";

                                                int newId = (int) (System.currentTimeMillis() / 1000) + i;

                                                // --- BƯỚC 3: DÙNG CONSTRUCTOR MỚI ---
                                                // Truyền biến 'description' vào đúng vị trí số 4 thay vì gõ cứng chuỗi "Tạo nhanh từ..."
                                                StudyTask newTask = new StudyTask(category, newId, name, description, deadline, priority, dueTime, estimatedMinutes, false);

                                                db.collection("users")
                                                        .document(currentUid)
                                                        .collection("user_tasks")
                                                        .document(String.valueOf(newId))
                                                        .set(newTask);

                                                tasksCreatedCount++;
                                            }

                                            cleanDisplayResponse = response.substring(0, response.indexOf("---TASK_START---")).trim();
                                            cleanDisplayResponse += "\n\n🤖 *[Hệ thống]: Đã tự động phân tích và lưu thành công " + tasksCreatedCount + " nhiệm vụ (đã dự phóng thời lượng) vào danh sách công việc của bạn!*";
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