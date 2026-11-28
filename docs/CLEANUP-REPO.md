# Dọn repo hiện tại

Phạm vi: `D:/gunny/repo/gunny-online-main`, ngày 28/11/2026. Dung lượng không tính thư mục `.git`.

- Trước: khoảng 849.4 MB, 5,810 file.
- Sau: khoảng 170.3 MB, 1,053 file.
- Đã loại khoảng 679.0 MB (4,759 file cũ), tương đương gần 80% dung lượng làm việc.

## Đã dọn

- `client/build`, `backend/build`, Python cache: JAR cũ, bản vá thử, baseline, class, ảnh/video xuất để kiểm tra và log có thể sinh lại.
- 1.322 ảnh export cũ trong `assets1` / `assets2`: `wing795-game`, `wing812-packed`, `wing1-free`, `winggroup-free`, `wingbal`, `hf39-npc`, vòng VIP 144/216 cũ và nền loading cũ. Đây là các nhóm đã bị script build hiện tại loại trừ.
- Báo cáo/hotfix và hash build của các phiên bản cũ, ảnh xem trước cũ, snapshot trạng thái Git cũ và báo cáo dọn ngoài repo đã lỗi thời.
- Source client không được tham chiếu hoặc đóng gói: `EmbeddedTls`, `TlsTrust`, `LongPollClient`; bản `GunnyServer.java` trùng và đã cũ trong client; ghi chú HF44 đã lỗi thời.
- Script `verify_tribal797.py` chỉ xác minh bộ cài 7.9.7 không còn trong repo.
- PDF HTTPS và hai gói TLS Symbian cũ. Server chỉ cho tải bốn JAR/JAD hiện tại; trang hướng dẫn TCP HTML được cập nhật phiên bản.

## Giữ lại

Source game hiện tại, toàn bộ test hồi quy, tài nguyên thực sự dùng trong bộ cài, ảnh nguồn và công cụ tái tạo, cấu hình cánh, Wing Studio, hai bộ cài 7.9.22 và SHA256, công cụ deploy, SQL cài mới/cập nhật và chuỗi migration. Giữ `.git`, file đính kèm người dùng và dữ liệu/cấu hình đang có. Không dọn các thư mục ngoài repo như `D:/gunny/Resource` hay `D:/gunny/GunnyNokia`.

Đã thêm README gốc, sửa hướng dẫn cài đặt và liên kết tài liệu, bổ sung ignore cho các export đã nghỉ dùng.

## Xác minh sau khi dọn

- Biên dịch backend Java 17 thành công.
- Build/preverify sạch cả E72 và E6 thành công sau khi xóa cache cũ.
- So sánh từng entry của JAR build mới với JAR ban đầu: nội dung giống hoàn toàn. Giữ lại chính xác bộ cài ban đầu để tránh thay đổi hash chỉ vì timestamp ZIP.
- `WeaponCombatCheck`: 407 kiểm tra qua.
- `UiWeaponCombatCheck`: 3.541 kiểm tra qua trên giả lập E72 320×240 với heap 32 MB.
- 10 kiểm tra HTTP: bốn bộ cài JAR/JAD hiện tại tải được; bản cũ/TLS trả 404; trang tải và hướng dẫn TCP trả 200 đúng phiên bản.
- CRC, dung lượng JAD và SHA256 của bộ cài khớp. Các nhóm tài nguyên bị xóa không có trong JAR đang dùng.
- `Gameplay771Check` vẫn dừng ở kiểm tra chỉ số nhân vật cấp 1; đã xác minh test nguyên bản trước thay đổi cũng lỗi tương tự. Phần kiểm tra allowlist download phía trước lỗi này đã qua; HTTP được kiểm tra riêng như trên. Các test cũ đang lỗi khác được ghi ở `WEAPON-COMBAT-E72.md`.

Cache sinh ra trong quá trình xác minh đã được xóa lần nữa. Khi chạy build/test, các thư mục `build` sẽ được tạo lại.
