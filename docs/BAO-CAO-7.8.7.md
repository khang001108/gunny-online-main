# Gunny 7.8.7

Chưa deploy Pi, chưa kiểm tra E72 vật lý. Không thêm trường hay bảng so với 7.8.6. File UPDATE-7.7.0-TO-7.8.7.sql tích lũy và bổ sung quyền SELECT bộ thẻ Tà Thần cho service_role (không mở quyền cho người chơi). Chạy một file này để bảo đảm endpoint xem thẻ đọc được bộ thẻ đã lưu.

## Sửa lỗi crash

Log người dùng chỉ đúng NullPointerException tại PhoneCanvas.drawVersus: versusOK.getWidth() được gọi khi ảnh null. Bản cũ chỉ tải nhóm ảnh nếu versusBg null, nên nhóm ảnh dở dang không được phục hồi. Bản mới kiểm tra từng ảnh bắt buộc và có chữ OK dự phòng nếu icon không đọc được. Không quy kết log này là thiếu RAM. Giữ các sửa giảm bộ nhớ của 7.8.6.

## Giao diện và điều hướng

- Tiệm rèn bắt đầu ở trang bị, OK tới danh sách chỉ có các cấp đá đang sở hữu, mềm trái xem thông tin, OK tới bùa/liên tục/cường hóa. Bỏ nút Đá Lv x2, mặc định một viên cho lượt chọn mới. Mềm phải trở về bước trước. Liên tục có viền vàng; kết quả thành công/thất bại hiện chữ.
- Tiêu đề Túi/Thời trang và rèn lấy tên vật phẩm đang trỏ. Đá, bùa và hòm dùng ItemTooltip với nền đen mờ đặt bên trái cùng phong cách trang bị, mô tả và số lượng lấy từ dữ liệu thật. Hòm ghi khoảng thưởng thực 50–100 đá Lv1 thay vì ví dụ 20 để không sai cơ chế đã chốt.
- Thông tin bạn bè: cấp và EXP một dòng nằm trên bảng đen; xếp hạng, thắng, lực chiến đều nằm trong bảng, không dùng chung cache kích thước với bảng Túi.
- Chọn phó bản: trái/phải chọn Kiến/Tà Thần, OK tới mức độ, OK tới xác nhận, OK chốt. Chạm map đổi ải. Hai ảnh cover/miêu tả thay đúng ải; các ô chờ thu cân lại, tiền lên trên, thanh thưởng nằm dưới. Preview hiển thị xác suất theo cấp đá và độ khó, VIP Thương Cổ/cánh theo số thẻ hiếm trong bộ 18 thẻ cuối Tà Thần, túi hoàn thành 100% ở boss cuối. Ảnh vật phẩm dùng tài nguyên Túi. Tỷ lệ đá trên thẻ cuối được nhân tỷ lệ thẻ đá còn lại sau thẻ hiếm, làm tròn một chữ số.

## Mở toàn bộ bộ thẻ

Endpoint deck kiểm tra đã lưu trận và mọi thành viên còn hoạt động, đủ điều kiện, đã nhận đủ lượt. Trước đó không trả nội dung thẻ úp. Khi đủ lượt, server giữ một bản xem trong Room: thẻ đã chọn dùng biên nhận thật, thẻ hiếm Tà Thần dùng bộ thẻ đã lưu DB, các ô thường chưa được chọn được server xác định một lần khi đóng chọn thưởng. Đây là chế độ chỉ xem: không cấp thêm đá/xu/vật phẩm, không cho chọn thêm. Kết quả xem ổn định cho mọi người trong phòng. Không giữ khóa phòng trong khi đọc deck Tà Thần qua HTTPS. Bộ xem không tồn tại khi server khởi động lại, tương tự vòng đời phòng hiện có.

Client thương lượng capability deck qua health; bản client cũ không gọi endpoint mới. Cần cập nhật server để máy mới mở toàn bộ bộ thẻ online. Offline mở các ô còn lại sau đủ lượt, chỉ cộng đúng vật phẩm của thẻ đã chọn. OK tiếp tục về phòng như luồng cũ sau phần xem.

## Kiểm thử và build

Ui787Check trên giả lập 320×240, 640×480 kiểm tra trường hợp nhóm ảnh VS tải dở, bước chọn đồ/đá sở hữu/bùa, tooltip, phòng chờ, picker và bố cục bạn bè cấp 70. DeckRevealCheck kiểm tra chưa đủ lượt không lộ thẻ, đủ lượt có bản xem ổn định và không cộng tiền. Suite expansion 205, TCP Tà Thần 162, quà 142 và cheat 24 kiểm tra qua. JAR preverify CLDC1.1/MIDP2.0.

Build bằng client/build.py với ECJ/ProGuard và JAR tài nguyên kèm; trên máy phát triển dùng D:/gunny/GunnyNokia/build-787.ps1. Backend JDK17+, biên dịch backend/src/*.java; gói có JAR sản xuất và run-787.sh. Docker build tại gốc repo. Giữ nguyên env/secret. /health và admin header báo 7.8.7 để kiểm tra đúng phiên bản được triển khai.
