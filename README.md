# Gunny Java Online 7.7.0

Dừng đồng hồ khi giữ lực, giảm tốc độ lực và bỏ lượt sau một lần vượt lực quay về 0.
Thiết lập hỗ trợ gán phím tùy chọn, lưu trên máy và cập nhật nhãn hướng dẫn/HUD.
Toàn bộ giao tiếp game dùng TCP với socket lệnh và socket thông báo; web/admin và
Supabase vẫn dùng HTTPS.

Đăng ký dùng email tự đặt, không gửi thư xác nhận. Tài khoản cũ giữ cùng UUID,
mật khẩu, nhân vật và vật phẩm. Admin có hạn không hoạt động mặc định 90 ngày,
chỉnh hạn chung/riêng và gia hạn thêm ngày; tự xoá tắt mặc định.

Xem [hướng dẫn cập nhật 7.7.0](docs/UPDATE-7.7.0.md). Với project đang chạy, tự chạy
[migration riêng](supabase/migrations/202611120001_virtual_accounts_retention.sql)
trong SQL Editor của project `mnquwasxacifkktmckdv`, rồi mới cập nhật server/client.
Không tự chạy migration khi container khởi động.

Build backend: `docker build -t gunny-online:7.7.0 .`.
Cấu hình [Portainer](deploy/portainer/README.md), [Pi/systemd](deploy/pi5/README.md).
Cổng game `22122/TCP`, web nội bộ `8080`; Caddy nhận HTTPS 443.

Kiểm thử: JDK 17+ và `bash verify.sh` trên Linux. Công cụ
`python3 deploy/check-network.py --tcp-host gunnyjava.xubi.org --web-url https://gunnyjava.xubi.org`
kiểm tra giao thức game thật mà không đăng nhập hay ghi dữ liệu.

Bản mã nguồn/bộ cài 7.7.0 được chuẩn bị riêng; server production 7.6.9 chưa được thay
và migration Supabase chưa được chạy theo yêu cầu chủ máy.
