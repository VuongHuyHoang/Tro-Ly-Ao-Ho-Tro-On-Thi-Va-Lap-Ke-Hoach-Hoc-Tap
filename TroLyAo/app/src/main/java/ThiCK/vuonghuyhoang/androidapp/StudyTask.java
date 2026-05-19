package ThiCK.vuonghuyhoang.androidapp;

public class StudyTask {
    private int id;
    private String taskName;
    private String deadline;
    private String priority;
    private boolean isCompleted;

    public StudyTask() {
    }

    public StudyTask(int id, String taskName, String deadline, String priority, boolean isCompleted) {
        this.id = id;
        this.taskName = taskName;
        this.deadline = deadline;
        this.priority = priority;
        this.isCompleted = isCompleted;
    }

    // Getters
    public int getId() { return id; }
    public String getTaskName() { return taskName; }
    public String getDeadline() { return deadline; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return isCompleted; }

    // Setters
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
