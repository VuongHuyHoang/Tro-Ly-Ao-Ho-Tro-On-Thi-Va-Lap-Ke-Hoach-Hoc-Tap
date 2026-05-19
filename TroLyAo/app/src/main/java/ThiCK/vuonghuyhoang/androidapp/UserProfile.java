package ThiCK.vuonghuyhoang.androidapp;

public class UserProfile {
    private String fullName;
    private String studentId;
    private String className;

    // Constructor rỗng bắt buộc cho Firebase
    public UserProfile() {
    }

    public UserProfile(String fullName, String studentId, String className) {
        this.fullName = fullName;
        this.studentId = studentId;
        this.className = className;
    }

    // Getters và Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
}