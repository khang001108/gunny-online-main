# Gunny Java 6.4.0

**Cập nhật cả gói server và game** vì quy tắc tiêu hao POW đã thay đổi. Không cần SQL mới nếu đã chạy `202611040001_gunny_rewards.sql`. Upload toàn bộ gói `gunny-online-main` lên repo, đợi Render deploy; `/health` có `"pow":2`, sau đó cài JAR/JAD 6.4.0 đúng E72 hoặc E6.

- Nhà Online dùng ảnh viền sáng trong suốt bạn cung cấp khi được chọn. Nhãn Online được dựng cùng font, cỡ chữ, màu chữ và viền với nhãn Sảnh.
- Sảnh dùng đủ bốn phím theo vị trí các công trình; danh sách phòng cũng điều hướng theo hàng/cột và vị trí nút. Cửa hàng nhận cả lên/xuống và trái/phải.
- Bảng đăng nhập/đăng ký dùng frame 129, ô nhập 30, nút 158, checkbox 145/146 từ thư viện SWF gốc `520b122a888c5076a2c0`. Sau ô mật khẩu/nhập lại là **Lưu mật khẩu**, rồi mới đến nút đăng nhập/đăng ký. OK bật/tắt checkbox và giữ nguyên focus; trái/phải trên checkbox cũng bật/tắt được.
- **Q**: +2 đạn; **W**: x3; **E**: +50%; **R**: hồi HP; **T**: bay. Nhận cả chữ hoa/thường. Phím 1–5 không còn dùng buff; phím 5 vẫn lấy lực và bắn như OK/Space trên bàn phím số.
- **Phím mềm trái**: bật xem camera; dùng cả bốn hướng để xem bản đồ, không di chuyển hay đổi góc nhân vật. Bấm mềm trái lần nữa để trở về nhân vật. Có thể xem khi tới lượt đối phương, kể cả lúc họ đang lấy lực. Khi chính bạn đang lấy lực, thả phím bắn trước khi chuyển camera. Xem camera không dừng đồng hồ trận.
- Thanh POW dùng texture gốc nhưng tích lũy màu từ dưới lên. **B** khi đầy sẽ bật POW và trừ toàn bộ 100 tích lũy ngay, không chờ thả đạn. B lần nữa không tắt POW hoặc hoàn lại tích lũy. Bạn vẫn chọn góc/lực rồi bắn đòn POW. Bay hoặc bỏ lượt có thể hủy hiệu lực đã bật, không hoàn tích lũy.
- Giữ các sửa kết nối, thông báo rời trận, đồng bộ cuối lượt và thưởng từ các bản trước. Camera chỉ thay khung nhìn, không thay tọa độ nhân vật hoặc giới hạn vật lý của đạn.

Trang `/download/` trong gói server cũng cung cấp bộ cài mới. Mẫu email và dữ liệu Supabase giữ nguyên. Kiểm tra bằng giả lập không thay thế thử trên Nokia vật lý.
