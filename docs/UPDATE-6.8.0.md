# Gunny Java 6.8.0

Cập nhật cả server và game trên các máy chơi cùng nhau. `/health` có `"matchmaking":1`. Không có SQL mới nếu đã chạy migration `202611040002_gunny_social_shared.sql` của 6.7.0.

- Phòng riêng có một người bấm Bắt đầu sẽ tìm một phòng riêng cũng một người, vào trận 1v1. Phòng có hai thành viên cùng đội (người hoặc AI đồng minh) tìm phòng có cùng số thành viên, giữ nguyên phe, vào 2v2. Đồng minh là người thật phải sẵn sàng trước khi chủ phòng tìm trận.
- Trong phòng riêng, chủ phòng bấm `#` hoặc chạm dòng “nhập mã đối phương” để nhập mã phòng và mật mã nếu có. Hai phòng cần cùng loại, cùng số người, đồng minh đã sẵn sàng. Không đổi mật mã thành công cụ vượt khóa phòng.
- Đấu đơn chỉ ghép với Đấu đơn khác, luôn 1v1. Nút Mời có thể mời một người đang ở sảnh làm đối thủ; không thêm AI/đồng minh vào Đấu đơn.
- Tổ đội mở bảng tên phòng/mật mã giống Tạo phòng trước khi tạo. Mọi người vào cùng phe; đủ ba người cùng đội, sẵn sàng rồi chủ phòng tìm một đội ba người khác để đấu 3v3. Không tự bắt đầu khi mới đủ người trong một đội.
- Khách bấm Sẵn sàng sẽ có dấu sẵn sàng bằng ảnh gốc của phòng offline trên ô nhân vật. Ô đồng minh hiển thị đúng người còn lại khi bạn là khách.
- Thêm hoặc rời đội, đổi AI, đổi bản đồ hoặc bỏ sẵn sàng sẽ hủy trạng thái tìm trận để tránh ghép một đội đã thay đổi.

Giữ đồng bộ từng đoạn và bộ thẻ chung từ các bản trước. Các bộ cài cũ đi theo đường tương thích cũ; để dùng cách ghép mới, mọi người cần cài 6.8.0. Không tự deploy hay sửa Supabase thật.
