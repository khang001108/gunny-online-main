# Shop, thông tin đồ và vòng đời ứng dụng 7.5.2

- Các nút thường dùng dùng chung NativeUi lấy từ nút asset gốc, chung ảnh/khung chọn/chữ/vùng chạm. Shop sửa hàng nút trên 50×20 và nhóm bên dưới 38×20; bỏ ảnh nút 44px gây lệch và chồng viền. Đổi xu, mũi tên, giỏ hàng và nút đóng được rà theo đúng kích thước ảnh.
- Tooltip dùng đúng bitmap `asset.core.tip.GoodsTipBg` từ `20cc885211cebf45f578.swf`, giữ alpha nền và viền bo; source có manifest. Hiện cạnh ô vật phẩm, không che ô đang xem. Tên tím, thông số cam, sở hữu xanh, dữ liệu dùng ItemRules thực. Viền vàng ô đang xem nhấp nháy nhẹ.
- Mềm trái xem/đóng thông tin trong shop và túi. Túi giữ OK lần đầu xem, lần thứ hai trang bị/tháo. Thay thế cơ chế mềm trái chuyển hai bên bằng điều hướng trái/phải. Từ tab Thời trang bấm trái đi thẳng vào vùng túi, kể cả khi túi trống hoặc chỉ có ít món; ô trống không trang bị nhầm món trước đó.
- Bỏ hẳn menu tạm dừng vẽ xanh cũ, các nhánh phím/chạm tương ứng và hành vi hideNotify đẩy sang mode cũ. Offline tạm dừng bằng popup chung; online ẩn/mở app giữ ngữ cảnh trận và không hiện menu offline. Cài đặt trong menu offline giữ trận tạm dừng cho tới khi đóng.
- Tách bộ đếm hiệu ứng khỏi vật lý trận. Hạ cánh, buff và động tác kết thúc được giảm trong lúc chờ đồng bộ đoạn hành động hoặc xác nhận cuối lượt, cả client và server. Không đổi quỹ đạo, sát thương, tiền hay kết quả.
- Bỏ ảnh popup bị kéo giãn và ảnh nút shop cũ không còn dùng khỏi JAR mới. Giữ các gói cài cũ/bản sao lưu ngoài bản phát hành để đối chiếu; không dọn thư mục backup của người dùng.

Cập nhật JAR 7.5.2 đúng E72/E6 và toàn bộ server. Không có SQL mới cho các sửa này. Nếu chưa sửa danh sách quản trị, bản vá SQL trước vẫn nằm trong gói.

Kiểm tra giả lập 320×240/640×480, hai người chơi, bộ kiểm tra máy chủ và đóng gói. Chưa deploy Render, chạy SQL thật hoặc thử trên Nokia thật.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
