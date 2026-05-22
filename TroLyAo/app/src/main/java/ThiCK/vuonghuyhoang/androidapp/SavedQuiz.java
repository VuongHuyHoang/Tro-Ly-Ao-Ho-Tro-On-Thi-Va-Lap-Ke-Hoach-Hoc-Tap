package ThiCK.vuonghuyhoang.androidapp;

import java.util.List;

public class SavedQuiz {
    // Thêm biến này để lưu ID của Document trên Firebase (Dùng cho việc xóa)
    @com.google.firebase.firestore.Exclude
    private String documentId;

    private String id;
    private String title;
    private long timestamp;
    private int score;
    private int totalQuestions;
    private List<QuizQuestion> questions;

    public SavedQuiz() {
        // Constructor rỗng bắt buộc cho Firebase
    }

    public SavedQuiz(String id, String title, long timestamp, int score, int totalQuestions, List<QuizQuestion> questions) {
        this.id = id;
        this.title = title;
        this.timestamp = timestamp;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.questions = questions;
    }

    // Getter và Setter cho documentId
    @com.google.firebase.firestore.Exclude
    public String getDocumentId() { return documentId; }
    @com.google.firebase.firestore.Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public long getTimestamp() { return timestamp; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public List<QuizQuestion> getQuestions() { return questions; }
}