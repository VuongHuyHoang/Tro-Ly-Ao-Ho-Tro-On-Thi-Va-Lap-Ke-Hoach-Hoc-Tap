package ThiCK.vuonghuyhoang.androidapp;

public class DiaryEntry {
    private String id;
    private String content;
    private long timestamp; // Lưu thời gian thực để lôi ra hiển thị

    public DiaryEntry() {
        // Constructor rỗng bắt buộc cho Firebase
    }

    public DiaryEntry(String id, String content, long timestamp) {
        this.id = id;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
}