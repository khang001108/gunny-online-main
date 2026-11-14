# Gunny Java Online 7.8.1

- Chữ Thoát ở sảnh chỉ hiện khi chọn. Nút mở hộp thoại xác nhận dùng chung.
- Phòng PvP hiện đủ bản đồ và các nút sau khi rời phó bản.
- Nút cửa ở danh sách phòng về đảo và giữ đăng nhập. Nhà Online/Offline đổi chế độ.
- Shop, Túi, Tiệm rèn và Phó bản dùng dữ liệu máy chủ khi đã đăng nhập.
- Tia đạn Sấm Sét lớn hơn. Hai ải Huyệt Ma Kiến dùng nhạc 1120 và 1121.

## Cập nhật trên Pi 5

Bản 7.8.1 dùng schema 7.8.0. Nếu Supabase vẫn ở 7.7.0, áp dụng một lần
[SQL nâng cấp gộp](../supabase/UPDATE-7.7.0-TO-7.8.0.sql). Nếu schema 7.8.0
đã đầy đủ, cập nhật bộ cài và image trực tiếp.

Build image bằng `docker build -t gunny-online:7.8.1 .` rồi đặt
`GUNNY_IMAGE=gunny-online:7.8.1` theo [hướng dẫn Portainer](../deploy/portainer/README.md).
Trang tải phục vụ JAR/JAD 781 cho E72/C3 và E6 sau khi cập nhật stack.
