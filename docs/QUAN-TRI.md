# Quản trị tài khoản và thông báo 7.5.0

Web riêng tại `https://TEN-SERVER.onrender.com/admin/`, dùng cùng backend Gunny và Supabase. Không có mật khẩu mặc định trong bản production. Không có khoá Supabase trong HTML/JS/JAR.

## Cài đặt

1. Giữ bản sao dự phòng. Chạy đủ các migration cũ trước, rồi chạy `supabase/migrations/202611050006_gunny_admin.sql` trong Supabase SQL Editor.
2. Bật pg_cron trong Database > Extensions, rồi chạy `supabase/admin-cron.sql`. Job mỗi phút xoá tài khoản đến hạn ngay tại DB, kể cả Render đang ngủ. Không cần mở máy tính cá nhân. Nếu bỏ bước này, backend chỉ xử lý lịch khi Render đang chạy, hoặc tại lần đăng nhập tiếp theo.
3. Cập nhật toàn bộ gói server. Trên Render thêm secret `GUNNY_ADMIN_PASSWORD` dài ít nhất 16, tối đa 256 ký tự. Tự chọn mật khẩu; đừng đưa mật khẩu lên GitHub. Deploy rồi mở `/admin/` qua HTTPS. Bỏ biến này để tắt trang quản trị và chức năng quản trị trong login; cron đã cài vẫn chạy, muốn dừng cần huỷ lịch xoá hoặc tắt job cron. Cần SQL trước khi bật biến.
4. Cài JAR 7.5.0 E72/E6 để nhận thông báo asset và liên hệ khiếu nại. Server giữ giao thức cũ, nhưng JAR cũ không hiển thị các thông báo mới.

Cookie đăng nhập HttpOnly, SameSite Strict, Secure, hết hạn tuyệt đối sau 8 giờ. Đăng xuất hoặc server khởi động lại sẽ huỷ phiên quản trị. Thao tác xoá ngay cần nhập `XOA`. Mật khẩu không xuất hiện trong URL, localStorage hay source client. Giới hạn đăng nhập 8 lần/phút/IP; có kiểm tra Origin cho mọi thay đổi. Server không trả password/hash game lên web.

## Tài khoản

- Danh sách lấy từ auth.users, bao gồm cả tài khoản đã đăng ký nhưng chưa xác nhận email/chọn nhân vật. Tìm tên, email hoặc public ID, tối đa 100 bản ghi mỗi trang.
- Giờ đăng nhập gần nhất tách riêng giờ kết nối gần nhất. Heartbeat ghi theo đợt mỗi 30 giây ở luồng riêng; số online dùng kết nối 60 giây gần đây. Việc tải trang không chạy truy vấn DB trên vòng tick trận đấu.
- Offline lâu nhất là khoảng mất kết nối đã ghi nhận và khoảng offline hiện tại. Lịch sử đầy đủ trước khi cài bản này không thể khôi phục; giá trị chưa biết hiển thị Chưa ghi nhận. Lần đăng nhập cũ khởi tạo từ auth last_sign_in_at; kết nối gần nhất và khoảng offline bắt đầu ghi từ bản này.
- Chọn ngày giờ Việt Nam (UTC+7), lý do, liên hệ khiếu nại. Chọn lại ngày để gia hạn, hoặc huỷ lịch. Mặc định tự huỷ lịch khi đăng nhập trước hạn; checkbox được đánh dấu sẵn cho lịch mới, có thể bỏ chọn nếu cần giữ lịch cố định. Thông báo lúc login giải thích rõ kết quả, số ngày còn lại làm tròn lên.
- Muốn giữ hết ngày 10/12/2026: chọn 10/12/2026 23:59. Tới hạn thì xoá vĩnh viễn auth.users và hồ sơ phụ thuộc qua cascade. Đã xoá không thể đăng nhập để cứu lại. Tài khoản đang online cũng bị thu hồi phiên trong tối đa một chu kỳ kiểm tra sau khi DB xoá.
- Có log thao tác trong gunny_admin_audit. Bảng quản trị/thông báo/RPC chỉ service_role truy cập.

## Thông báo

