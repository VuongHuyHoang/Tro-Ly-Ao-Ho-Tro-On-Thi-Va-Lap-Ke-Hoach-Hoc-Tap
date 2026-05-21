package ThiCK.vuonghuyhoang.androidapp;

import java.util.List;

public class SavedQuiz {
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

    public String getId() { return id; }
    public String getTitle() { return title; }
    public long getTimestamp() { return timestamp; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public List<QuizQuestion> getQuestions() { return questions; }
}