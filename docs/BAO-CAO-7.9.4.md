# Gunny Java 7.9.4

Đã sửa source và build JAR/JAD E72/E6. Chưa deploy hoặc sửa cấu hình production.

- Chữ Xếp hạng ở đảo dùng cùng phông Arial Bold, cỡ13, màu kem và viền nâu với bộ nhãn Sảnh/Tiệm rèn hiện tại; lưu chung `home-label-6.png`, không dùng kiểu chữ nhỏ của bảng thông tin.
- Mọi toast qua `PhoneCanvas.message` và thông báo đến lượt được vẽ một lần bằng khung đen `notice754-bg` cùng chữ vàng của trận đấu. Bỏ các cách vẽ riêng ở Túi, Shop, Tiệm rèn và Phó bản. Toast ở lớp cuối, không chìm sau bảng thông tin/Shop. Bảng xác nhận OK/Hủy vẫn là cửa sổ xác nhận.
- Xếp hạng: STT → huy hiệu cấp → ảnh đầu nhân vật → tên → icon vũ khí → dame → thắng. Server gửi ngoại hình thật; client dựng ảnh đầu bằng sprite cutin hiện có, cùng cách với danh sách bạn bè. Metadata mới nằm sau phần gói tin cũ, giữ khả năng đọc của client trước đó. Tài khoản cheat bị loại cả dòng lẫn metadata ảnh.
- Thiết lập có Đăng xuất, dùng được bằng phím/chạm, có xác nhận. Hoàn tất đăng xuất trở lại bảng đăng nhập.
- Chưa đăng nhập chỉ vẽ nền đảo, bảng đăng nhập và cửa thoát. Ẩn và chặn cả điều hướng/chạm vào Túi, Shop, Bạn bè và Thiết lập. Đóng bảng đăng nhập hoặc phím quay lại sẽ hỏi thoát game.
- Sửa thêm kiểm tra màn hình khi xác nhận đổi đá: đúng mode8 của Túi, thay cho mode7 tạo nhân vật.

## Kiểm tra

- Compile/preverify Java ME và build hai JAR/JAD7.9.4.
- `Ui794Check`: chạy hai bộ cài thật,320×240 heap32MB và640×480 heap128MB; kiểm tra login chỉ có cửa thoát, không chọn được nút ẩn, settings/đăng xuất/hủy, xác nhận đổi đá, icon gốc, các màn Shop/Túi/phó bản/loading/thưởng và xoay màn hình. Đã xem ảnh thực từ giả lập.
- `Ranking794Check`: metadata ngoại hình từ server, thứ tự byte tương thích, loại tài khoản cheat.
- `verify.ps1`: các kiểm tra server hiện có. Kết quả đầy đủ ở `backend/build/verify794.log`.
- Manifest/JAD đúng7.9.4, đường dẫn và kích thước đúng; hash tại `docs/BUILD-7.9.4.json`.

Không có SQL mới so với7.9.3, không có env/secret mới. Chưa thử máy Nokia thật. Những mục còn thiếu từ yêu cầu cũ, gồm cách chia xác suất thẻ VIP trong túi phó bản, được ghi tại `BAO-CAO-7.9.3.md`; bản này không tự thay tỷ lệ đó.
