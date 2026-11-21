# Gunny Java 7.9.1

Đã sửa trực tiếp source và build hai bộ cài E72/Asha và E6. Từ 7.9.0 đã nạp SQL VIP/cánh không cần SQL, env, secret hoặc đổi cổng mới. Chưa đẩy Git hoặc deploy production.

## Các lỗi sửa

- Đấu đơn: kết thúc/nhận thưởng dùng leave và dọn trạng thái mạng trước khi mở sảnh. Không quay lại phòng solo và chuẩn bị tải trận lần nữa. PvP phòng riêng/tổ đội/phó bản vẫn về phòng theo cơ chế riêng.
- Sảnh: ảnh nhà phóng và viền sáng dựng sẵn thành PNG cho hai độ phân giải, cache lại. Khi điều hướng không chạy các vòng tính viền/sao chép điểm ảnh lớn như trước.
- Menu G tài khoản test: thêm dòng thứ bảy +999 Thẻ VIP1–12/Full VIP. SQL chỉ tạo lưu trữ, không tự thêm nút client. Server vẫn kiểm tra quyền test_cheats do Admin bật. Các cấp phát dùng mã ổn định theo yêu cầu/vật phẩm để retry không cộng lần nữa; chỉ thực hiện ở sảnh/phòng chưa đánh. Không thay chức năng Full cấp cũ.
- Shop: tab VIP lấy nền/tab gốc cùng Đạo cụ, bỏ nút xanh khác kiểu. Danh sách cánh giữ tên/giá, bỏ dòng HP và chữ khóa cấp; thay bằng roomlist-lock gốc. Mềm trái trên cánh mở bảng đen bên trái, chứa HP, VIP yêu cầu, giá và thời hạn vĩnh viễn; vẽ trên cùng sau ví/nút Mua.
- Trận chiến: bỏ vòng VIP trên nhân vật đứng trong map, giữ cánh. Vòng hiển thị ở ảnh nhân vật đang đến lượt góc trái và các ảnh lượt nhỏ. Khi Bpow có VIP, vòng gấp3 (72→216), quay từ atlas dựng sẵn, đặt giữa màn để không bị cắt; VIP0 không có vòng. Cấp VIP của từng actor lấy từ roster server, dùng chung đường Bpow đang đồng bộ cho chủ và khách.
- Cache vòng lớn giải phóng sau Bpow/chuyển trận. Tận dụng 3 mẫu gốc, vẫn chưa có icon VIP10–12 và 12 bộ vòng nguyên bản riêng như đã báo ở 7.9.0.

## Kiểm thử

- Biên dịch backend Java17 và client CLDC/MIDP/preverify: qua.
- Fix791Check: quyền cheat, 13 loại +999 và gửi lặp không cộng trùng: qua.
- CardsPresence790Check (21), MatchReturnRegressionCheck, VipTcp790Check (20): qua. Giữ kiểm tra phòng riêng/tổ đội, quay về phòng cũ, reconnect và thưởng đồng thời.
- Ui790Check bổ sung kiểm tra solo dùng leave, menu7 mục, tooltip VIP, Bpow lớn và roster: qua cả320×240 và640×480, chạy6 vòng vào trận/quay Túi. Giả lập E72 heap32MB, E6 heap128MB. Ảnh trong client/build/vip790-*.png và fix791-cheat-*.png.
- CRC, MANIFEST7.9.1, JAD trỏ đúng JAR/kích thước: qua. Hash trong BUILD-7.9.1.json.
- Chưa có hai Nokia thật để đo FPS hoặc xác nhận hình Bpow trên hai máy thật; cần thử lại với bộ cài/server mới. Không khẳng định đã hết mọi lag trên điện thoại.

## File chính

Client: OnlineClient.java, OnlineResult.java, HomeScreen.java, PhoneCanvas.java, VipSystem.java, ShopScreen.java. Backend: TestCheats.java. Asset mới: home791-*-zoom/glow và vip-ring-*-216, hai cỡ màn. Kiểm tra mới: backend/tests/Fix791Check.java, mở rộng client/tests/Ui790Check.java. Metadata/trang tải/build chuyển7.9.1, chỉ giữ bộ cài hiện tại.

Build: `python client/build.py --tools D:/gunny/GunnyNokia/tools`. Backend: `./verify.ps1` hoặc `bash verify.sh`. Bộ cài ở public/downloads. Dữ liệu VIP/cánh dùng migration 202611220001 đã có; không chạy thêm migration cho bản vá này.
