# Gunny Java 6.9.0

Cập nhật cả server và game; `/health` có `"waitbot":1`. Không có SQL mới nếu đã chạy migration `202611040002_gunny_social_shared.sql`.

- Tìm trận tối đa 60 giây, thời gian do server tính. Hết giờ: Đấu đơn tự rời hàng chờ về danh sách phòng, giữ đăng nhập; phòng riêng/Tổ đội dừng tìm, giữ nguyên phòng và thành viên.
- Trong thời gian tìm, bấm H (nhận cả h/H) để đồng ý đấu bot. Điện thoại cảm ứng có thể chạm dòng thời gian/H ở cuối phòng. Hiển thị số người đã xác nhận.
- Một người tìm một mình bấm H sẽ đấu bot ngay. Đội hai người phải có cả hai xác nhận; bấm H nhiều lần chỉ tính một phiếu. Đội ba người cũng cần đủ ba phiếu. AI đồng minh không cần bỏ phiếu.
- Bot đối phương có số lượng bằng đội đang tìm; server chạy AI và kiểm tra đạn, sát thương, đất như các trận online khác. Giữ đồng minh, đồng bộ cuối lượt, kết quả và thẻ thưởng chung.
- Hết giờ, thay đổi đội, bỏ sẵn sàng, đổi map hoặc hủy tìm sẽ xóa xác nhận cũ. Không có phiếu H nào được mang sang lần tìm tiếp theo.

Đã kiểm tra bằng giả lập và tài khoản thử trong RAM; chưa triển khai lên Render/Supabase thật hoặc Nokia vật lý.
