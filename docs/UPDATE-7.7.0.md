# Gunny Java 7.7.0

## Lấy lực và đồng hồ

Giữ phím bắn dừng đồng hồ lượt. Lực tăng 1 đơn vị mỗi tick (trước đây là 2),
từ 0 đến 100 rồi giảm về 0 đúng một lần. Nếu vẫn giữ phím khi về 0, lượt kết thúc
mà không bắn. Nhấn lặp không khởi động lại chu kỳ để kéo dài lượt vô hạn.
Client và server dùng cùng `Battle.java`; luật cũng áp dụng khi chơi offline.

Trong trận online, server không tính các frame giữ lực vào thời gian di chuyển/
ngắm còn lại. Quá lực kết thúc ở hàng rào đồng bộ lượt để các máy chuyển lượt cùng nhau.

## Gán phím

Vào Thiết lập → Gán phím điều khiển, chọn chức năng → Gán phím, rồi nhấn phím/nút.
Hỗ trợ hướng, giữ lực/bắn, camera, trở về, buff, ba ô đạo cụ và phím tắt.
Cấu hình lưu RMS riêng trên thiết bị và có nút Mặc định; không sửa hồ sơ người chơi.
Phím trùng với một gán tùy chỉnh khác sẽ bị từ chối.

HUD đạo cụ, tooltip và hướng dẫn lấy nhãn từ cấu hình phím hiện tại. Trên RG40XX H
hoặc tay cầm PS1, giả lập cần chuyển nút tay cầm thành sự kiện phím Java ME. JAR
không cài driver tay cầm của hệ điều hành.

## TCP cho toàn bộ giao tiếp game

Đăng nhập/đăng ký, phòng, chiến đấu, cửa hàng, túi, bạn bè, thông báo và kết quả
dùng TCP. Client có một socket lệnh và một socket nhận thông báo thay đổi từ server;
nhờ đó thao tác không chờ sau yêu cầu nhận thông báo. Không còn HTTP/HTTPS polling
trong MIDlet. URL game HTTPS lưu cũ được đổi sang `socket://HOST:22122`.

TCP server gọi trực tiếp handler trong tiến trình, không chuyển yêu cầu qua HTTP
localhost. Khi chạy production, `/api` và `/wait` HTTP trả 410; chỉ các fixture
kiểm thử cũ dùng chúng. Web admin, trang tải và kết nối lưu trữ Supabase vẫn dùng HTTPS.

Client/server 7.7.0 cần cập nhật cùng nhau vì luật lực và kênh thông báo đã đổi.

## Tài khoản ảo và quản trị

Email chỉ là tên đăng nhập có dạng email, được phép dùng địa chỉ không có hộp thư
thật. Đăng ký dùng Admin Auth ở backend với `email_confirm: true`, không gửi thư.
Khóa dịch vụ luôn nằm ở server.

Migration chuyển chính sách của tài khoản cũ ngay trên cùng UUID: giữ email đăng
nhập, hash mật khẩu và mọi cột của profile. Nhân vật, tiền, vật phẩm, cấp độ, quan
hệ bạn bè và dữ liệu liên kết tiếp tục dùng UUID cũ. Không tạo tài khoản thay thế.

Admin có chính sách không hoạt động: mặc định 90 ngày, chỉnh được 1–3650 ngày,
hạn riêng từng tài khoản và gia hạn thêm ngày. Gửi lại cùng yêu cầu gia hạn không
cộng ngày hai lần. Đăng nhập/chơi làm mới mốc hoạt động.

Tự xoá do không hoạt động **tắt mặc định**. Khi admin bật, mọi tài khoản cũ được
cấp một chu kỳ đầy đủ từ thời điểm bật. Các lịch xoá thủ công đã có vẫn độc lập.
Chỉ khi tính năng được bật và hạn đã đến, maintenance mới xoá tài khoản theo chính sách.

## Cập nhật project hiện tại

Project cần giữ: `mnquwasxacifkktmckdv`. Không dùng project `izydmfvsgaruvhoqkcnt`.
Không đặt mật khẩu database hoặc token vào repository.

1. Trong Supabase SQL Editor của project đang chạy, tự chạy duy nhất
   [migration 7.7.0](../supabase/migrations/202611120001_virtual_accounts_retention.sql).
   Không chạy lại các SQL lịch sử. Migration có transaction và bước đối chiếu
   email/hash mật khẩu/toàn bộ profile; nếu dữ liệu đó thay đổi, transaction bị hủy.
2. Sau khi SQL thành công, xác nhận lại để cập nhật container sang image
   `gunny-online:7.7.0`. Image này không tự chạy SQL khi khởi động.
3. Cập nhật cả JAD/JAR 7.7.0. Giữ tên MIDlet/Vendor để dữ liệu RMS trên máy còn đọc được.
4. Kiểm tra đăng nhập cũ, vật phẩm và số dư; kiểm tra admin hiện chính sách 90 ngày
   đang tắt trước khi quyết định bật.

Chạy migration khi không có giao dịch mua đồ/phần thưởng để bước đối chiếu không
báo thay đổi đồng thời. File `GUNNY-ALL.sql` dành cho cài mới; project đang có dữ
liệu dùng migration riêng ở bước 1.

Theo yêu cầu chủ máy, lần chuẩn bị này **không chạy migration và không cập nhật
container production**. Bộ cài mới cần server 7.7.0 nên chỉ phát hành cho người
chơi sau khi hoàn thành SQL và cập nhật server.

## Kiểm thử

- `bash verify.sh`: luật lực, TCP, quyền lượt, thông báo, đăng ký không gửi thư,
  Supabase REST fixture, phòng, tải bộ cài và API admin.
- `Controls770Check`: JAR E72/E6 trong MicroEmulator, gán/lưu/khôi phục phím,
  chống gán trùng, giữ/thả lực và chuyển địa chỉ cũ sang TCP.
- `Online770Check`: hai MIDlet giao tiếp với server TCP thật trên localhost,
  phòng/sẵn sàng, vào trận, giữ lực dừng đồng hồ, bắn và logout.
- `backend/tests/retention770_check.py`: PostgreSQL 17 tạm, chạy migration lặp,
  bảo toàn dữ liệu, hạn/gia hạn/chống lặp và quyền RLS.

Chưa thử trên Nokia vật lý hoặc RG40XX H/tay cầm vật lý.
