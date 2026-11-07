# Đồng bộ sảnh và Bạn bè — 7.6.7

Danh sách phòng ở sảnh tự tải lại theo lịch riêng mỗi khoảng 2 giây, không bị tác vụ presence/progress/socialpulse liên tục dời mốc tải. Đã bỏ phụ thuộc mốc lastPoll dùng chung.

Bạn bè có trạng thái tải riêng cho danh sách, tìm kiếm, thao tác và hồ sơ. Trạng thái mạng chung không làm danh sách trống nhấp nháy nữa. Dữ liệu khác tab/thế hệ yêu cầu bị bỏ qua; yêu cầu UI được chờ xử lý thay vì mất khi worker đang bận. Danh sách Bạn bè/lời mời tự kiểm tra lại khoảng 4 giây khi mở.

Tab Thêm bạn: dòng đầu “Tìm kiếm bạn bè” bên trái, nút cộng bên phải lấy từ vòng cộng xanh trong asset giỏ hàng gốc. Các dòng bên dưới hiển thị lời mời nhận được. Chọn dòng tìm hoặc nút cộng mở khung nhập dùng đúng frame, nút, ô nhập và font đã có; không vẽ template mới. Có thể gõ QWERTY trực tiếp hoặc chọn Nhập để dùng bộ gõ native như đăng nhập, rồi Tìm. Kết quả hiển thị trong bảng cũ cùng nút Kết bạn.

Gửi/đồng ý kết bạn hiện popup tiến trình và kết quả bằng NativePopup đang dùng. Phản hồi từ server là nguồn trạng thái. Chấp nhận lại quan hệ đã thành bạn trả về trạng thái đã kết bạn thay vì báo lỗi lời mời. Không đổi SQL.

Chọn Xem bạn bè mở ngay khung thông tin cũ và dòng Đang tải…, sau đó thay bằng nhân vật/trang bị/chỉ số. Đóng khung khi đang tải không tự mở lại khi dữ liệu cũ về. Mềm trái xem tooltip, mềm phải/0/X trở về như bản 7.6.4.

Cập nhật server và JAR/JAD 7.6.7. Không có SQL mới nếu đã chạy GUNNY-ALL.sql từ 7.6.2. Giữ cả sáu map cao 264 và Mộc Diệp Trấn phá được toàn bộ.

Kiểm tra bằng hai client: phòng xuất hiện/biến mất tự động, tìm người, gửi/nhận/đồng ý kết bạn, tự cập nhật danh sách và mở/đóng hồ sơ đang tải, hai độ phân giải. Chưa thử Nokia thật hoặc Supabase/Render thật.
