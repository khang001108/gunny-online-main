# Shop, thông tin đồ và vòng đời ứng dụng 7.5.2

- Các nút thường dùng dùng chung NativeUi lấy từ nút asset gốc, chung ảnh/khung chọn/chữ/vùng chạm. Shop sửa hàng nút trên 50×20 và nhóm bên dưới 38×20; bỏ ảnh nút 44px gây lệch và chồng viền. Đổi xu, mũi tên, giỏ hàng và nút đóng được rà theo đúng kích thước ảnh.
- Tooltip dùng đúng bitmap `asset.core.tip.GoodsTipBg` từ `20cc885211cebf45f578.swf`, giữ alpha nền và viền bo; source có manifest. Hiện cạnh ô vật phẩm, không che ô đang xem. Tên tím, thông số cam, sở hữu xanh, dữ liệu dùng ItemRules thực. Viền vàng ô đang xem nhấp nháy nhẹ.
- Mềm trái xem/đóng thông tin trong shop và túi. Túi giữ OK lần đầu xem, lần thứ hai trang bị/tháo. Thay thế cơ chế mềm trái chuyển hai bên bằng điều hướng trái/phải. Từ tab Thời trang bấm trái đi thẳng vào vùng túi, kể cả khi túi trống hoặc chỉ có ít món; ô trống không trang bị nhầm món trước đó.
- Bỏ hẳn menu tạm dừng vẽ xanh cũ, các nhánh phím/chạm tương ứng và hành vi hideNotify đẩy sang mode cũ. Offline tạm dừng bằng popup chung; online ẩn/mở app giữ ngữ cảnh trận và không hiện menu offline. Cài đặt trong menu offline giữ trận tạm dừng cho tới khi đóng.
- Tách bộ đếm hiệu ứng khỏi vật lý trận. Hạ cánh, buff và động tác kết thúc được giảm trong lúc chờ đồng bộ đoạn hành động hoặc xác nhận cuối lượt, cả client và server. Không đổi quỹ đạo, sát thương, tiền hay kết quả.
- Bỏ ảnh popup bị kéo giãn và ảnh nút shop cũ không còn dùng khỏi JAR mới. Giữ các gói cài cũ/bản sao lưu ngoài bản phát hành để đối chiếu; không dọn thư mục backup của người dùng.

Cập nhật JAR 7.5.2 đúng E72/E6 và toàn bộ server. Không có SQL mới cho các sửa này. Nếu chưa sửa danh sách quản trị, bản vá SQL trước vẫn nằm trong gói.

Kiểm tra giả lập 320×240/640×480, hai người chơi, bộ kiểm tra máy chủ và đóng gói. Chưa deploy Render, chạy SQL thật hoặc thử trên Nokia thật.
