package ThiCK.vuonghuyhoang.androidapp;

public class StudyTask {
    private String category;
    private int id;
    private String taskName;
    private String description; // Đã thêm biến mô tả
    private String deadline;
    private String priority;
    private String dueTime;
    private int orderIndex;
    private int estimatedMinutes;
    private boolean isCompleted;

    public StudyTask() {
    }

    // 🟢 2. CONSTRUCTOR ĐẦY ĐỦ THAM SỐ (Dùng khi tạo mới Task)
    // Lưu ý thứ tự: category, id, taskName, description, deadline, priority, dueTime, estimatedMinutes, isCompleted
    public StudyTask(String category, int id, String taskName, String description, String deadline, String priority, String dueTime, int estimatedMinutes, boolean isCompleted) {
        this.category = category;
        this.id = id;
        this.taskName = taskName;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.dueTime = dueTime;
        this.estimatedMinutes = estimatedMinutes;
        this.isCompleted = isCompleted;
        this.orderIndex = 0; // Mặc định khi tạo mới
    }

    // --- GETTERS VÀ SETTERS ---
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public int getId() { return id; }

    public String getTaskName() { return taskName; }

    public String getDeadline() { return deadline; }

    public String getPriority() { return priority; }

    public boolean isCompleted() { return isCompleted; }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public void setCompleted(boolean completed) { isCompleted = completed; }
}