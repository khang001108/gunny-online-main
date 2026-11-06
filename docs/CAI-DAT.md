# Máy bay, POW và bàn phím Nokia — 7.5.5

Cập nhật cả server và JAR/JAD 7.5.5 cho các máy chơi cùng nhau. Không có SQL mới. Luồng email xác nhận một lần của 7.5.4 được giữ nguyên.

- Máy xem phát lại không còn vẽ vệt bom/vũ khí phía sau máy bay hoặc đạn băng. Kiểm tra nằm ngay ở hàm tạo vệt dùng chung cho mọi đường phát lại.
- W của E72 (mã phím số 1 khi không giữ Fn) dùng buff x3 như w/W. Chỉ áp dụng mã số trên các dòng Nokia QWERTY được nhận diện; bàn phím số thông thường giữ điều khiển cũ.
- Gói hoạt ảnh dài, gồm POW, được chia nhỏ khoảng tối đa 16 KiB khi các khung hình riêng lẻ vừa giới hạn. Từng khung hình, thứ tự và địa hình đầu đoạn được giữ đủ. Một khung hình địa hình lớn riêng lẻ vẫn được giữ nguyên để tránh cắt hỏng giao thức.
- Nếu đồng bộ trong trận gặp lỗi I/O, thử lại một lần và chuyển sang polling một kết nối. Lệnh đồng bộ có sequence ID nên không dùng POW/bắn trùng khi server đã nhận gói trước đó. Không tự gửi lại giao dịch mua đồ hoặc cộng tiền.
- Chưa tái hiện trực tiếp mã native Error-5 trên Nokia thật. Đã giả lập mất phản hồi sau khi server nhận lệnh, kiểm tra không mất trận hoặc dùng POW hai lần. Cần thử lại trên E72 để xác nhận lỗi phần mạng/TLS của máy đã hết.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
