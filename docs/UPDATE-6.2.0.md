# Cập nhật Gunny Online 6.2.0

Bản này thay đổi cả game và máy chủ. Cập nhật toàn bộ gói server trước khi mở lại JAR mới.

1. Giải nén `gunny-online-main.zip`, chép các file mới vào repository GitHub hiện tại, gồm `backend/src`, **đủ sáu file** `backend/assets/land0.png` đến `land5.png`, Dockerfile và `.dockerignore`.
2. Push lên GitHub, chờ Render deploy thành công. Giữ nguyên Environment trên Render và Supabase; không cần chạy lại SQL hoặc tạo lại tài khoản.
3. Mở `/health` trên hostname Render của bạn. Máy chủ mới có `"rooms":2` và `"maps":6`.
4. Thoát game cũ, cài JAR/JAD 6.2.0 đúng E72 hoặc E6 rồi mở lại để nhận khả năng mới của máy chủ.

**Đấu đơn:** vào phòng hai phe đỏ/xanh. **Tổ đội:** vào màn VS chờ người ở phe đối diện. Khi đối thủ thật vào, nhân vật và map ngẫu nhiên đã chốt hiện ở cả hai máy. Người chơi khách bấm **Sẵn sàng**, chủ phòng bấm **Bắt đầu**.

Chạm ô bản đồ để mở màn chọn map. Chọn ảnh chỉ đổi phần xem trước; **Xác nhận** mới gửi thiết lập lên server. Chủ phòng được chọn map và thời gian 8/12/15 giây. Đổi thiết lập sẽ hủy trạng thái sẵn sàng cũ để người chơi xác nhận lại. Map ngẫu nhiên được server chọn, không chọn riêng trên từng máy.

Ba map mới có đủ nền và địa hình: **Đảo Trên Mây** (1008), **Bờ Biển Nắng** (1014), **Đáy Biển** (1025). Cùng với ba map cũ, tổng cộng có sáu map. Game cũ vẫn dùng phòng định dạng cũ và ba map cũ.

Chạy `verify.ps1` trong repository bằng JDK 17 trở lên để kiểm tra server. Kiểm thử hai client và ảnh xem trước nằm trong workspace GunnyNokia.
