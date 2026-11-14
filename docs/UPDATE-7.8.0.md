# Gunny 7.8.0 — sảnh, rèn đồ và Huyệt Ma Kiến

Bản chuẩn bị ngày 13/11/2026. Chưa chạy SQL trên Supabase production, chưa thay
container đang phục vụ người chơi. Giữ project `mnquwasxacifkktmckdv`.

## Nội dung

- Sảnh dùng nền đảo và hai công trình Online/Tiệm rèn từ ảnh được cung cấp. Bấm
  Online mở đăng nhập; đăng nhập xong trở lại sảnh, Sảnh mở danh sách phòng PvP.
  Có nút đăng xuất. Cổng Online cũ đổi thành Phó bản.
- Sửa bố trí theo ảnh đánh số: ô 1 là Tiệm rèn, ô 2 là Online, ô 3 là Phó bản.
  Công trình thường không có viền chọn; Online/Tiệm rèn phóng to khoảng 20% và
  có viền vàng khi được chọn, giữ chân công trình ở cùng vị trí. Phó bản dùng ảnh
  gốc lớn hơn và căn giữa ô đất; giữ ảnh vàng gốc khi chọn. Vùng chạm và phím
  điều hướng khớp bố trí mới.
- Túi đồ có mục Đạo cụ giữa Trang bị và Thời trang, ba thanh máu đỏ, thể lực xanh
  lá, EXP xanh dương. Công/thủ/may mắn tách phần tăng theo cấp và trang bị/rèn:
  `120 (+30)`. Mỗi cấp vẫn tăng 5 cho cả ba chỉ số.
- Shop bán đá cường hóa ở mục Đạo cụ, giá 80 tiền mua sắm/đá. Tiệm rèn chỉ nhận
  vật phẩm đã sở hữu; lần đầu tốn 4 đá, sau đó 8, 12, 16… theo công thức
  `4 × (cấp rèn hiện tại + 1)`. Giới hạn cấp rèn là +99.
- Tiệm rèn dùng bố trí theo `tiem_ren.jpg`: hai ô trang bị/đá bên trái, phần trăm,
  ô Liên tục và nút Cường hóa; bên phải có hai ngăn Trang bị/Đạo cụ. Chỉ vũ khí,
  áo rời, nón và kính nhận lần rèn mới. Rèn thành công tăng 5 công cho vũ khí,
  tăng 5 thủ cho áo/nón/kính. Các cấp rèn cũ của loại khác vẫn được bảo toàn.
  Phần trăm là 100% khi đủ điều kiện, 0% khi chưa đủ; bản này không thêm xác suất
  thất bại. Liên tục chỉ chạy sau khi xác nhận, trừ đá từng lần và dừng khi thiếu đá,
  đạt +99, tắt tích hoặc rời cửa sổ. Chỉ vật phẩm đang mặc có tác dụng.
  Đổi vũ khí giữ đúng phần cộng từ trang phục. Theo cơ chế game hiện
  có, thủ tăng giới hạn máu và may mắn tăng giới hạn thể lực.
- Đá cường hóa dùng icon gốc `strength4/icon.png` theo mẫu đá lửa. Ba mục bên phải
  túi đồ dùng biểu tượng/nhãn đầy đủ của giao diện gốc, thay cho chữ TB/ĐC/TT.
- Khắc phục mảng hình vuông cạnh nhân vật: ảnh nền phó bản từng ghi đè `back7.png`
  của vũ khí trái cây. Bộ tạo asset chỉ ghi nền vào `bg6/bg7`, khôi phục `back7`
  thành dải chín khung 32×40 có alpha và kiểm tra kích thước/độ trong suốt.
- Trận 3v3 có 18 thẻ chung, mỗi người đủ điều kiện chọn 2 thẻ. Thưởng tiền hoặc
  1–10 đá/thẻ. Trận đấu đơn giữ bảng thưởng cũ. Server quyết định vật phẩm, số lượng
  và quyền chọn; gửi lại yêu cầu không nhận thêm thưởng.
- Di chuyển của người chơi từ xa được nội suy ở phần vẽ giữa các trạng thái TCP.
  Tọa độ va chạm, sát thương và chuyển lượt vẫn do server quyết định. Người đang
  điều khiển giữ phản hồi di chuyển tại máy; đường đạn tiếp tục dùng trạng thái trực tiếp.

## Phó bản

Huyệt Ma Kiến yêu cầu cấp 15, cho 1–3 người cùng đội. Phòng có tạo phòng, tổ đội,
mời bạn, chọn thường/khó và nút sẵn sàng/bắt đầu. Ải 1 diệt 20 kiến đỏ/lam;
ải 2 đánh Nữ Vương. Map, ảnh tải, hình kiến và phần thưởng dùng tài nguyên gốc.
Kiến có hành vi tiến gần/cắn; Nữ Vương có đòn bắn. Chế độ khó tăng máu và sát thương.
Thắng thường lưu quyền mở khó riêng cho từng tài khoản. Chuyển ải có bước tải map
đồng bộ cho cả phòng; có thể trở lại trận từ màn hình phòng.

