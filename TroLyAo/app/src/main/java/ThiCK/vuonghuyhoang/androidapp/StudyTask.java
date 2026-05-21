package ThiCK.vuonghuyhoang.androidapp;

public class StudyTask {
    private String category;
    private int id;
    private String taskName;
    private String deadline;
    private String priority;
    private String dueTime;
    private int orderIndex;
    private int estimatedMinutes;
    private boolean isCompleted;

    public StudyTask() {
    }

    public StudyTask(String category, int id, String taskName, String deadline, String priority, String dueTime, int estimatedMinutes, boolean isCompleted) {
        this.category = category;
        this.id = id;
        this.taskName = taskName;
        this.deadline = deadline;
        this.priority = priority;
        this.dueTime = dueTime;
        this.estimatedMinutes = estimatedMinutes;
        this.isCompleted = isCompleted;
    }

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

    // Setters
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
