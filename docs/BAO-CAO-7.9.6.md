# Gunny Java 7.9.6

Đã sửa source và build bộ cài E72/E6. Chưa deploy hoặc thay cấu hình production.

## Cheat và HP cao

Snapshot TCP lưu HP trong2 byte, nhưng client trước đây đọc bằng `readShort`, khiến HP trên32.767 thành số âm. Full cấp70 cộng cánh/trang bị vượt ngưỡng này. Đổi riêng HP sang `readUnsignedShort`; giữ nguyên độ dài và thứ tự gói tin. NPC bổ sung đã dùng kiểu không dấu từ trước.

Tài khoản bật cheat vẫn vào trận, dùng chỉ số và chế độ bất tử bình thường. Không thêm điều kiện cấm chơi cho cheat; bộ lọc chỉ nằm ở danh sách xếp hạng. Tài khoản thường có HP cao cũng hưởng bản sửa này.

## Cánh ở giao diện nhân vật

Tăng atlas nhìn chính diện theo3 kích cỡ112×72,64×42,32×22. Hạ gốc cánh xuống vai/lưng; dùng chung cách đặt cho Túi, Shop, xem bạn bè và các ảnh nhân vật dùng chung renderer. Vẽ vòng VIP trước, cánh sau, nhân vật và ô trang bị ở lớp trên. Giữ animation gốc20 khung; không đổi cánh chiến đấu19 khung của7.9.5.

Giải phóng cache ảnh cánh chiến đấu khi rời trận/loading, và bỏ atlas cánh xem thử không còn dùng trong các màn chỉ có một nhân vật. Không giữ đồng thời tất cả các atlas lớn của màn trước.

## Ghép trận/loading

- Cánh và vòng VIP dùng PNG đứng yên riêng, không giải mã atlas animation để vẽ loading.
- Người chưa xong hiện phần trăm; người đã xong hiện icon xanh `room-loading-ok` gốc.
- Server chỉ bắt đầu chờ2 giây khi tất cả thành viên đã báo tải xong. Nếu readiness mất trước khi đủ thời gian thì đặt lại mốc chờ. Trong2 giây này battle countdown không chạy.
- Client giữ màn loading đến khi server chuyển phase sang trận. Không phát thông báo đến lượt trong loading; chỉ báo khi thật sự vào trận và là lượt của người đó.

## Kiểm tra

- `Loading796Check`: tài khoản cheat HP cao vào trận, giữ god mode, bị loại khỏi xếp hạng; người chưa tải ngăn trận bắt đầu; đủ người vẫn chờ2 giây; mất readiness đặt lại chờ; không mất thời gian lượt trong lúc chờ.
- `Ui796Check`: đọc nguyên gói snapshot từ server, HP trên32.767 giữ đúng giá trị và không lệch byte; không báo lượt trong loading; không dùng atlas animation trong loading; chỉ vào trận sau server; các kiểm tra hồi quy UI/cánh/VIP/Túi/Shop.
- Hai JAR thực:320×240 heap32 MB và640×480 heap128 MB. Đã kiểm tra hình Túi và loading. Hai ảnh loading ở frame0 và17 giống hệt nhau.
- `verify.ps1`: toàn bộ kiểm tra server hiện có; log tại `backend/build/verify796.log`. Những kiểm tra cũ chờ100 ms sau readiness đã được cập nhật theo khoảng chờ2 giây mới.
- Manifest/JAD7.9.6, kích thước và hash tại `docs/BUILD-7.9.6.json`.

Không có SQL/env/secret mới so với7.9.5. Chưa thử trên máy Nokia vật lý. Các mục kinh tế còn thiếu từ yêu cầu cũ vẫn ghi trong báo cáo7.9.3.
