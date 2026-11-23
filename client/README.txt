BẢN FIX KHUNG NỀN XẾP HẠNG - KHÔNG PHẢI CHỈ DỜI BẢNG TRẮNG

Mã nguồn: src/Ranking793.java (từ bản vá gần nhất)

- Dời chính ảnh nền khung viền vàng settings743-popup lên 6 px:
    c.img(frame, 6, 6) -> c.img(frame, 6, 0)
- Thu mép dưới bảng trắng từ Y=225 về Y=220 để bảng không chạm/đè lên viền vàng bên dưới.
- Giữ header Y=22; các cột, 6 hàng dữ liệu, căn giữa thông báo, hitbox không đổi.
- Không chỉnh sửa PNG hoặc giao thức game.

Ứng dụng: giải nén, thay src/Ranking793.java trong client/src, build lại.
Nếu đã sửa Ranking793.java khác bản cũ, áp dụng file Ranking793.patch thủ công để không mất thay đổi.

Chưa xác minh bằng KEmulator thực tế.
