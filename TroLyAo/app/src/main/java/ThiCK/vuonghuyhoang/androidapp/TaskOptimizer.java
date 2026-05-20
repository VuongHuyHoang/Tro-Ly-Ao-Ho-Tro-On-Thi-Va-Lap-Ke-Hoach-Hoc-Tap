package ThiCK.vuonghuyhoang.androidapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskOptimizer {

    public static List<StudyTask> getOptimalTasks(List<StudyTask> tasks, int totalAvailableMinutes) {
        int n = tasks.size();
        if (n == 0 || totalAvailableMinutes <= 0) return new ArrayList<>();

        int[] values = new int[n];
        int[] weights = new int[n];

        for (int i = 0; i < n; i++) {
            weights[i] = tasks.get(i).getEstimatedMinutes();
            String p = tasks.get(i).getPriority();
            values[i] = "Cao".equals(p) ? 3 : ("Trung bình".equals(p) ? 2 : 1);
        }

        int[][] dp = new int[n + 1][totalAvailableMinutes + 1];

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= totalAvailableMinutes; j++) {
                if (weights[i - 1] <= j) {
                    dp[i][j] = Math.max(values[i - 1] + dp[i - 1][j - weights[i - 1]], dp[i - 1][j]);
                } else {
                    dp[i][j] = dp[i - 1][j];
                }
            }
        }

        List<StudyTask> selectedTasks = new ArrayList<>();
        int res = dp[n][totalAvailableMinutes];
        int w = totalAvailableMinutes;

        for (int i = n; i > 0 && res > 0; i--) {
            if (res != dp[i - 1][w]) {
                selectedTasks.add(tasks.get(i - 1));
                res -= values[i - 1];
                w -= weights[i - 1];
            }
        }

        // TÍNH NĂNG MỚI: SẮP XẾP CHRONOLOGICAL (Theo thứ tự Giờ Hạn Chót)
        // Sau khi AI đã chọn xong các món đồ vào balo, ta sắp xếp lại chúng từ sáng đến tối
        Collections.sort(selectedTasks, new Comparator<StudyTask>() {
            @Override
            public int compare(StudyTask t1, StudyTask t2) {
                // Lấy giờ (VD: "14:30" và "08:15")
                String time1 = t1.getDueTime() != null ? t1.getDueTime() : "23:59";
                String time2 = t2.getDueTime() != null ? t2.getDueTime() : "23:59";

                // So sánh chuỗi thời gian (Theo chuẩn HH:mm, so sánh chuỗi cũng tương đương so sánh giờ)
                return time1.compareTo(time2);
            }
        });

        return selectedTasks;
    }
}