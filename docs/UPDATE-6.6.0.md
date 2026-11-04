# Gunny Java 6.6.0

Cập nhật cả server và game của những người chơi cùng nhau. `/health` có `"chunks":1`. Không cần SQL mới.

Server gửi các đoạn theo số thứ tự: di chuyển được chốt khi lấy lực; hiệu ứng POW riêng; từng loạt đạn được chốt khi tất cả viên trong loạt chạm/nổ hoặc bay hụt. X3 cùng lúc là một loạt; các phát nối tiếp được gửi riêng. Máy nhận phát các khung hình đã xử lý ở nhịp game, không ngoại suy vị trí theo gói mạng. Server vẫn tự tính toàn bộ va chạm, sát thương, đất và kết quả. Thao tác của người bắn tiếp tục chạy tại máy; các lệnh di chuyển và nhả lực được xử lý đúng thứ tự, không áp dụng lệnh trùng.

Server giữ các đoạn chưa xem; gói lặp không phát hai lần. Chỉ khi mọi người xem xong đoạn cuối mới mở đủ thời gian lượt kế. Game cũ dùng đường tương thích trước đây. Chưa kiểm tra Nokia vật lý.
