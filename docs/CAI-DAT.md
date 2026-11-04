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


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
