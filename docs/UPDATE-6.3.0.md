# Cập nhật Gunny Java 6.3.0

## Cần cập nhật cả server và hai bản game

1. Supabase → **SQL Editor**: chạy `supabase/migrations/202611040001_gunny_rewards.sql`. Đây là bản nâng cấp thêm bảng nhận thưởng và RPC, đồng thời cho phép ghi kết quả có 6 người. Không xóa hồ sơ hay xu cũ. Project mới chạy migration `202611030001_gunny.sql` trước.
2. Upload toàn bộ nội dung gói `gunny-online-main` mới lên repo GitHub của server, gồm `backend`, `public`, `supabase`, `Dockerfile`, `.dockerignore`, `render.yaml` và các file còn lại. Đợi Render deploy xong.
3. Kiểm tra `https://gunny-online-main.onrender.com/health`: có `"async":1` và `"seats":6`. Cài game 6.3.0 đúng máy E72 hoặc E6; hai người nên dùng cùng phiên bản.

## Email xác nhận

Supabase → Authentication → URL Configuration:

- **Site URL**: `https://gunny-online-main.onrender.com/auth/confirmed`
- **Redirect URLs**: thêm URL trên. Nếu server của bạn có tên khác, thay bằng tên đó.
- Không để `http://localhost:3000` làm Site URL của game đang dùng trên điện thoại.

Authentication → Email Templates → **Confirm signup**:

- Subject: **Xác nhận tài khoản Gunny Java của bạn**
- Dán nội dung `supabase/templates/confirm-signup-vi.html` vào phần nội dung thư rồi Save.
- Giữ nguyên biến `{{ .ConfirmationURL }}`. Link này xác nhận email tại Supabase trước khi trở về trang hướng dẫn đăng nhập.
- Bật Confirm email nếu muốn yêu cầu xác nhận. Tài khoản đã xác nhận được phát hiện từ lỗi “already registered” hoặc kết quả `identities` rỗng mà Supabase trả về; email chưa xác nhận có thể nhận lại thư khi đăng ký lại. Game không biến phản hồi bị che giấu thành thông báo đã tạo thêm tài khoản.

