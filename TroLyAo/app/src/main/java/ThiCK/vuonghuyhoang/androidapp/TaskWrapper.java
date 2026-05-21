package ThiCK.vuonghuyhoang.androidapp;

public class TaskWrapper {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_TASK = 1;

    public int type;
    public String headerTitle;
    public StudyTask task;
    public boolean isExpanded; // Dùng để biết danh mục đang mở hay đóng

    // Constructor dùng để tạo Thanh Tiêu Đề
    public TaskWrapper(String headerTitle, boolean isExpanded) {
        this.type = TYPE_HEADER;
        this.headerTitle = headerTitle;
        this.isExpanded = isExpanded;
    }

    // Constructor dùng để tạo Thẻ Công Việc
    public TaskWrapper(StudyTask task) {
        this.type = TYPE_TASK;
        this.task = task;
    }
}