Khi thắng, mỗi người chọn 2 trong 18 thẻ, mỗi thẻ chỉ thưởng 10–100 đá. Thua không
mở khó và không có thẻ phó bản. Thắng được 300 EXP, thua được 100 EXP.
Thiết kế hai ải tham khảo [hướng dẫn Huyệt Ma Kiến chính thức](https://gunny.360game.vn/thuvien/tieu-hoc/huyet-ma-kien.html).
Số máu, sát thương và phần thưởng là luật của bản Java này.

Chưa đăng nhập vẫn mở cửa sổ Phó bản để chơi một mình offline. Chơi offline có
đủ hai ải, yêu cầu cấp 15, thường/khó, hai thẻ đá và lưu tiến độ trên máy. Không
gọi mạng hoặc gửi đăng nhập khi chọn chơi một mình. Chọn Tổ đội Online mới mở
đăng nhập; đăng nhập xong đến thẳng danh sách phó bản. EXP/đá/quyền mở khó của hồ
sơ offline được giữ riêng khi đăng nhập hoặc đăng xuất. Thưởng offline có cùng
giới hạn với online; thua không có thẻ, thắng không cấp thêm xu.

## SQL và bảo toàn dữ liệu

Nếu Supabase đã chạy migration tài khoản 7.7.0, mở SQL Editor và chạy toàn bộ
[UPDATE-7.7.0-TO-7.8.0.sql](../supabase/UPDATE-7.7.0-TO-7.8.0.sql) một lần.
File gộp bốn migration sau 7.7.0 trong cùng một transaction và nạp lại schema
cache khi commit. File thêm đá, tiến độ cường hóa +15, độ khó và phần thưởng
hai ải Huyệt Ma Kiến. UUID, thông tin đăng nhập, tiền và EXP hiện có được giữ lại.

Nếu trước đây chỉ cập nhật game/server 7.7.0 mà chưa chạy SQL tài khoản,
cần áp dụng [migration tài khoản 7.7.0](../supabase/migrations/202611120001_virtual_accounts_retention.sql)
trước. Dùng `supabase/GUNNY-ALL.sql` khi cài mới.

Các thao tác mua/rèn khóa profile, dùng request key bền vững và từ chối tái dùng
key với tài khoản/thao tác/payload khác. Nhận thẻ khóa kết quả trận, giới hạn hai
lượt chọn, chống chọn trùng thẻ, chống nhận bằng cả API cũ lẫn mới. RPC chỉ cho
`service_role`, không cấp cho `anon`/`authenticated`.

Migration tài khoản 7.7.0 giữ SHA256
`274110436b25eae327c6c811efb4ee535fb4978c33759515a04b10f347da2f9f`.
Xem [rà soát SQL 7.8.0](REVIEW-SQL-7.8.0.md) và [rà soát retention 7.7.0](REVIEW-SQL-7.7.0-2026-11-13.md).

## Kiểm thử và cập nhật

Chi tiết kết quả trong `verification-7.8.0.json`, log `gameplay-780-verification.log`
và ảnh `review780/`. Có kiểm thử ba MIDlet đóng gói thật qua TCP ở 320×240 và
640×480, cùng PostgreSQL 17 riêng biệt. Chưa kiểm thử trên Nokia/tay cầm vật lý
hoặc đo gameplay qua Internet/4G.

Từ workspace đầy đủ, chạy `bash GunnyNokia/verify-780.sh`. Nếu muốn chạy kiểm thử
PostgreSQL, cung cấp `GUNNY_TEST_PG_PORT` của instance localhost dùng riêng đã gắn
`gunny.isolated_test=1`; cần psycopg. Bộ kiểm thử tạo/xóa database tạm trên instance đó.

Image đích là `gunny-online:7.8.0`. Tool `sudo bash deploy/cap-nhat-gunny.sh`
kiểm tra cả hai migration và tính năng mới trước khi đổi container. Xem
[hướng dẫn tool](../deploy/CAP-NHAT-DOCKER.md). Cập nhật cả server và bộ cài 780;
phòng mới tách client 780 khỏi client cũ để tránh sai định dạng gói tin.

## Chỉnh giao diện theo asset chỉ định

Dùng ddtstore 80 cho đá, 78 cho nút Cường hóa, 81 cho khung thanh và 19 cho phần
ruột thanh. Tiệm rèn có một lưới trang bị 25 ô với phân trang, phím mềm phải xem
thông tin; trái ở đầu hàng chuyển về phần cường hóa. Đạo cụ trong túi có 25 ô.
Thông tin trang bị hiển thị Strength và chỉ số gốc (+cường hóa). Chỉ số/thanh nằm
trong khung; nhãn công trình chỉ hiện khi chọn; Phó bản nâng cao và căn giữa.

Bộ JAR/JAD mới đã kiểm thử UI và offline ở hai độ phân giải. Image Docker đã
chuẩn bị trước đó chưa chứa lượt chỉnh UI này; cần build lại từ source hiện tại
bằng tùy chọn --build khi chủ máy thực hiện cập nhật. Không chạy SQL/đổi production.
