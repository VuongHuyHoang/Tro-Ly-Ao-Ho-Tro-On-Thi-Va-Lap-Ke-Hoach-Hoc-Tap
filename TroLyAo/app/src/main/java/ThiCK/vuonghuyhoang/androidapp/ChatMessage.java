package ThiCK.vuonghuyhoang.androidapp;

public class ChatMessage {
    private String content;
    private boolean isUser; // 'true' nếu là bạn nhắn, 'false' nếu là AI nhắn

    public ChatMessage(String content, boolean isUser) {
        this.content = content;
        this.isUser = isUser;
    }

    public String getContent() { return content; }
    public boolean isUser() { return isUser; }
}
