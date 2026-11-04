# Gunny Java 6.9.2

Thay nút hành động trong phòng online bằng đúng hai sprite gốc trong `gunny-main/Source Flash/FlashSV1/ui/vietnam/swf/room.swf`:

- Chủ phòng: `room.view.roomView.startBtnAsset` (180), chữ gốc “Bắt đầu”.
- Khách: `room.view.roomView.prepBtnAsset` (175), chữ gốc “Chuẩn bị”, dùng cho thao tác sẵn sàng/bỏ sẵn sàng.

Giữ nguyên hình và chữ trên sprite, xuất đủ bốn trạng thái gốc, không vẽ lại nhãn nút. Dùng trong phòng riêng và tổ đội; dấu xác nhận trên ô nhân vật giữ nguyên. Nút bắt đầu offline cũng dùng cùng sprite gốc. Trạng thái “Đang tìm”, “Đã sẵn” hoặc “Về trận” hiện riêng bên dưới khi cần, không đè lên ảnh.

Cài game E72/E6 6.9.2. Server 6.9.x vẫn chơi được; gói server đi kèm cập nhật trang tải. Không có SQL mới. Manifest nguồn và SHA-256 lưu tại `source-assets/room/actions/manifest.json`.
