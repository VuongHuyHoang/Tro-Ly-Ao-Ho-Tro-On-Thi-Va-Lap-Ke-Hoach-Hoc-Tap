package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class DiaryFragment extends Fragment {
    private RecyclerView recyclerDiaries;
    private FloatingActionButton fabAddDiary;
    private DiaryAdapter diaryAdapter;
    private List<DiaryEntry> diaryList;


    public DiaryFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_diary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvDiaryTitleHeader = view.findViewById(R.id.tv_diary_title_header);

        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        tvDiaryTitleHeader.setText("Nhật ký (" + currentYear + ")");


        recyclerDiaries = view.findViewById(R.id.recycler_diaries);
        fabAddDiary = view.findViewById(R.id.fab_add_diary);

        // Nút FAB mở Activity viết nhật ký (Thêm mới)
        fabAddDiary.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), WriteDiaryActivity.class);
            startActivity(intent);
        });

        // Thiết lập RecyclerView
        diaryList = new ArrayList<>();

        // ĐOẠN CODE ĐÃ ĐƯỢC SỬA: Lắng nghe cả sự kiện Xóa và Click xem chi tiết
        diaryAdapter = new DiaryAdapter(diaryList, new DiaryAdapter.OnDiaryItemClickListener() {
            @Override
            public void onDeleteClick(DiaryEntry diary) {
                showDeleteConfirmDialog(diary);
            }

            @Override
            public void onItemClick(DiaryEntry diary) {
                // Mở màn hình WriteDiaryActivity nhưng truyền kèm dữ liệu cũ sang để Sửa
                Intent intent = new Intent(getActivity(), WriteDiaryActivity.class);
                intent.putExtra("DIARY_ID", diary.getId());
                intent.putExtra("DIARY_CONTENT", diary.getContent());
                startActivity(intent);
            }
        });

        recyclerDiaries.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerDiaries.setAdapter(diaryAdapter);

        // Tải dữ liệu từ Firebase
        loadDiariesFromFirebase();
    }

    private void loadDiariesFromFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();

            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .collection("user_diaries")
                    .orderBy("timestamp", Query.Direction.DESCENDING) // Xếp nhật ký mới nhất lên đầu
                    .addSnapshotListener((value, error) -> {
                        if (error != null) return;
                        if (value != null) {
                            diaryList.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                DiaryEntry entry = doc.toObject(DiaryEntry.class);
                                if (entry != null) diaryList.add(entry);
                            }
                            diaryAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void showDeleteConfirmDialog(DiaryEntry diary) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa nhật ký")
                .setMessage("Bạn có chắc chắn muốn xóa bản ghi này không?")
                .setPositiveButton("Xóa", (dialog, which) -> executeDeleteDiary(diary))
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void executeDeleteDiary(DiaryEntry diary) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String currentUid = auth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUid)
                    .collection("user_diaries")
                    .document(diary.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Đã xóa", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}