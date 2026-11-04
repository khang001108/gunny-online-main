# Gunny Java 6.7.0 — đồng minh, bạn bè và thẻ thưởng chung

## Cập nhật

1. Trong Supabase SQL Editor, **tạo truy vấn mới** và chạy `supabase/migrations/202611040002_gunny_social_shared.sql` (file nguồn `server/social-670.sql`). Không ghi đè `202611040001_gunny_rewards.sql`. Migration mới thêm ID số, bảng quan hệ bạn bè và khóa bộ thẻ chung; giữ tài khoản và xu đang có.
2. Upload toàn bộ nội dung gói server mới vào repo GitHub, đợi Render deploy. `/health` có `"social":1`, `"chunks":1`.
3. Mọi máy chơi cùng nhau cài bản E72/E6 6.7.0. Nếu tạo project mới, chạy lần lượt cả ba migration theo tên tăng dần.

## Kết quả và thẻ

Máy đã xem xong đoạn cuối chờ các máy còn lại xác nhận rồi server mới lưu kết quả và tiền thưởng trận. Mọi người bấm OK ở kết quả; khi đủ người đồng ý, cùng mở một bộ 9 thẻ. Mỗi người lấy một thẻ khác nhau. Thẻ đã mở hiện tên người nhận và số xu trên các máy; tranh cùng một thẻ chỉ một người nhận, người kia chọn thẻ khác. Gửi lại lệnh không nhận thêm xu. Các máy chờ nhau nhận xong trước khi về danh sách phòng. Người mất kết nối bị loại khỏi nhóm chờ sau thời hạn 45 giây, tránh khóa vĩnh viễn kết quả.

## Phòng riêng và bạn bè

Tạo phòng riêng có ô đồng minh ở cạnh nhân vật. Mời bạn bè hoặc người ở Sảnh vào cùng đội; đối thủ vào phòng ở phe bên kia. Tab AI cho phép chọn một AI vào ô đồng minh; đổi AI được trước khi vào trận. Những ô không có người/AI không tự tham chiến.

Nút bắt tay mở Bạn bè / Sảnh / AI / Yêu cầu. ID số hiển thị trên tiêu đề; nút ID nhập mã để gửi yêu cầu kết bạn. Người nhận vào tab Yêu cầu để xác nhận; quan hệ lưu tại Supabase và dùng được sau khi server khởi động lại. Sảnh liệt kê người online chưa vào phòng. Lời mời phòng có nút vào đội hoặc từ chối. Tab Bạn bè chứa các bạn đã xác nhận, có trạng thái online/offline. Mời phòng là trạng thái phiên, không lưu sau khi server khởi động lại.

Giao diện tái sử dụng khung 129, ô nền và nút 158 từ thư viện SWF gốc cùng nút thoát/ảnh nhân vật đang dùng; không lấy UI Android làm ảnh nền. Danh sách sáu thẻ, đầu nhân vật thật hoặc AI. Điều khiển bốn hướng; `#` đổi tab, `I` nhập ID, OK thao tác, phím phải đóng. Chạm trực tiếp cũng được.

Đã thử bằng tài khoản trong RAM và giả lập. Chưa áp dụng migration lên Supabase thật, chưa deploy và chưa thử trên Nokia vật lý.
