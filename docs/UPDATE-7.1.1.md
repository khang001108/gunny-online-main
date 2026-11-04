# Cập nhật Gunny Java 7.1.1

Bản này sửa bảng Thiết lập phòng trước khi tạo, theo bố cục tham khảo và ghép từ asset game gốc. Không cắt bảng từ ảnh tham khảo.

- Nền khung gỗ, tiêu đề, khối tên/mật mã và nút Đồng ý vàng được sắp lại; dùng khung, input, checkbox và button gốc.
- Nhãn Loại phòng và khối lập phương dấu hỏi lấy từ `roomlist.swf` gốc.
- Có asset **Thi đấu** (bitmap 43) và **Thám hiểm** (bitmap 68) trong `Source Flash/FlashSV1/ui/vietnam/swf/roomlist.swf`.
- Có chữ **Tổ đội** trên asset `asset.ddtroomlist.pvpBtn.formAteam` (bitmap 1, thư viện PC `a1e3ccec8d2963f1885d.swf`). Tách đúng phần chữ của asset; không lấy ảnh tham khảo.
- Tạo phòng thường chỉ hiện Thi đấu. Vào Tổ đội để tạo chỉ hiện Tổ đội, không còn chữ Thi đấu nằm phía dưới/chồng lên. Thám hiểm chỉ là asset tìm được, không thêm chế độ chơi chưa có vào game.
- Ô khóa mật mã theo hình có thể bật/tắt bằng chạm hoặc phím OK. Bật khóa rồi chưa nhập mật mã sẽ yêu cầu nhập trước khi tạo. Mật mã hiển thị dấu sao.
- Bốn phím hướng chọn được tên, mật mã, ô khóa, Đồng ý và nút đóng. Nút X đóng hộp thoại mà không tạo phòng.

Nguồn và cách ghép nằm ở `source-assets/create711/manifest.json` và `create_assets.py` trong project phát triển.

## Cài đặt

Cài cả JAR và JAD 7.1.1 đúng loại máy. Không cần SQL mới. Giao thức và luật trận vẫn là `items:2`, tương thích server 7.1.0. Gói server 7.1.1 kèm web và file tải mới; cập nhật gói này nếu muốn web trên server cung cấp bản mới.

Các sửa về đạo cụ, popup Mời, bản đồ, tàng hình/đạn băng và camera của 7.1.0 vẫn giữ nguyên; xem [hướng dẫn 7.1.0](UPDATE-7.1.0.md).

Đã kiểm tra giao diện 320×240 và 640×480 qua MicroEmulator; thử tạo phòng riêng có mật mã, từ chối mật mã sai, vào phòng bằng mật mã đúng và tạo Tổ đội 3v3 trên server localhost. Chưa thử trên Nokia vật lý.
