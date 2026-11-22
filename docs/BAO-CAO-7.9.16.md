# Gunny Java 7.9.16 — Phòng viễn chinh và giảm giật/bộ nhớ

Đã sửa source, build JAR/JAD và kiểm tra cục bộ. Chưa deploy. Không đổi SQL, env/secret hay dữ liệu người chơi.

## Giao diện

Loại phòng trong bảng tạo phòng trở lại **Ải viễn chinh**, bỏ dòng “1: Kiến • 2: Tà Thần” và bỏ luôn phím/chạm đổi ngầm ở vị trí đó. Tên phòng mặc định cũng là Ải viễn chinh. Chọn Kiến/Tà Thần bằng bảng chọn map trong phòng chờ như hiện tại; không xóa hai phó bản.

## Các điểm đã tối ưu

- Vòng VIP ở avatar nhỏ: trước đây mỗi lần vẽ một cấp vòng lại xóa cache hai cấp khác. Nếu nhiều người khác cấp xuất hiện cùng khung hình thì ảnh bị đọc/giải mã lại liên tục. Giữ cache tối đa ba mẫu nhỏ, giải phóng theo vòng đời màn hình. Các vòng lớn vẫn chỉ giữ mẫu đang dùng để giới hạn RAM.
- Tra glyph font: trước đây quét cả bảng ký tự cho mỗi chữ, nhiều lần mỗi khung hình. Thêm bảng tra trực tiếp ASCII/Latin và tiếng Việt; kiểm tra từng ký tự để đảm bảo đúng glyph gốc. Ký tự ngoài các vùng này vẫn dùng cách tra cũ.
- Đồng hồ hiệu ứng mạng: thay việc ghép nhiều chuỗi cho mỗi đơn vị/vụ nổ/số sát thương bằng khóa số và bộ nhớ đệm cố định. Giữ quy tắc hiệu ứng không tua ngược khi nhận snapshot trùng/cũ; không đưa đồng hồ hiển thị vào vật lý.
- Snapshot client: tái sử dụng các đối tượng đạn, vụ nổ và số sát thương trong pool có giới hạn20/16/20, đặt lại trạng thái trước mỗi lần đọc. Giảm tạo rác khi có nhiều đạn và số sát thương.
- Không vẽ NPC nằm hoàn toàn ngoài vùng nhìn. Cache vị trí nền đất của vùng lửa thay vì quét mặt đất mỗi lần render; nền Tà Thần vốn không bị phá.
- Bổ sung bảo vệ tên nhân vật khi đang chuyển trạng thái online/offline, tránh truy cập tên bot với botStyle=-1 trong lúc chuyển màn hình.

Không giảm số khung animation, không xóa cánh/vòng đang dùng, không giảm chất lượng ảnh, không đổi sát thương, AI, lượng lính hoặc thưởng để đạt số đo đẹp.

## Dung lượng

Loại khỏi cả seed và overlay khi build: các vòng VIP216 không có trong danh sách kích thước runtime72/44/24/144 và `fx-pow-ring.png` của hiệu ứng Bpow cũ không còn được code tải. Các ảnh hiện dùng giữ nguyên. Bản E72 tiếp tục dùng ZIP_STORED như trước để giữ tương thích bộ cài cũ.

| Bộ cài | 7.9.15 (byte) | 7.9.16 (byte) | Giảm gần đúng |
|---|---:|---:|---:|
| E72/Asha | 27.801.969 | 26.112.000 | 1,69MB / 6,1% |
| E6 | 76.196.406 | 70.578.000 | 5,62MB / 7,4% |

Kích thước chính xác và SHA256 cuối cùng ở `docs/BUILD-7.9.16.json`; số mới trong bảng đã làm tròn.

## Kiểm tra và giới hạn số đo

- `UiPerf7916Check`: dùng snapshot cảnh Sulan đỏ có viện binh, chạy800 lượt đọc snapshot + render với ảnh đích dùng lại. E72 heap32MB, E6 heap128MB. Bài đo là Java SE/MicroEmulator trên PC, không phải FPS Nokia thực.
- Bản cũ:800 khung,395ms,39 lần GC trong lần đo baseline. Các lần đo bản mới E72 dao động381–466ms,15–21 lần GC; lượt cuối381ms/21GC. Vì thời gian CPU biến động, không kết luận FPS tăng bao nhiêu. Số lần GC và việc bỏ tải lại vòng liên tục là bằng chứng giảm áp lực bộ nhớ, có ích cho hiện tượng giật từng đợt.
- Kiểm tra từng glyph bằng cách đối chiếu với phép quét cũ, cache hai cấp vòng liên tiếp không bị mất, snapshot dùng lại cùng đối tượng, bảng tạo phòng bỏ cả nhãn lẫn điều khiển đổi phó bản ngầm.
- Kèm kiểm tra UI trước: slot NPC6–13, điều kiện3 giây/phímM, gói thiếu yêu cầu snapshot, đọc snapshot/chunk/TCP, phó bản, phòng/map và PvP. Hai JAR thực đều qua.
- `verify.ps1` qua; log `backend/build/verify-7916.log`. Compile/preverify CLDC/MIDP, đối chiếu class đóng gói, version và kích thước JAD, font và asset.

Chưa đo trên Nokia vật lý, chưa có trace FPS/mạng của lần tụt FPS người dùng gặp. Bản này xử lý các điểm tải ảnh/tạo rác/quét chữ xác định được trong source; chưa thể khẳng định đã loại mọi nguyên nhân tụt FPS trên máy thật.