Tham khảo chính thức: [Email templates](https://supabase.com/docs/guides/auth/auth-email-templates), [Sign-up và email đã tồn tại](https://supabase.com/docs/reference/javascript/auth-signup).

Mẫu thư đã được chuẩn bị trong gói; file ở repo không tự thay đổi mẫu trên dashboard Supabase. Không gửi email thật khi chạy các phép thử.

## Chơi và giao diện

- **Đấu đơn**: ghép vào phòng đang đợi người thật; màn VS không có nút Bắt đầu. Khi hai người đã kết nối, game tự tải trận.
- **Tổ đội**: 3 ô Đỏ và 3 ô Xanh luôn mở. Mời người thật bằng mã phòng; đủ 6 người, mọi khách bấm Sẵn sàng thì tự vào trận. Chủ phòng được coi là sẵn sàng.
- **Tạo phòng**: mở hộp thiết lập tên và mật mã riêng, vào giao diện phòng cổ điển. Không gọi nút Đấu đơn. Khách bấm Sẵn sàng, chủ phòng xác nhận vào trận.
- Nút X đỏ thay cửa thoát trong phòng online. Thanh công cụ ở dưới danh sách phòng. Túi và Shop hoạt động; biểu tượng thú cưng chỉ hiển thị thông tin vì bản Java chưa có hệ thống thú cưng.
- Đăng nhập và đăng ký dùng khung nguồn, thêm Lưu mật khẩu. Chỉ khi đánh dấu mới lưu mật khẩu trong kho RMS riêng của ứng dụng trên máy. Bỏ dấu sẽ xóa bản đã lưu; đây không phải kho mật khẩu được mã hóa.
- Logo Gunny Java mới, tiêu đề kết quả và ảnh lật thẻ dùng tài nguyên đã chỉ định. Bảng thành tích hiển thị số liệu có thật trong bản Java; không tạo EXP/guild/danh vọng chưa có hệ thống xử lý.

## Đồng bộ lượt

Giao thức binary được giữ; client 6.3.0 thương lượng `X-Gunny-Client: 3` khi health có `async:1`. Các bản cũ vẫn có đường giao thức riêng, nhưng không vào phòng 6 người.

Máy đang có lượt mô phỏng bước đi, góc, lực, đạn và hiệu ứng ngay tại chỗ. Các bước nhập được nén thành đoạn ngắn và gửi theo số thứ tự. Server chạy lại luật vật lý, giới hạn thời gian/đạo cụ/quyền điều khiển, xác nhận HP, địa hình và kết quả; không nhận HP/xu/tọa độ tự khai từ client. Yêu cầu gửi lại không thực hiện lệnh lần hai. Máy còn lại nhận các trạng thái rồi làm mượt chuyển động.

Khi đạn và sát thương đã xử lý xong, server chờ các máy đang kết nối nhận trạng thái cuối lượt rồi mới đổi lượt và cấp đầy đủ thời gian cho người tiếp theo. Chờ hơn 30 giây sẽ hiện **Đợi xử lý dữ liệu...**. Máy không còn phản hồi sau 45 giây được loại khỏi điều kiện xác nhận để không giữ trận vô hạn. Kết nối POST bị chặn quá 60 giây bị đóng; game trở về sảnh, người dùng chọn Online để đăng nhập và yêu cầu mạng lại. Đánh thức server đang ngủ vẫn áp dụng ở bước vào Online.

## Xu và SQL

Thưởng trận hiện có vẫn là 300 xu thắng, 80 xu thua/hòa. Sau bảng kết quả, chọn một trong 9 thẻ để nhận thêm 50–200 xu. Server chọn mức thưởng. RPC `gunny_claim_card` kiểm tra người tham dự, khóa ví trong transaction và lưu lựa chọn theo `(match_id,user_id)`; gửi lại hay chọn thẻ khác vẫn trả thưởng đầu tiên. Chỉ `service_role` được chạy RPC, client không có khóa này. Thẻ offline chỉ cộng vào dữ liệu offline, một lần trong trận hiện tại.

Phòng/trận đang chơi vẫn ở RAM; nếu Render khởi động lại trước khi kết quả được lưu, trận đó không thể tiếp tục. Bảng `card_rewards` và các khoản đã ghi ở Supabase giữ nguyên. Chưa kiểm thử bản này trên Nokia vật lý hoặc Supabase/Render thật trong lượt sửa này.

## Web cho E72

Sau deploy, mở **https://gunny-online-main.onrender.com/download/**. Trang không cần JavaScript, CSS Grid, font hay tài nguyên từ ngoài; bố cục một cột cho màn 320px. Có JAD/JAR của cả E72/E6, PDF gốc, bản chữ và từng ảnh trong hướng dẫn, MBedTLS và ssl-v21.

Nếu Nokia chưa truy cập HTTPS được, tải từ máy tính rồi chép sang máy; hoặc giải nén `Gunny-download.zip`, chạy `start-http.ps1`, mở `http://IP-MAY-TINH:8088/` trên Nokia cùng Wi-Fi. Windows có thể cần cho phép Python trên mạng riêng. Trang static này chỉ chia sẻ thư mục tải game, không chia sẻ server/env. Bản HTML giữ nguyên trình tự hướng dẫn trong PDF bạn cung cấp.

## Kiểm tra

`verify.ps1` kiểm tra hợp đồng HTTP, tài khoản qua Supabase giả lập localhost, phòng/bản đồ, đoạn nhập lặp, quyền điều khiển, rào cuối lượt, 6 người, thưởng lặp, tải JAR theo Range/HEAD và chặn đường dẫn ngoài thư mục public. Kiểm tra MIDP riêng chạy 6 client thực trong MicroEmulator ở 320×240 và 640×480, gồm thao tác khi server chậm, các giao diện mới và trở về sảnh khi mất mạng.
