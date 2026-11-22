# Gunny Java 7.9.14

Đã sửa source và build JAR/JAD; chưa deploy. Không có SQL hay env/secret mới.

- Chặn slot NPC chưa nhận đủ dữ liệu đi vào renderer nhân vật. Cache thân nghiêng/mờ dùng kích thước `Battle.MAX_UNITS`, thêm kiểm tra trước khi truy cập bộ ảnh avatar 6 người. Xóa trạng thái NPC cũ khi chuẩn bị tải lại trận. Có kiểm tra tái hiện slot 6–13 với NPC=0 và maxHp cũ còn sót, đúng kiểu lỗi trong stack `drawSlopedBody` người dùng gửi.
- Gặp khoảng trống chỉ số đoạn TCP: bỏ hàng đợi thiếu, yêu cầu snapshot đầy đủ qua luồng phục hồi đang có, giữ đăng nhập thay vì ném lỗi “Thiếu đoạn hành động”. Không cấp lại thưởng hoặc sửa HP bằng client.
- Giáo Sulan: sửa hướng gốc của sprite giáo và tăng thời gian bay vật lý để thấy cung bay rõ, không còn đường gần ngang. Dùng lại bitmap 81 của living121, không tạo hình giáo khác.
- Đã xem video `D:/gunny/tà thần phó bản/ải 3 - boss đỏ - phần 2.mp4`, đặc biệt đoạn khoảng 84–85 giây. Lửa địa ngục đổi thành ba đợt sát nhau, mỗi đợt nhắm các người chơi còn sống; quả lửa rơi từ phía trên chếch về mục tiêu. Giữ sát thương cháy, dập lửa của đậu, chiêu nổ vị trí và gọi viện binh từ bản trước. Đây vẫn là thích nghi Java ME từ video, không chạy timeline Flash nguyên bản.
- Điều kiện thắng/thua dùng chung Kiến 1/2 và cả ba ải Tà Thần. Khung thông tin vật phẩm đen gốc, hai cột, màu tiêu đề gốc cam/xanh. Tự hiện 3 giây khi vào ải rồi ẩn; M mở lại hoặc ẩn. Kiến cũng có giới hạn 200 lượt tương ứng dòng điều kiện thua.
- Mini map phòng chờ hiển thị ải hiện tại–tổng: Kiến 1-2, 2-2; Tà Thần 1-3, 2-3, 3-3. Không đổi ảnh mini map theo tiến trình.
- Chân dung trên thanh máu Bulan/Sulan thu về 22×23 theo kích thước chân dung thanh Nữ Vương, không phóng/cắt ảnh 48×50 vào ô nhỏ; cache riêng khỏi avatar góc trái.

## Xác minh

- `verify.ps1` và các kiểm tra Sulan/viện binh trước vẫn qua; log cục bộ `backend/build/verify-7914.log`.
- `Fix7914Check`: cung giáo đi lên, mưa lửa rơi từ trên, ba người nhận ba đợt trước lượt đậu, điều kiện giới hạn lượt Kiến.
- `UiFix7914Check`: JAR thực 320×240 heap32MB, 640×480 heap128MB. Tái hiện và kiểm tra slot 6–13, tự hiện/ẩn điều kiện cả 5 ải, phím M, gói TCP bị thiếu yêu cầu phục hồi thay vì mất đăng nhập. Kèm đọc snapshot/chunk, phòng, map, animation và PvP từ bộ kiểm tra trước.
- Compile/preverify CLDC/MIDP; manifest, kích thước JAD, class đóng gói và hash được đối chiếu. Hai bộ cài hiện tại là 7914; hash ở `docs/BUILD-7.9.14.json`.

Chưa thử Nokia vật lý hoặc mạng production. Không thay cấu hình DNS/mạng máy chủ; các lỗi mất kết nối/không phân giải được tên miền trong console không phải nguyên nhân của lỗi chỉ số mảng.
