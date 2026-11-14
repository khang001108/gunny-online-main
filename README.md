# Gunny Java Online 7.8.1

Sảnh đảo mới, tiệm rèn, đá cường hóa và phó bản Huyệt Ma Kiến cho 1–3 người.
Di chuyển từ xa nội suy từ trạng thái TCP; 3v3 có 18 thẻ và hai lượt chọn mỗi người.
Rèn đồ tích lũy đá đến +15. Mua đá giá 80 tiền mua sắm mỗi viên.
Phó bản chơi một mình offline; đăng nhập để tổ đội. Tiệm rèn có tích Liên tục,
chỉ rèn mới vũ khí/áo/nón/kính; túi đồ dùng biểu tượng gốc.
7.8.1 thêm xác nhận thoát ở sảnh, sửa giao diện PvP sau phó bản và luồng chuyển Online/Offline. Xem [ghi chú bản 7.8.1](docs/UPDATE-7.8.1.md).

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
Nếu schema đang ở 7.7.0, chạy một lần [SQL nâng cấp gộp](supabase/UPDATE-7.7.0-TO-7.8.0.sql).
Nếu máy chủ đã chạy 7.8.1 trước bản cập nhật chat, chạy [SQL chat](supabase/migrations/202611150001_gunny_chat.sql).
Nếu máy chủ đã có chat, chạy [SQL mở rộng emoji](supabase/migrations/202611150002_gunny_chat_emoji.sql).
Ở sảnh online, nhấn G hoặc chạm icon Chat để mở Hiện tại và Chat mật.
Chat công khai làm mới lúc 0 giờ Việt Nam. Tin nhắn bạn bè lưu 30 ngày.
Không tự chạy migration khi container khởi động.

Build backend: `docker build -t gunny-online:7.8.1 .`.
Tool cập nhật có menu và tự phục hồi: `sudo bash deploy/cap-nhat-gunny.sh`.
Xem [cách dùng tool trên Pi 5](deploy/CAP-NHAT-DOCKER.md). Tool chỉ cập nhật sau khi
bạn xác nhận đã tự chạy SQL và schema được kiểm tra thành công.
Cấu hình [Portainer](deploy/portainer/README.md), [Pi/systemd](deploy/pi5/README.md).
Cổng game `22122/TCP`, web nội bộ `8080`; Caddy nhận HTTPS 443.

Kiểm thử: JDK 17+ và `bash verify.sh` trên Linux. Công cụ
`python3 deploy/check-network.py --tcp-host gunnyjava.xubi.org --web-url https://gunnyjava.xubi.org`
kiểm tra giao thức game thật mà không đăng nhập hay ghi dữ liệu.

