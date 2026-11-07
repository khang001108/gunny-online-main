# Gunny Java Online 7.6.6

## Cài mới hoặc cập nhật

7.6.6 tăng chiều cao cả 6 map từ 220 lên 264 (+20%), giữ nguyên chiều ngang 480 và kích thước nhân vật. Đồng bộ ảnh địa hình, va chạm, mặt nạ vùng cứng và bản đồ nhỏ. Mộc Diệp Trấn trở lại phá được toàn bộ, gồm vòng sắt. [Hướng dẫn](docs/UPDATE-7.6.6.md).

Cập nhật toàn bộ server (backend/assets) và JAR/JAD 7.6.6 trên tất cả máy cùng chơi. Không có SQL mới nếu đã cập nhật GUNNY-ALL.sql từ 7.6.2. Cài mới dùng duy nhất [GUNNY-ALL.sql](supabase/GUNNY-ALL.sql).

## Level và phần thưởng

Dùng đúng bitmap gốc `asset.LevelIcon.Level_1` đến `Level_70`, giữ số, họa tiết và màu từng nhóm cấp. Bắt đầu 01; 01→02 cần 50 EXP, 02→03 cần 100, mỗi cấp tiếp theo tăng 50 EXP. EXP cũ giữ nguyên; chỉ đổi mốc hiển thị từ 0 sang 01. Cấp cao nhất theo bộ asset là 70, số EXP tổng vẫn được lưu.

Thắng +300 EXP, thua +100 EXP. Người thua có sát thương nhận nửa xu thưởng trận (250 xu đấu đơn/phòng riêng; 500 xu tổ đội) và mở một thẻ thưởng. Không gây sát thương thì chỉ nhận EXP. Gửi lại kết quả/thẻ không cộng trùng.

Level hiện ở túi đồ, phòng chờ, trong trận và kết quả. Túi đồ hiện EXP, hạng theo tổng trận thắng, số trận thắng, lực chiến tính cả trang bị đang mặc. Ảnh POW dùng đúng ngoại hình đang mặc. Xem [thay đổi 7.6.6](docs/UPDATE-7.6.6.md).

## Web và tài khoản

- `/admin/`: quản lý tài khoản, lịch xóa, thông báo và cấp tiền/xu. Không có mật khẩu mặc định.
- `/download/`: tải JAD/JAR E72/E6 và hướng dẫn HTTPS. Giữ thư mục public vì Docker đóng gói các file này.
- Xác nhận email mở trang chúc mừng một lần, rồi quay lại game đăng nhập. Cấu hình Supabase Site URL/Redirect URL bằng địa chỉ Render thật, không dùng localhost.

## Kiểm tra và chạy thử

Cần JDK 17 trở lên. Chạy `./verify.ps1` để kiểm tra API, giao thức, hồ sơ, phần thưởng, tải file và xác nhận email. `./backend/run-local.ps1 -TestAccounts` chạy server với tài khoản trong RAM tại localhost:8080; không dùng chế độ tài khoản thử trên Render.

Đã kiểm tra giả lập hai độ phân giải và API server. Chưa thử trên Nokia vật lý hoặc chạy SQL trên Supabase của bạn. Phòng đang chơi ở RAM; khởi động lại Render mất trận chưa lưu. Hồ sơ và phần thưởng đã ghi được lưu trong Supabase.
