package ThiCK.vuonghuyhoang.androidapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Yêu cầu một public constructor rỗng
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện từ file XML
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ các thành phần giao diện (Views)
        MaterialButton btnAddDocument = view.findViewById(R.id.btn_add_document);
        RecyclerView recyclerTasks = view.findViewById(R.id.recycler_tasks);

        // 2. Xử lý sự kiện click nút Thêm tài liệu
        btnAddDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(requireContext(), "Mở màn hình thêm tài liệu", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. Khởi tạo dữ liệu giả (Mock Data) để test UI
        List<StudyTask> dummyTasks = new ArrayList<>();
        dummyTasks.add(new StudyTask(1, "Vẽ sơ đồ DFD và Class Diagram", "18/05/2026 - 15:00", "Cao", false));
        dummyTasks.add(new StudyTask(2, "Code giao diện Home và Chat", "19/05/2026 - 23:59", "Cao", false));
        dummyTasks.add(new StudyTask(3, "Đăng ký API Key Gemini", "20/05/2026 - 10:00", "Trung bình", true));
        dummyTasks.add(new StudyTask(4, "Ôn tập 20 câu hỏi trắc nghiệm", "21/05/2026 - 20:00", "Thấp", false));

        // 4. Thiết lập RecyclerView hiển thị danh sách
        TaskAdapter taskAdapter = new TaskAdapter(dummyTasks);
        recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerTasks.setAdapter(taskAdapter);

        // Tắt tính năng cuộn lồng nhau để cuộn mượt hơn với NestedScrollView ở ngoài
        recyclerTasks.setNestedScrollingEnabled(false);
    }
}
