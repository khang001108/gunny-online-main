# Gunny Java 7.9.18 HF7

Patch tích lũy HF1–HF6. HF7 chỉ sửa client, không đổi backend, SQL hoặc tỉ lệ thưởng. Nếu backend đã chạy HF6 thì chỉ cần build lại JAR; health vẫn là dungeonHF6: 1.

- Phòng phó bản: focus ô quà thưởng (66), OK hoặc chạm mở bảng riêng, không mở chọn map. Có thể di chuyển từ ô 3 sang phải hoặc ô 2 xuống dưới.
- Popup dùng hệ tọa độ 320x240 chung cho E72/E6. Lưới 3 cột khi có 5–6 món, 4 cột khi có 7–8 món; icon tối đa 40x40 giữ tỉ lệ, chữ % dùng font 14px thay font nhỏ của khung xem nhanh. Màu nền tối, viền vàng chọn từng món.
- Phím hướng chọn món, softkey trái/OK mở hoặc đóng tooltip. Tooltip dùng StoneShop.drawInfo, ItemTooltip.draw và ItemTooltip.goods như túi đồ. Softkey phải đóng tooltip trước, sau đó đóng popup, giữ focus ô quà thưởng. Chạm nút tương ứng cũng được. Popup tự đóng khi đổi phòng, độ khó/ải hoặc vào trận.
- Khung xem nhanh và popup dùng chung DungeonRewardPreview: giữ nguyên toàn bộ tỉ lệ hiển thị HF6. Không thay bảng drop server. Icon lớn cache tối đa 8, giải phóng khi vào trận.

Kiểm thử đã chạy: javac và DungeonRewardPreviewCheck (807 kiểm tra, đủ 5 mã ải và 3 độ khó, thứ tự phần thưởng, tỉ lệ, bounds, touch và điều hướng). ReturnRoomClientCheck bổ sung kiểm thử tích hợp; chưa chạy do thiếu SDK Java ME/MicroEmulator. Chưa build JAR, chưa xem trực tiếp trên E72/E6.
