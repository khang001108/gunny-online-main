# Cập nhật Gunny Java 7.1.4

Không cần SQL mới. Game vẫn tương thích server 7.1.3 có `cards:1` và `items:2`; gói server 7.1.4 kèm trang tải và bộ cài mới. Không tự triển khai máy chủ.

## Đăng nhập

- Popup gọn trên nền sảnh menu chính, giữ thanh biểu tượng trong suốt ở dưới. Bỏ nền sao riêng, thu nhỏ logo, nâng bảng lên, căn lại viền và nút X bằng asset gốc.
- Máy chủ và Chưa có TK? Đăng ký nằm trong góc dưới bên phải. Máy chủ mở trang `/download/` của địa chỉ máy chủ đang cấu hình; bỏ nút thử máy chủ.
- Lưu mật khẩu và Tự đăng nhập nằm cạnh nhau, dùng được bằng chạm hoặc bốn phím hướng. Tự đăng nhập bật luôn lưu mật khẩu; bỏ lưu mật khẩu sẽ tắt tự đăng nhập và xóa mật khẩu đã lưu.
- Chỉ tự đăng nhập một lần khi khởi động ứng dụng. Quay lại từ ô nhập hoặc tiếp tục ứng dụng không gửi lại yêu cầu. Sai tài khoản dừng ở bảng đăng nhập để sửa, không thử lặp.
- Tùy chọn lưu trong RMS trên thiết bị, đọc được bản lưu cũ không có tùy chọn tự đăng nhập (mặc định tắt). Các bước cho phép mạng và xác nhận email vẫn theo yêu cầu của máy/server.

## Phòng

Bỏ hai khung thông tin/map cũ phía dưới hai thẻ nhân vật của phòng thường, mở rộng thẻ xuống phần trống bằng khung asset gốc. Map vẫn nằm trong cột bên phải và bấm để chọn. Ô chờ mở/đóng được cả ở phần dưới thẻ; thanh công cụ và các tooltip giữ nguyên. Bố cục phòng Tổ đội 3v3 giữ nguyên.

## Kiểm tra

Biên dịch MIDP/CLDC, đóng gói E72 320×240 và E6 640×480; kiểm tra popup, điều hướng, RMS, đăng nhập tự động và mở/đóng ô chờ bằng MicroEmulator cùng server Memory trên localhost. Kiểm tra toàn bộ gói server bằng verify.ps1. Chưa kiểm tra trên điện thoại Nokia thật.
