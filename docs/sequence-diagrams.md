# Sơ Đồ Tuần Tự (Sequence Diagram)

## 1. Chức năng Quản Lý Tài Liệu

### 1.1. Tải lên tài liệu (Upload file PDF/DOCX)

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHThemTL as Màn Hình Thêm Tài Liệu<br/>(AddDocumentActivity)
    participant HeDieuHanh as Hệ Điều Hành Android
    participant BoDocPDF as Bộ Đọc PDF<br/>(PDFBox)
    participant BoDocDOCX as Bộ Đọc DOCX<br/>(ZipInputStream)

    NguoiDung->>MHThemTL: Bấm nút "Chọn File"
    MHThemTL->>HeDieuHanh: Mở trình chọn file<br/>(PDF hoặc DOCX)
    HeDieuHanh-->>NguoiDung: Hiển thị danh sách file
    NguoiDung->>HeDieuHanh: Chọn file tài liệu
    HeDieuHanh-->>MHThemTL: Trả về đường dẫn file (URI)

    MHThemTL->>MHThemTL: Hiển thị hộp thoại<br/>"Đang trích xuất văn bản..."

    alt File PDF
        MHThemTL->>BoDocPDF: Gửi luồng dữ liệu file PDF
        BoDocPDF->>BoDocPDF: Đọc và trích xuất văn bản
        BoDocPDF-->>MHThemTL: Trả về nội dung văn bản
    else File DOCX
        MHThemTL->>BoDocDOCX: Giải nén file DOCX (ZIP)
        BoDocDOCX->>BoDocDOCX: Tìm file word/document.xml
        BoDocDOCX->>BoDocDOCX: Loại bỏ thẻ XML,<br/>giữ lại văn bản thuần
        BoDocDOCX-->>MHThemTL: Trả về nội dung văn bản
    end

    MHThemTL->>MHThemTL: Đóng hộp thoại chờ
    MHThemTL-->>NguoiDung: Hiển thị nội dung văn bản<br/>vào ô nhập liệu
```

### 1.2. Nhập tài liệu thủ công (Dán text trực tiếp)

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHThemTL as Màn Hình Thêm Tài Liệu<br/>(AddDocumentActivity)

    NguoiDung->>MHThemTL: Mở màn hình Thêm Tài Liệu
    MHThemTL-->>NguoiDung: Hiển thị ô nhập nội dung trống
    NguoiDung->>MHThemTL: Dán hoặc gõ nội dung<br/>tài liệu vào ô nhập liệu
    MHThemTL-->>NguoiDung: Nội dung sẵn sàng<br/>để tạo đề thi
```

---

## 2. Chức năng Tạo Đề Thi

### 2.1. Tạo đề thi tự động từ tài liệu

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHThemTL as Màn Hình Thêm Tài Liệu<br/>(AddDocumentActivity)
    participant BoGiaoTiepAI as Bộ Giao Tiếp AI<br/>(GeminiClient)
    participant MayChutGoogle as Máy Chủ Google<br/>(Gemini API)
    participant MHXemTruoc as Màn Hình Xem Trước Đề<br/>(QuizPreviewActivity)

    NguoiDung->>MHThemTL: Chọn mức độ khó<br/>(Cơ bản / Trung bình / Nâng cao)
    NguoiDung->>MHThemTL: Bấm nút "Tạo Đề Thi"

    MHThemTL->>MHThemTL: Kiểm tra nội dung tài liệu<br/>(tối thiểu 50 ký tự)

    alt Nội dung quá ngắn
        MHThemTL-->>NguoiDung: Thông báo lỗi:<br/>"Nội dung quá ngắn"
    else Nội dung hợp lệ
        MHThemTL-->>NguoiDung: Hiển thị hộp thoại chọn số câu hỏi<br/>(3 / 5 / 10 / 15 câu)
        NguoiDung->>MHThemTL: Chọn số lượng câu hỏi

        MHThemTL->>MHThemTL: Hiển thị hộp thoại chờ:<br/>"AI đang phân tích tài liệu..."
        MHThemTL->>BoGiaoTiepAI: Gửi nội dung tài liệu +<br/>yêu cầu tạo đề (prompt)
        BoGiaoTiepAI->>MayChutGoogle: Gửi HTTP POST<br/>(JSON chứa prompt)
        MayChutGoogle->>MayChutGoogle: AI phân tích tài liệu<br/>và biên soạn câu hỏi
        MayChutGoogle-->>BoGiaoTiepAI: Trả về JSON chứa<br/>danh sách câu hỏi

        BoGiaoTiepAI->>BoGiaoTiepAI: Trích xuất phần văn bản<br/>từ phản hồi API
        BoGiaoTiepAI-->>MHThemTL: Trả về chuỗi JSON đề thi

        MHThemTL->>MHThemTL: Đóng hộp thoại chờ
        MHThemTL->>MHThemTL: Tách JSON từ thẻ đánh dấu<br/>(QUIZ_START / QUIZ_END)

        MHThemTL->>MHXemTruoc: Chuyển sang màn hình Xem Trước<br/>(kèm dữ liệu JSON đề thi)
        MHXemTruoc-->>NguoiDung: Hiển thị danh sách câu hỏi<br/>để xem trước và chỉnh sửa
    end
