# Gunny Java 7.9.9

Sửa lại bố cục theo phản hồi người dùng; chưa deploy, không có SQL/env/secret mới.

- Chọn phó bản dùng lại `MapPicker.drawFrame` và bố cục chọn map PvP: bên trái là lưới hai thẻ dọc Kiến/Tà Thần; bên phải là mức độ, ảnh ngang/mô tả, nút Xác nhận gốc. Bỏ cửa sổ popup thay thế của7.9.8.
- Thẻ dùng ảnh `dungeon-picker-cover` / `tribal-picker-cover` và khung `kit-card` đang có; giữ tỷ lệ ảnh. Không tạo hình minh họa/icon mới.
- Ô thưởng bỏ chữ Quà ải. Khung ngoài dùng chính `kit-card` như ô chờ, ô đồ dùng `bag715-cell`. Giữ kích thước82×80 bằng ô chờ.
- Lưới3×3 chứa tối đa8 món; mỗi ô23×23, icon14×14 và tỷ lệ dùng glyph từ font game. Icon/chữ được giới hạn clip theo từng ô, không tràn sang ô khác hoặc ra ngoài khung.
- Giữ các bước chọn thẻ → độ khó → xác nhận, điều khiển phím/chạm và cảnh báo mất tiến trình đã có.

## Kiểm tra

- Compile/preverify và build hai JAR/JAD7.9.9.
- `UiTribal799Check`: hai bộ cài thật E72 320×240 heap32MB và E6 640×480 heap128MB; kiểm tra thẻ dọc, nút mức độ bên phải, xác nhận/cảnh báo tiến trình, các ải và gói TCP, native animation/cánh/PvP. Đã xem ảnh trường hợp khó có đủ8 món thưởng và bảng chọn map trên cả hai kích thước.
- `verify.ps1`: kiểm tra server hiện có; log tại `backend/build/verify799.log`.
- Kiểm tra manifest/JAD, lớp client trong bộ cài và kích thước; hash trong `docs/BUILD-7.9.9.json`.

Chưa thử Nokia vật lý; không thay luật chiến đấu/phần thưởng so với7.9.8. Không xóa dữ liệu người chơi.
