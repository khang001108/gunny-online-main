# Raspberry Pi 5 và TCP socket — 7.6.8

Game hỗ trợ địa chỉ `socket://TEN-MIEN-HOAC-IP:22122`. Một kết nối TCP được giữ trong suốt phiên, dùng cho đăng nhập, sảnh, phòng và trận đấu. Khi socket hỏng hoặc ứng dụng bị ẩn quá lâu, lần kết nối tiếp theo mở lại socket; lệnh trận vẫn giữ số thứ tự chống áp dụng trùng.

Server mở hai cổng độc lập:

- `PORT=8080`: web quản trị, trang xác nhận email, tải game, health và API HTTP tương thích bản cũ.
- `TCP_PORT=22122`: giao thức game TCP cho bản 7.6.8.

TCP gateway giữ nguyên toàn bộ luật, phiên đăng nhập, giới hạn thao tác và dữ liệu Supabase của server hiện tại. IP thật của người chơi chỉ được chuyển vào API từ gateway loopback, nên client ngoài mạng không thể giả header giới hạn đăng nhập.

Client TCP không mở HTTP long-poll. Nó đồng bộ trạng thái trực tiếp trên socket đang giữ, giảm bắt tay HTTPS và tránh giới hạn raw TCP của Render. Web/email vẫn cần một tên miền HTTPS trỏ về Pi 5.

Thiết lập Pi nằm trong `deploy/pi5/README.md`. Mở/chuyển tiếp TCP 22122 trên router. Cổng web 80/443 đi qua Caddy về `127.0.0.1:8080`; không công khai thẳng cổng 8080.

Trong game, nhập `socket://ten-mien-game:22122` tại ô Máy chủ TCP / HTTPS. JAD/JAR 7.6.8 đã khai báo quyền socket. Địa chỉ trang tải dùng thuộc tính JAD `Gunny-Web-URL` nếu đóng gói riêng.

Luồng TCP thô không tự mã hóa email/mật khẩu. Nếu đưa cổng 22122 ra Internet công cộng, chỉ dùng trên mạng bạn kiểm soát hoặc đặt một lớp đường hầm/mã hóa tương thích ở trước nó. Web quản trị và khóa Supabase không đi qua cổng TCP.

Không có SQL mới. Supabase vẫn dùng `GUNNY-ALL.sql` hiện tại.