```

### 2.2. Xem trước và chỉnh sửa đề thi

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHXemTruoc as Màn Hình Xem Trước Đề<br/>(QuizPreviewActivity)
    participant MHLamBai as Màn Hình Làm Bài Thi<br/>(QuizActivity)

    MHXemTruoc->>MHXemTruoc: Phân tích JSON thành<br/>danh sách câu hỏi
    MHXemTruoc-->>NguoiDung: Hiển thị danh sách câu hỏi<br/>(có đánh dấu đáp án đúng)

    opt Chỉnh sửa câu hỏi
        NguoiDung->>MHXemTruoc: Bấm vào câu hỏi cần sửa
        MHXemTruoc-->>NguoiDung: Hiển thị hộp thoại chỉnh sửa<br/>(nội dung câu hỏi + đáp án)
        NguoiDung->>MHXemTruoc: Sửa nội dung và bấm "Lưu thay đổi"
        MHXemTruoc->>MHXemTruoc: Cập nhật dữ liệu câu hỏi
        MHXemTruoc-->>NguoiDung: Cập nhật giao diện danh sách
    end

    NguoiDung->>MHXemTruoc: Bấm nút "Lưu và Làm bài"
    MHXemTruoc->>MHXemTruoc: Đóng gói danh sách câu hỏi<br/>thành chuỗi JSON
    MHXemTruoc->>MHLamBai: Chuyển sang Màn Hình Làm Bài<br/>(kèm dữ liệu JSON)
```

### 2.3. Làm bài thi trắc nghiệm

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHLamBai as Màn Hình Làm Bài Thi<br/>(QuizActivity)
    participant BoDemGio as Bộ Đếm Thời Gian<br/>(CountDownTimer)
    participant Firebase as Cơ Sở Dữ Liệu<br/>(Firebase Firestore)

    MHLamBai->>MHLamBai: Phân tích JSON thành<br/>danh sách câu hỏi (Gson)
    MHLamBai-->>NguoiDung: Hiển thị câu hỏi đầu tiên
    MHLamBai->>BoDemGio: Khởi động đếm ngược 30 giây

    loop Với mỗi câu hỏi
        BoDemGio-->>MHLamBai: Cập nhật thời gian còn lại mỗi giây

        alt Người dùng chọn đáp án
            NguoiDung->>MHLamBai: Bấm chọn đáp án
            MHLamBai->>BoDemGio: Dừng bộ đếm thời gian
            MHLamBai->>MHLamBai: Kiểm tra đáp án đúng/sai
            alt Đáp án đúng
                MHLamBai->>MHLamBai: Cộng điểm, tô xanh đáp án
            else Đáp án sai
                MHLamBai->>MHLamBai: Tô đỏ đáp án sai,<br/>tô xanh đáp án đúng
            end
        else Hết thời gian 30 giây
            BoDemGio-->>MHLamBai: Thông báo hết giờ
            MHLamBai->>MHLamBai: Tô xanh đáp án đúng<br/>(không cộng điểm)
        end

        MHLamBai-->>NguoiDung: Hiển thị giải thích đáp án
        NguoiDung->>MHLamBai: Bấm "Câu tiếp theo"
        MHLamBai->>BoDemGio: Khởi động lại đếm ngược 30 giây
        MHLamBai-->>NguoiDung: Hiển thị câu hỏi tiếp theo
    end

    MHLamBai->>Firebase: Lưu kết quả bài thi<br/>(điểm số, danh sách câu hỏi)
    Firebase-->>MHLamBai: Xác nhận lưu thành công
    MHLamBai->>Firebase: Cập nhật điểm cao nhất<br/>(nếu điểm mới cao hơn)
    MHLamBai-->>NguoiDung: Hiển thị kết quả:<br/>"Số câu đúng: X / Y"
