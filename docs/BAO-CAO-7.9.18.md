# Gunny Java 7.9.18

Đã chỉnh source và build JAR/JAD E72/Asha302 và E6. Chưa deploy, không chạy SQL và không đổi env/secret/dữ liệu người chơi trên máy chủ.

## Hai thay đổi

- Giáo nhọn lớn của Sulan vàng: đảo đầu giáo 180 độ cho cả tám hướng trong atlas gốc, ở hai độ phân giải. Cập nhật bộ sinh asset tương ứng. Giữ đường bay, các loại giáo/đạn khác và hình nguồn.
- Nút Thoát trận trong menu tạm dừng phó bản gửi `dungeonretreat` qua TCP, thay vì lệnh rời phòng PvP. Server xác nhận rồi client xóa trạng thái trận và hiển thị phòng chờ phó bản.
- Dùng lại cơ chế phòng chờ/đội `waitingCopy` và `returnRooms`. Giữ tên, mật mã, chủ phòng, ghế đồng đội/bot, phó bản, độ khó và ải để chơi lại. Nếu đồng đội còn trong trận, họ tiếp tục và có thể về cùng phòng sau đó; phòng chờ chưa cho bắt đầu khi đội chưa về đủ. Một người chơi thoát thì giải phóng trận cũ. ID phòng chờ có thể khác ID trận đã thoát.
- Lệnh lặp khi đã về phòng chờ không tạo phòng hay thưởng lần nữa. Không áp dụng lệnh này cho PvP. Khi kết quả đã được xác lập, vẫn dùng kiểm tra lưu kết quả và nhận thẻ hiện có, không bỏ qua bảo vệ phần thưởng.

Giữ các bản vá HUD/snapshot7.9.17 và tối ưu7.9.16.

## Kiểm tra

- `DungeonRetreat7918Check`: TCP thật, Kiến ải1/2 và Tà Thần ải1/2/boss; chơi đơn, hai người, chủ phòng hoặc khách thoát, cả loading và đang đánh. Kiểm tra lệnh lặp, không cộng vật phẩm khi thoát, giữ cấu hình đội, đồng đội tiếp tục loading/trận và quay về, chơi lại, chặn thao tác vào phòng PvP.
- `UiRetreat7918Check` với hai JAR phát hành thực tế: E72 320×240 heap32MB, E6 640×480 heap128MB. Kiểm tra nút xác nhận thoát phó bản/PvP, trạng thái về phòng chờ, cùng các kiểm tra renderer/snapshot/HUD của bản trước.
- `verify.ps1` qua: `backend/build/verify-7918.log`. Bài kiểm tra mới cũng được thêm vào `verify.sh`.
- Đối chiếu từng class với `preverified.jar`, manifest/JAD cùng7.9.18, URL và kích thước đúng. Kiểm tra pixel cả tám ô giáo vàng đảo180 độ, ba hàng projectile còn lại giữ nguyên. Hash ở `BUILD-7.9.18.json`.

Chưa kiểm tra trên điện thoại Nokia vật lý hoặc production. Cần cập nhật cả backend và JAR7.9.18 để có thao tác TCP mới. Không có SQL/env/secret mới cho bản này; không đóng ZIP Pi5.
