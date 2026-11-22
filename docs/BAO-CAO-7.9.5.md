# Gunny Java 7.9.5 — cánh theo animation gốc

Đã xem4 video người dùng gửi ngày22/11. Ba clip phòng thử thể hiện cánh nhìn chính diện mở sang hai bên và đóng/mở quanh gốc cố định; clip chiến đấu thể hiện cánh nhìn nghiêng theo nhân vật. Các clip không phải hình ảnh của bản Java đang chạy.

## Sửa thực tế

- Bỏ cách vỗ giả4 khung và những ngôi sao chớp tự thêm.
- Export timeline `wing_show_*` trong6 SWF đang dùng:20 khung theo ma trận biến đổi gốc cho phòng thử, Túi, Shop, xem bạn bè và ảnh nhân vật phù hợp.
- Export timeline `wing_game_*`:19 khung bitmap gốc, tách hai lớp trong/ngoài theo depth của SWF. Dùng tư thế wrapper đứng gốc; ghép góc nghiêng theo địa hình và điểm lưng của nhân vật Java.
- Trong trận, lớp trong ở sau nhân vật/vũ khí; lớp ngoài sau bước vẽ vũ khí. Điểm gắn theo thân/lưng, không đi theo bàn tay ném vũ khí. Không sửa vật lý, hitbox hoặc TCP.
- Sprite được dựng trước khi build; render chỉ lấy khung trong atlas. Giải phóng cache góc không còn nhân vật sử dụng. Bỏ atlas cánh giả cũ trong hai bộ tài nguyên và bộ cài.
- Nguồn6 cánh giữ nguyên: wing027/006/007/005/030/002. Không tự vẽ lại hoặc đổi màu cánh để giống màu của một loại cánh khác trong video.

Script tái tạo: `client/tools/wing_timelines795.py`. Thông tin nguồn ở `client/VIP-ASSETS.json`. Script cần bộ Resource gốc, Pillow và decoder `audit_weapon_assets.py` hiện có tại máy build.

## Kiểm tra

- Compile/preverify và build hai JAR/JAD7.9.5.
- `Ui795Check`: JAR thật E72 với heap32MB và E6 với heap128MB; cả6 cánh ×5 góc nghiêng ×38 lần render, cùng các kiểm tra Túi/Shop/VIP/loading/thưởng/đăng nhập của bản trước. Xuất38 khung liên tiếp để kiểm tra chuyển động và xem lại ảnh giả lập. Không kết luận FPS trên Nokia vật lý.
- `verify.ps1` qua toàn bộ các kiểm tra server hiện có; log `backend/build/verify795.log`.
- Kiểm tra manifest, JAD, kích thước và hash bộ cài; atlas mới có đủ19/20 khung, không còn atlas giả cũ trong JAR. Hash tại `docs/BUILD-7.9.5.json`.

Không có SQL/env/secret mới so với7.9.4. Chưa deploy production. Chưa thử trực tiếp Nokia E72/E6/Asha. Các mục kinh tế còn thiếu từ yêu cầu cũ vẫn ghi tại báo cáo7.9.3; bản này chỉ sửa trình bày cánh.
