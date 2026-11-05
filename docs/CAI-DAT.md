# Sửa trong trận online và popup 7.5.1

- Nhân vật cầm asset máy bay khi lấy lực phi máy bay; không vẽ vũ khí đang trang bị vào tay. Áp dụng chung offline, online và phần phát lại của đối phương. Không thay sát thương, quỹ đạo hoặc gói vật lý.
- Chỉ hỏi cảm xúc nền khi client ở pha đang đấu. Trong pha chờ/tải/kết quả, server trả bộ cảm xúc rỗng cho yêu cầu đọc thay vì lỗi. Yêu cầu gửi cảm xúc vẫn được kiểm tra và chặn ngoài trận. Phản hồi từ yêu cầu đọc nền không bật popup lỗi lặp, để việc đồng bộ trận tiếp tục ngay cả khi server cũ chưa cập nhật.
- Nút popup dùng lại asset gốc, giữ tỷ lệ chiều cao/viền; phần giữa kéo theo chiều rộng chữ, căn giữa, vùng chạm và viền chọn theo đúng kích thước nút. Dùng chung cho thông báo, menu, xác nhận, startup và cảnh báo tài khoản.

Cập nhật toàn bộ gói server và JAR 7.5.1 đúng E72/E6. Không cần migration mới cho thay đổi trong trận. Nếu danh sách quản trị còn hiện dấu —, chạy bản vá `202611060001_gunny_admin_accounts_fix.sql` đã có trong gói; xem [hướng dẫn quản trị](UPDATE-7.5.0.md).

Không deploy Render hay chạy SQL thật trong quá trình sửa. Kiểm tra trên giả lập và server loopback; chưa kiểm tra trên Nokia thật.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
