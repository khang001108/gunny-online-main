# Máy bay, POW và bàn phím Nokia — 7.5.5

Cập nhật cả server và JAR/JAD 7.5.5 cho các máy chơi cùng nhau. Không có SQL mới. Luồng email xác nhận một lần của 7.5.4 được giữ nguyên.

- Máy xem phát lại không còn vẽ vệt bom/vũ khí phía sau máy bay hoặc đạn băng. Kiểm tra nằm ngay ở hàm tạo vệt dùng chung cho mọi đường phát lại.
- W của E72 (mã phím số 1 khi không giữ Fn) dùng buff x3 như w/W. Chỉ áp dụng mã số trên các dòng Nokia QWERTY được nhận diện; bàn phím số thông thường giữ điều khiển cũ.
- Gói hoạt ảnh dài, gồm POW, được chia nhỏ khoảng tối đa 16 KiB khi các khung hình riêng lẻ vừa giới hạn. Từng khung hình, thứ tự và địa hình đầu đoạn được giữ đủ. Một khung hình địa hình lớn riêng lẻ vẫn được giữ nguyên để tránh cắt hỏng giao thức.
- Nếu đồng bộ trong trận gặp lỗi I/O, thử lại một lần và chuyển sang polling một kết nối. Lệnh đồng bộ có sequence ID nên không dùng POW/bắn trùng khi server đã nhận gói trước đó. Không tự gửi lại giao dịch mua đồ hoặc cộng tiền.
- Chưa tái hiện trực tiếp mã native Error-5 trên Nokia thật. Đã giả lập mất phản hồi sau khi server nhận lệnh, kiểm tra không mất trận hoặc dùng POW hai lần. Cần thử lại trên E72 để xác nhận lỗi phần mạng/TLS của máy đã hết.
