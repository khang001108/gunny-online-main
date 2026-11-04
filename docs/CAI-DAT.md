# Gunny Java 6.5.0 — phát bắn online theo lượt

**Cập nhật cả server và game E72/E6.** Upload toàn bộ gói `gunny-online-main` lên GitHub, đợi Render deploy, kiểm tra `/health` có `"replay":1`, rồi cài game 6.5.0. Không cần SQL mới nếu đã chạy `202611040001_gunny_rewards.sql`.

- Máy người bắn lấy lực, thả đạn, va chạm và phá đất bằng cùng vật lý với offline. Không chờ mạng để vẽ đường đạn hoặc hố nổ.
- Server kiểm tra thao tác và xử lý phát bắn ở nền. Trong lúc xử lý, máy còn lại chờ; giao thức mới không gửi các vị trí đạn dở dang để kéo đạn theo mạng.
- Khi xử lý xong, server gửi trạng thái trước phát bắn cùng kết quả cuối. Máy nhận phát lại cả phát bắn bằng vật lý offline: đạn chạm đất và đất nổ cùng khung hình. Các gói lặp không đặt lại đạn.
- Kết quả HP, hố đất và phần thưởng vẫn do server quyết định. Client đối chiếu trạng thái cuối sau khi hiệu ứng hoàn tất; không được tự cộng thưởng.
- Chỉ sau khi các máy nhận và phát lại xong, server mới mở lượt kế tiếp và bắt đầu đủ thời gian lượt. Nếu quá 30 giây chưa hoàn tất xử lý/nhận dữ liệu, game hiện “Đợi xử lý dữ liệu...”.
- POW, nhiều đạn, x3 và bay dùng cùng luồng hoàn tất phát bắn. Giữ camera, phím Q W E R T / B và sửa đăng nhập từ 6.4.0.

Giao thức được thương lượng bằng `/health` và header `X-Gunny-Replay: 1`. Server giữ đường giao thức cũ cho các bộ cài trước; cần cả hai người dùng bản mới để có phát lại đầy đủ. Không tự deploy hay thay đổi cấu hình Supabase.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
