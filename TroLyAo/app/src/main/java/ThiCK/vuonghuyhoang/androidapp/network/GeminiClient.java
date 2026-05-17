package ThiCK.vuonghuyhoang.androidapp.network;

import com.google.gson.internal.GsonBuildConfig;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import ThiCK.vuonghuyhoang.androidapp.BuildConfig;


public class GeminiClient {

    // THAY API KEY CỦA BẠN VÀO DÒNG DƯỚI ĐÂY:
    private static final String API_KEY = BuildConfig.GEMINI_API_KEY;

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;

    public GeminiClient() {
        // Cấu hình thời gian chờ mạng tối đa là 30 giây
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void sendPrompt(String prompt, AiCallback callback) {
        try {
            // 1. Đóng gói câu hỏi thành định dạng JSON chuẩn của Google
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject partsObject = new JSONObject();
            JSONArray textArray = new JSONArray();
            JSONObject textObject = new JSONObject();

            textObject.put("text", prompt);
            textArray.put(textObject);
            partsObject.put("parts", textArray);
            contentsArray.put(partsObject);
            jsonBody.put("contents", contentsArray);

            // 2. Tạo Request gửi lên Server
            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
            System.out.println("TEST_URL: " + URL);
            Request request = new Request.Builder()
                    .url(URL)
                    .post(body)
                    .build();

            // 3. Gửi Request chạy ngầm (Asynchronous)
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError("Lỗi mạng: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body().string();
                        callback.onError("Lỗi API: " + response.code() + "\n" + errorBody);
                        return;
                    }

                    try {
                        // 4. Bóc tách dữ liệu JSON do AI trả về để lấy phần văn bản
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);

                        String aiResponseText = jsonResponse.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        // Trả kết quả về cho Giao diện
                        callback.onSuccess(aiResponseText);
                    } catch (Exception e) {
                        callback.onError("Lỗi đọc dữ liệu AI: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onError("Lỗi tạo yêu cầu: " + e.getMessage());
        }
    }
}
