# Gunny Java 7.6.9 — sửa cấu hình TCP Pi 5

- Gắn `Gunny-Server-URL: socket://gunnyjava.xubi.org:22122` vào cả JAD và manifest bên trong JAR.
- Gắn `Gunny-Web-URL: https://gunnyjava.xubi.org` để mở trang xác nhận email và tải game.
- Cài trực tiếp JAR hoặc qua JAD đều tự nhận đúng máy chủ, không còn báo thiếu `socket://TEN-MIEN:PORT`.
- Đổi tên bộ cài sang 7.6.9 để tránh trình duyệt và trình quản lý Java dùng lại cache 7.6.8.
- Không có SQL mới; dữ liệu Supabase giữ nguyên.

Sau khi chép server mới lên Pi, gỡ bản 7.6.8 trên điện thoại rồi cài 7.6.9.
