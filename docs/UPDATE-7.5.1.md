# Sửa trong trận online và popup 7.5.1

- Nhân vật cầm asset máy bay khi lấy lực phi máy bay; không vẽ vũ khí đang trang bị vào tay. Áp dụng chung offline, online và phần phát lại của đối phương. Không thay sát thương, quỹ đạo hoặc gói vật lý.
- Chỉ hỏi cảm xúc nền khi client ở pha đang đấu. Trong pha chờ/tải/kết quả, server trả bộ cảm xúc rỗng cho yêu cầu đọc thay vì lỗi. Yêu cầu gửi cảm xúc vẫn được kiểm tra và chặn ngoài trận. Phản hồi từ yêu cầu đọc nền không bật popup lỗi lặp, để việc đồng bộ trận tiếp tục ngay cả khi server cũ chưa cập nhật.
- Nút popup dùng lại asset gốc, giữ tỷ lệ chiều cao/viền; phần giữa kéo theo chiều rộng chữ, căn giữa, vùng chạm và viền chọn theo đúng kích thước nút. Dùng chung cho thông báo, menu, xác nhận, startup và cảnh báo tài khoản.

Cập nhật toàn bộ gói server và JAR 7.5.1 đúng E72/E6. Không cần migration mới cho thay đổi trong trận. Nếu danh sách quản trị còn hiện dấu —, chạy bản vá `202611060001_gunny_admin_accounts_fix.sql` đã có trong gói; xem [hướng dẫn quản trị](UPDATE-7.5.0.md).

Không deploy Render hay chạy SQL thật trong quá trình sửa. Kiểm tra trên giả lập và server loopback; chưa kiểm tra trên Nokia thật.
