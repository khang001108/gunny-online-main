# Gunny Java 6.9.3 — sửa phòng và danh sách mời

Cập nhật cả game và server; `/health` có `"roomfix":1`. Không có SQL mới nếu đã chạy migration `202611040002_gunny_social_shared.sql`.

- Bỏ nút Mời và điểm chọn ẩn trên màn chờ Đấu đơn.
- Thiết lập phòng dùng khung 129, ô nhập 30, nút 158 của thư viện gốc; không dùng ảnh chụp người dùng làm panel. Bỏ checkbox mật mã và nhãn Đồng đội chèn lên Thi đấu. Nhập mật mã để khóa phòng, bỏ trống để mở.
- Panel bản đồ dùng parchment và các nhãn native trong SWF, không dùng ảnh tham chiếu của người dùng.
- Ô chờ thứ hai mở/đóng bằng chạm hoặc OK. Nút X ở đầu ô dành cho chủ phòng để đóng/mở ô trống, gỡ AI hoặc đưa đồng minh ra khỏi phòng. Biểu tượng cười gốc trên ô bản thân bật/tắt một biểu cảm cười; đồng bộ cho các máy trong phòng. Không có bộ emoji tự vẽ.
- Mời dùng danh sách hàng dọc theo bố cục PC, ảnh đầu nhân vật, nút Mời riêng và phân trang. Phím hướng chuyển tab, dòng và thanh phân trang; OK thao tác. Loại các điểm chọn vô hình/không có quyền khỏi điều hướng.
- Tổ đội là một phòng có hai phe 3v3. Vào phòng tự chọn phe còn chỗ; sẵn sàng và đủ sáu vị trí thì vào trận tại phòng đó. Không tìm một phòng tổ đội khác. Mời AI vào chỗ trống của phe hiện tại được ở cả đỏ và xanh. Đổi phe không tự sẵn sàng và chủ phòng không bị đóng dấu sẵn sàng do đổi vị trí.

Giữ nguyên hàng chờ phòng riêng, giới hạn 60 giây và H, đồng bộ từng đoạn, thẻ thưởng chung. Đã kiểm tra tài khoản thử và giả lập, chưa deploy hoặc thử điện thoại thật.