Chọn tất cả hoặc một tài khoản bằng public ID/UUID, đặt tiêu đề/nội dung và thời gian bắt đầu/kết thúc. Gửi trong 3 ngày = khoảng thời gian phát 3 ngày. Hiện mỗi lần đăng nhập trong khoảng [bắt đầu, kết thúc), không lưu “đã đọc vĩnh viễn”. Dừng/xoá sẽ ngừng phát cho lần login tiếp theo. Thông báo đang mở trên máy người chơi không bị thu hồi từ xa. Tối đa 20 thông báo có lịch phát trùng khoảng thời gian (hệ thống từ chối thêm khi vượt mức), thêm một cảnh báo lịch xoá nếu có.

Popup dùng NativePopup và asset gốc của game. Nội dung dài có Tiếp theo; Xác nhận ở dưới giữa tại trang cuối. Khiếu nại là link HTTP(S) hoặc số điện thoại, mở bằng phím mềm trái hoặc chạm dòng liên hệ; máy sẽ hỏi quyền nền tảng nếu cần. Không gửi SMS tự động.

Đổi nhanh: 2.000 / 5.000 / 10.000 xu = 20 / 50 / 100 tiền ở tỷ lệ 100:1 hiện tại. Nhập tay, kiểm tra số dư và tính chống đổi trùng online được giữ nguyên.

## Kiểm tra và giới hạn

`Admin750Check` dùng tài khoản giả lập trên loopback: chặn truy cập chưa login, sai mật khẩu, CSRF, thông báo riêng/chung, lặp mỗi login, xoá theo lịch và thu hồi phiên. `Admin750Check preview` chạy bản xem thử ở localhost:8765 với dữ liệu giả lập và mật khẩu thử trong mã test; không được đưa chế độ thử này lên Render. Production chạy GunnyServer.

SQL/cron chưa được chạy trên Supabase thật. Cần thực hiện hai bước SQL và secret ở trên sau khi xem lại bản sao lưu. Chưa deploy Render và chưa kiểm tra trên điện thoại Nokia vật lý.


## Vá danh sách tài khoản 06/11/2026

Nếu trang chỉ hiện dấu —, chưa phải kết quả có 0 tài khoản. Bản SQL đầu có alias a trùng biến record a trong PL/pgSQL. Chạy `supabase/migrations/202611060001_gunny_admin_accounts_fix.sql` trong SQL Editor rồi làm mới trang. File chỉ thay hàm và nạp lại schema, không đặt lại hoặc xoá dữ liệu tài khoản. Không cần đổi JAR. Bản web cập nhật cũng hiện lỗi tải dữ liệu cố định thay vì giấu lỗi lúc mở trang.


## Cập nhật lựa chọn tự huỷ lịch xoá

Chạy `supabase/migrations/202611060002_gunny_admin_auto_cancel.sql` để đặt mặc định mới và chuyển các lịch đang chờ sang tự huỷ khi đăng nhập trước hạn. File không xoá tài khoản, không thay đổi ngày hẹn. Sau đó cập nhật web/server và tải lại trang. Tài khoản đã quá hạn vẫn bị xoá; đăng nhập sau hạn không cứu lại tài khoản. Tỷ lệ giữ 100 xu = 1 tiền, các nút nhanh là 2.000 / 5.000 / 10.000 xu.


## VIP / cánh 7.9.0

Chọn tài khoản → chọn Thẻ VIP 1–12 hoặc Full VIP → nhập số lượng → Cấp vật phẩm. Thẻ thường cộng 100–1.200 EXP; Full VIP đưa lên 21.200 EXP / VIP 12. Full VIP không có trong Shop. API client không được phép cấp vật phẩm.

Khi bật cheat từ tắt sang bật, tài khoản được cộng 999 thẻ mỗi loại, gồm cả Full VIP (13 loại). Bấm lưu lại khi đã bật không cộng thêm. Tắt rồi bật lại sẽ cộng thêm 999, giới hạn 999.999 mỗi loại. Đây là đồ dùng thử có thể tiêu hao bình thường. Game tự cập nhật túi qua TCP khi đang ở sảnh/phòng.

Cấp vật phẩm có mã giao dịch; gửi lại cùng yêu cầu không cộng trùng. EXP, mua và trang bị cánh lưu trên Supabase. Phải chạy migration VIP 202611220001 trước khi dùng chức năng này. Không đổi mật khẩu Admin hoặc secret Supabase trong đợt cập nhật.