```

### 2.4. Quản lý lịch sử đề thi

```mermaid
sequenceDiagram
    actor NguoiDung as Người Dùng
    participant MHHoSo as Màn Hình Hồ Sơ<br/>(ProfileFragment)
    participant MHLichSu as Màn Hình Lịch Sử Đề Thi<br/>(QuizHistoryActivity)
    participant Firebase as Cơ Sở Dữ Liệu<br/>(Firebase Firestore)
    participant MHLamBai as Màn Hình Làm Bài Thi<br/>(QuizActivity)
    participant MHThemTL as Màn Hình Thêm Tài Liệu<br/>(AddDocumentActivity)

    NguoiDung->>MHHoSo: Bấm nút "Lịch sử đề thi"
    MHHoSo->>MHLichSu: Mở Màn Hình Lịch Sử Đề Thi
    MHLichSu->>Firebase: Truy vấn danh sách đề thi<br/>(sắp xếp theo thời gian mới nhất)
    Firebase-->>MHLichSu: Trả về danh sách đề thi đã lưu
    MHLichSu-->>NguoiDung: Hiển thị danh sách đề thi

    alt Làm lại đề thi cũ
        NguoiDung->>MHLichSu: Bấm vào đề thi cần ôn lại
        MHLichSu-->>NguoiDung: Hiển thị xác nhận "Bắt đầu ôn tập?"
        NguoiDung->>MHLichSu: Bấm "Bắt đầu làm"
        MHLichSu->>MHLamBai: Chuyển sang Màn Hình Làm Bài<br/>(kèm dữ liệu câu hỏi)
    else Đổi tên đề thi
        NguoiDung->>MHLichSu: Nhấn giữ đề thi
        MHLichSu-->>NguoiDung: Hiển thị menu tùy chọn
        NguoiDung->>MHLichSu: Chọn "Đổi tên đề thi"
        MHLichSu-->>NguoiDung: Hiển thị ô nhập tên mới
        NguoiDung->>MHLichSu: Nhập tên mới, bấm "Lưu"
        MHLichSu->>Firebase: Cập nhật tên đề thi trên server
        Firebase-->>MHLichSu: Xác nhận cập nhật thành công
    else Xóa đề thi
        NguoiDung->>MHLichSu: Nhấn giữ đề thi
        MHLichSu-->>NguoiDung: Hiển thị menu tùy chọn
        NguoiDung->>MHLichSu: Chọn "Xóa đề thi"
        MHLichSu-->>NguoiDung: Hiển thị xác nhận xóa
        NguoiDung->>MHLichSu: Bấm "Xóa"
        MHLichSu->>Firebase: Xóa đề thi khỏi server
        Firebase-->>MHLichSu: Xác nhận xóa thành công
    else Tạo đề thi mới
        NguoiDung->>MHLichSu: Bấm nút "Tạo đề mới"
        MHLichSu->>MHThemTL: Chuyển sang Màn Hình Thêm Tài Liệu
    end
```
