package ThiCK.vuonghuyhoang.androidapp;

public class StudyTask {
    private int id;
    private String taskName;
    private String deadline;
    private String priority;
    private String dueTime;  // MỚI: Giờ hạn chót (VD: 23:59)
    private int estimatedMinutes; // MỚI: Thời lượng làm việc dự kiến (VD: 120)
    private boolean isCompleted;

    public StudyTask() {
    }

    // Cập nhật Constructor
    public StudyTask(int id, String taskName, String deadline, String dueTime, int estimatedMinutes, String priority, boolean isCompleted) {
        this.id = id;
        this.taskName = taskName;
        this.deadline = deadline;
        this.dueTime = dueTime;
        this.estimatedMinutes = estimatedMinutes;
        this.priority = priority;
        this.isCompleted = isCompleted;
    }

    // Getters
    // Thêm Getters và Setters cho 2 trường mới
    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public int getId() { return id; }
    public String getTaskName() { return taskName; }
    public String getDeadline() { return deadline; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return isCompleted; }

    // Setters
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
