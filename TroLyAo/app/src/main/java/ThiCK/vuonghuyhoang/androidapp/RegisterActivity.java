package ThiCK.vuonghuyhoang.androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edt_register_email);
        edtPassword = findViewById(R.id.edt_register_password);
        btnRegister = findViewById(R.id.btn_register_submit);
        tvGoToLogin = findViewById(R.id.tv_go_to_login);

        TextInputEditText edtName = findViewById(R.id.edt_register_name);
        TextInputEditText edtMssv = findViewById(R.id.edt_register_mssv);
        TextInputEditText edtClass = findViewById(R.id.edt_register_class);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String fullName = edtName.getText().toString().trim();
                String mssv = edtMssv.getText().toString().trim();
                String className = edtClass.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || fullName.isEmpty() || mssv.isEmpty() || className.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập đầy đủ tất cả các trường", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vô hiệu hóa nút bấm tạm thời để tránh người dùng nhấn liên tục khi đang xử lý mạng
                btnRegister.setEnabled(false);

                // 1. Gọi hàm tạo tài khoản xác thực của Firebase Auth
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 2. Lấy mã UID độc nhất vừa sinh ra của tài khoản này
                                String uid = mAuth.getCurrentUser().getUid();

                                // 3. Đóng gói dữ liệu tiếng Việt có dấu vào Object UserProfile
                                UserProfile profile = new UserProfile(fullName, mssv, className);

                                // 4. Đẩy thông tin Profile lên Cloud Firestore đám mây
                                FirebaseFirestore.getInstance()
                                        .collection("profiles")
                                        .document(uid)
                                        .set(profile)
                                        .addOnCompleteListener(firestoreTask -> {
                                            // Bật lại nút bấm
                                            btnRegister.setEnabled(true);

                                            if (firestoreTask.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

                                                // Chuyển thẳng vào màn hình chính MainActivity
                                                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                                startActivity(intent);
                                                finishAffinity(); // Xóa sạch các activity trước đó khỏi ngăn xếp stack
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Lỗi tạo hồ sơ Firestore: " + firestoreTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        });
                            } else {
                                btnRegister.setEnabled(true);
                                Toast.makeText(RegisterActivity.this, "Lỗi đăng ký tài khoản: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}