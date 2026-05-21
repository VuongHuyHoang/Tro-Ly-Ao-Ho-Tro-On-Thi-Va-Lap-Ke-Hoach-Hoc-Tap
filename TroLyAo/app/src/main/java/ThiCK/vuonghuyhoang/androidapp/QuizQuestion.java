package ThiCK.vuonghuyhoang.androidapp;

import java.util.List;

public class QuizQuestion {
    private String question;
    private List<String> options;
    private int correctIndex;
    private String explanation;

    public QuizQuestion() {}

    public String getQuestion() { return question; }
    public List<String> getOptions() { return options; }
    public int getCorrectIndex() { return correctIndex; }
    public String getExplanation() { return explanation; }
}