# Cập nhật Gunny Java 7.1.2

## Thanh công cụ trong phòng

- Bỏ dòng thông tin cố định dưới cùng và phần mô tả đạo cụ cũ phía trên vị trí Quan chiến.
- Khu vực Quan chiến dùng khung chọn bản đồ, với ảnh ALL – Bản đồ ngẫu nhiên tiếng Việt gốc và góc bánh răng. Chạm hoặc chọn bằng phím hướng rồi OK để mở chọn map; quyền đổi map Online vẫn thuộc chủ phòng.
- Thanh dưới phòng gồm Shop, Túi, Bạn bè, Hướng dẫn, cửa về sảnh. Dùng đúng ảnh thường và ảnh viền vàng từ thanh công cụ sảnh Online; giữ kích thước biểu tượng, giảm khoảng cách tâm từ 58 xuống 39 pixel để vừa phần trái của phòng.
- Chạm xuống để xem tooltip và viền vàng; kéo qua biểu tượng khác để xem lựa chọn mới, thả trên biểu tượng để mở. Thả ngoài thanh thì không mở. Có thể dùng bốn phím hướng và OK.
- Cửa trong phòng Online rời phòng về sảnh nhưng giữ đăng nhập. Trong phòng Offline, cửa trở về sảnh Offline.
- Shop/Túi mở từ phòng sẽ quay lại đúng phòng bằng phím Trở về hoặc nút cảm ứng dưới cùng. Quản lý bạn bè và hướng dẫn dùng lại hành động ở sảnh Online.

Thông tin chi phí đạo cụ chỉ xuất hiện dạng tooltip khi đang chọn đạo cụ, không chiếm thanh dưới cùng. Nguồn hình: thư viện PC `01abe2636b0f2ab4da71.swf`, các bitmap toolbar 25/28/29/31/32/42/49/63 và biểu tượng Hướng dẫn gốc mà sảnh Online đang dùng. Không cắt từ ảnh tham khảo.

## Cập nhật

Cài cặp JAR/JAD 7.1.2 đúng E72 hoặc E6. Không có SQL mới. Vẫn tương thích server từ 7.1.0 (`items:2`). Gói server này kèm web và file tải 7.1.2; cập nhật gói nếu muốn web trên máy chủ cung cấp bản mới.

Các phần Thiết lập phòng giữ sửa ở [7.1.1](UPDATE-7.1.1.md); các luật đạo cụ và đồng bộ giữ như [7.1.0](UPDATE-7.1.0.md).

Đã kiểm tra giao diện và thao tác ở 320×240 / 640×480 trên MicroEmulator, gồm chạm giữ/kéo/hủy, Shop/Túi quay lại phòng, Bạn bè, Hướng dẫn, chọn map và cửa về sảnh Online. Chưa thử trên Nokia vật lý.
