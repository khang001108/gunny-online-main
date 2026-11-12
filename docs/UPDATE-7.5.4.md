# Sửa túi đồ, máy bay và giao diện trận — 7.5.4

Cập nhật toàn bộ gói server lên repo đang dùng rồi deploy Render. Cài JAR/JAD 7.5.4 đúng E72 hoặc E6. Cả hai máy nên cùng phiên bản.

Không có SQL mới trong bản này. Nếu đã chạy đầy đủ SQL 7.5.3 thì không cần chạy lại. Nếu còn lỗi thiếu `gunny_equip`, dùng `supabase/UPDATE-7.5.3-ALL.sql` theo hướng dẫn 7.5.3.

- Túi: P = Trang bị, L = Thời trang; hai tab dọc không nhận con trỏ hoặc viền chọn vàng. Các phím nổi vẫn bấm được bằng cảm ứng.
- Máy bay bỏ va chạm nhân vật, cho đáp xuống mặt đất dù có người đứng cạnh hoặc cùng vị trí; không gây sát thương. Bay ra ngoài bản đồ hoặc xuống vực vẫn không có chỗ đáp.
- Đồng hồ hiển thị hiệu ứng độc lập với các gói đồng bộ cũ: ánh sáng đáp máy bay, buff, vụ nổ và số dame tự hết hạn, không làm thay đổi HP/vật lý/lượt.
- Đạo cụ Z/X/C dùng xong mất biểu tượng. Buff/đạo cụ thiếu thể lực dùng ảnh gốc chuyển xám, máy bay hồi lượt cũng xám. Không có bộ bitmap xám riêng trong các thư viện đã rà; bản xám giữ hình và alpha của chính asset gốc.
- Lượt bot offline ẩn đồng hồ. Tủ thuốc dùng dao mổ gốc `blastout341.swf / bullet341 / bitmap28` cho cầm ném và đường đạn.
- Báo thao tác trong trận dùng nền mờ `asset.core.tip.GoodsTipBg` và chữ vàng. Popup chung dùng khung/nút asset, tiêu đề và nội dung căn giữa, tự co theo chữ, xuống dòng; nội dung rất dài cuộn bằng lên/xuống. Hai lựa chọn nằm cạnh nhau.
- Thiếu vũ khí báo “Không có trang bị vũ khí không thể bắt đầu” trước thông báo chờ đồng minh, kiểm tra ở JAR và server khi bắt đầu/chuẩn bị.

Đã kiểm tra giả lập 320×240, 640×480, hai khách online, gói nhị phân lặp lại, luật máy bay và bộ kiểm tra server. Chưa kiểm thử trên điện thoại Nokia thật.

## Email xác nhận một lần

Giữ mẫu email mặc định trong Supabase; không cần sửa mục Confirm sign up hay dán HTML. Người chơi mở email, bấm liên kết xác nhận một lần; Supabase xác nhận email rồi chuyển tới trang **“Chúc mừng bạn đăng ký thành công!”**. Trang không có form đăng nhập hoặc nút xác nhận tiếp; người chơi đóng trang và đăng nhập trong game.

- Authentication → URL Configuration → Site URL: `https://gunny-online-main.onrender.com`.
- Redirect URLs: thêm `https://gunny-online-main.onrender.com/auth/confirmed`.
- Giữ xác nhận email được bật. Cấu hình SMTP đã có tiếp tục dùng để gửi thư; chỉ bỏ yêu cầu tuỳ biến mẫu email.
- Cập nhật server/web và dùng thư gửi mới sau khi sửa URL; thư cũ có localhost không tự đổi được.
- `/` và `/auth/confirmed` nhận kết quả xác nhận tự động. `/account/` vẫn là trang tài khoản riêng, `/admin/` vẫn là quản trị.
- Liên kết TokenHash của mẫu cũ vẫn được xử lý tự động. Liên kết hết hạn/không hợp lệ không báo đăng ký thành công.
- Không có SQL mới và không cần cài lại JAR cho thay đổi email này.
