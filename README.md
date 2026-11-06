# Gunny Java Online 7.5.8

## Cài mới hoặc cập nhật

1. Supabase → SQL Editor: chạy duy nhất [GUNNY-ALL.sql](supabase/GUNNY-ALL.sql). File dùng được cho project mới hoặc đã có dữ liệu, gộp đầy đủ shop, trang bị, bản vá equip, quản trị, thông báo, EXP và thưởng người thua. Không chạy thêm SQL cũ sau file này.
2. Cập nhật toàn bộ repo server lên Render. Giữ nguyên biến môi trường Supabase và GUNNY_ADMIN_PASSWORD (tối thiểu 16 ký tự). Không đưa khóa vào mã nguồn.
3. Cài JAR/JAD 7.5.8 đúng E72 hoặc E6. Người chơi cùng trận nên dùng cùng phiên bản. [Hướng dẫn](docs/CAI-DAT.md).

SQL giữ tài khoản, đồ, số dư, EXP, số trận thắng và lịch xóa đang có. Chỉ các hồ sơ chưa có EXP mới bắt đầu từ 0 EXP. SQL chạy trong một giao dịch, có thể chạy lại; không có lệnh xóa/reset tài khoản. Bản gộp chỉ giữ định nghĩa RPC cuối cùng, có cả sửa toán tử trang bị.

**Không cần tải từng migration, UPDATE-7.5.3-ALL hay UPDATE-7.5.7.** Các file trong `supabase/history/` và `supabase/migrations/` là lịch sử nguồn để tra cứu hoặc dành cho quy trình CLI đã có; không phải danh sách cần chạy thủ công. `supabase/templates/` chỉ là mẫu email, không thay SQL.

Nếu pg_cron đã được bật, file gộp thiết lập một lịch dọn tài khoản mỗi phút. Nếu chưa bật, backend vẫn bảo trì trong lúc đang chạy; có thể bật pg_cron trong Supabase rồi chạy lại cùng GUNNY-ALL.sql.

## Level và phần thưởng

Dùng đúng bitmap gốc `asset.LevelIcon.Level_1` đến `Level_70`, giữ số, họa tiết và màu từng nhóm cấp. Bắt đầu 01; 01→02 cần 50 EXP, 02→03 cần 100, mỗi cấp tiếp theo tăng 50 EXP. EXP cũ giữ nguyên; chỉ đổi mốc hiển thị từ 0 sang 01. Cấp cao nhất theo bộ asset là 70, số EXP tổng vẫn được lưu.

Thắng +300 EXP, thua +100 EXP. Người thua có sát thương nhận nửa xu thưởng trận (250 xu đấu đơn/phòng riêng; 500 xu tổ đội) và mở một thẻ thưởng. Không gây sát thương thì chỉ nhận EXP. Gửi lại kết quả/thẻ không cộng trùng.

Level hiện ở túi đồ, phòng chờ, trong trận và kết quả. Túi đồ hiện EXP, hạng theo tổng trận thắng, số trận thắng, lực chiến tính cả trang bị đang mặc. Ảnh POW dùng đúng ngoại hình đang mặc. Xem [thay đổi 7.5.8](docs/UPDATE-7.5.8.md).

## Web và tài khoản

- `/admin/`: quản lý tài khoản, lịch xóa, thông báo và cấp tiền/xu. Không có mật khẩu mặc định.
- `/download/`: tải JAD/JAR E72/E6 và hướng dẫn HTTPS. Giữ thư mục public vì Docker đóng gói các file này.
- Xác nhận email mở trang chúc mừng một lần, rồi quay lại game đăng nhập. Cấu hình Supabase Site URL/Redirect URL bằng địa chỉ Render thật, không dùng localhost.

## Kiểm tra và chạy thử

Cần JDK 17 trở lên. Chạy `./verify.ps1` để kiểm tra API, giao thức, hồ sơ, phần thưởng, tải file và xác nhận email. `./backend/run-local.ps1 -TestAccounts` chạy server với tài khoản trong RAM tại localhost:8080; không dùng chế độ tài khoản thử trên Render.

Đã kiểm tra giả lập hai độ phân giải và API server. Chưa thử trên Nokia vật lý hoặc chạy SQL trên Supabase của bạn. Phòng đang chơi ở RAM; khởi động lại Render mất trận chưa lưu. Hồ sơ và phần thưởng đã ghi được lưu trong Supabase.
