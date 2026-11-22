# Tool cập nhật Docker Gunny trên Pi 5

Chạy trên chính Pi đang chạy container `gunny-online`:

```bash
sudo bash /home/khazg/gunny/gunny-online-main/deploy/cap-nhat-gunny.sh
```

Nhập mật khẩu hệ thống khi sudo hỏi. Menu có:

1. **Kiểm tra**: xem container/image hiện tại và kiểm tra SQL 7.7.0 và 7.8.0 bằng API chỉ đọc.
2. **Cập nhật lên 7.9.13**: yêu cầu bạn xác nhận đã tự chạy SQL, xác minh schema,
   chạy thử image rồi đổi container.
3. **Quay lại bản trước**: khôi phục container đã lưu ở lần cập nhật gần nhất.
0. **Thoát**.

Tool không chạy SQL, không chuyển project Supabase, không xoá hay reset tài khoản.
Nếu Supabase đang ở schema 7.7.0, chạy một lần
[supabase/UPDATE-CURRENT.sql](../supabase/UPDATE-CURRENT.sql)
trong SQL Editor của project hiện tại. Khi tool hỏi, nhập `DA CHAY SQL`.
Ngay cả khi có xác nhận, tool vẫn chặn cập nhật nếu không xác minh được migration.

## Tool thực hiện những gì

- Đọc cấu hình trực tiếp từ container cũ, không yêu cầu nhập lại khoá Supabase/admin.
- Dùng image `gunny-online:7.9.13` đã build. Nếu chưa có, build từ thư mục repo.
- Chạy thử image bằng tài khoản RAM trên các cổng localhost ngẫu nhiên; không đưa
  khoá Supabase vào container thử.
- Giữ biến môi trường, cổng publish, network và restart policy đang dùng; đổi đường
  dẫn assets sang `/app/assets1` của image mới.
- Lưu cấu hình riêng tư, dừng và đổi tên container cũ thành bản backup. Backup
  được tắt tự khởi động để tránh tranh cổng sau khi Pi reboot.
- Khởi động bản mới, kiểm tra HTTP, TCP giữ kết nối, TCP events/luật lực và hai JAD.
- Tự khôi phục bản cũ nếu khởi động/kiểm tra bản mới thất bại. Không tự xoá backup.

Đổi container sẽ ngắt trận đang nằm trong RAM. Chạy khi người chơi đã thoát trận.
Dữ liệu đã lưu trên Supabase giữ nguyên. Quay lại bản Docker cũ không hoàn tác SQL.

Cấu hình backup có secret nên được ghi với quyền `0600` vào
`deploy/backups/updater/`; không chia sẻ thư mục này. Nếu tool bị tắt đột ngột
giữa lúc đổi container, chạy lại rồi chọn **Quay lại bản trước**.

Tool dùng Docker CLI trên máy, không thay đổi cấu hình stack Portainer cũ.
Container `gunny-online` hiện đang chạy độc lập nên được hỗ trợ. Với container
thực sự thuộc Compose/stack, tool sẽ dừng và yêu cầu cập nhật qua stack để tránh
ghi đè cấu hình quản lý.

## Lệnh trực tiếp

Trong thư mục `gunny-online-main`:

```bash
sudo bash deploy/cap-nhat-gunny.sh check
sudo bash deploy/cap-nhat-gunny.sh update --sql-confirmed
sudo bash deploy/cap-nhat-gunny.sh rollback
```

Chỉ dùng `--sql-confirmed` sau khi chính bạn đã chạy SQL. Muốn build lại image từ
mã hiện tại, thêm `--build` vào lệnh update. Tool chỉ cần Python 3 và Docker CLI;
không cần cài pip package hay Docker Compose.

Bản tool hiện tại chuẩn bị cập nhật **7.9.13**, kiểm tra `actors/live/growth`,
`expansion/forge/dungeon` và hai JAD 789. Tool không tự chạy SQL. Với Xubi, đẩy
Git rồi Deploy trực tiếp, không cần chạy tool này hay tạo ZIP Pi.
