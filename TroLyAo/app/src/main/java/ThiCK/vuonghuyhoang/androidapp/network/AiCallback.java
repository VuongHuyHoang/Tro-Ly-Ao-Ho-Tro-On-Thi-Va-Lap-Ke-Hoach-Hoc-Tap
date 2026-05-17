package ThiCK.vuonghuyhoang.androidapp.network;

public interface AiCallback {
    void onSuccess(String response); // Gọi khi AI trả về kết quả thành công
    void onError(String error);      // Gọi khi rớt mạng hoặc lỗi API
}
