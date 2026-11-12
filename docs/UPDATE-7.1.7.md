# Gunny Java 7.1.7 — Tủ thuốc, Tủ lạnh, Giỏ đồ, Pháo

Cập nhật game và server. **Có migration mới** `supabase/migrations/202611050002_gunny_arsenal.sql`; chạy sau migration shop 7.1.6. Không chạy lại schema gốc. Gói chuẩn bị file, chưa chạy SQL hoặc triển khai server thay người dùng.

## Bốn vũ khí

| Vũ khí | ID game | Asset mang/cầm | Đạn và nổ SWF | Góc | Sát thương cơ sở | Bán kính đất |
|---|---:|---|---:|---|---:|---:|
| Tủ thuốc | 2 | arm/OPSbox, cầm viên thuốc | 171 | 20–75° | 300 | 10 |
| Tủ lạnh | 4 | arm/television, cầm tủ lạnh | 331 | 15–70° | 370 | 17 |
| Giỏ đồ | 7 | arm/fruit, cầm cà chua | 401 | 10–80° | 315 | 14 |
| Pháo | 1 | arm/bombpack, cầm ống pháo | 161 | 15–60° | 390 | 10 |

Hình mang trên lưng, icon, vật cầm lúc lấy đà, đạn bay và hiệu ứng va chạm được lấy riêng từ nguồn gốc. Pháo là giỏ pháo xanh có các ống pháo. Không ném cả túi thuốc hoặc cả giỏ đồ thay cho viên đạn. Giữ chuỗi lấy đà/ném của bản Java ME như lu gạch/phi tiêu; đạn xoay theo chuyển động. Tủ lạnh dùng khung đạn 32px để giữ hình cao, các loại mới còn lại dùng 16px.

**Các con số là cân chỉnh Java ME, không phải bảng chỉ số PC khôi phục.** Bộ asset có ảnh/animation nhưng chưa có bảng ItemTemplate/BallInfo gốc chứa số liệu. Luật được đặt trong `Battle.java`, dùng cùng mã trên điện thoại và máy chủ. Tủ lạnh rơi nặng hơn và ít nhạy gió; viên thuốc nhẹ và nhạy gió; cà chua ở mức giữa; ống pháo có vùng góc hẹp và vùng nổ tập trung. Bốn vũ khí trước giữ góc, trọng lực, gió, sát thương và bán kính cơ sở.

POW, +2, x3, +50%, bay, máu và các đạo cụ Z/X/C dùng chung đường xử lý đã có. POW tiêu thụ nộ khi nhả lực, tăng sát thương/vùng nổ. Bay và đạn băng giữ vật lý riêng của đạo cụ. AI, phát đạn local, máy chủ xử lý và trình phát lại dùng cùng lực gió/trọng lực theo loại đạn.

## Shop, túi và online

Tổng cộng 8 vũ khí. Bốn loại mới có giá: tủ thuốc 950, tủ lạnh 550, giỏ đồ 700, pháo 650 xu. Mua bằng giỏ hàng, vào túi để trang bị; phím F đổi trong số vũ khí đã có. Không tự cấp vũ khí mới cho tài khoản.

Migration mở ràng buộc equipped cho tám ID và cập nhật RPC mua/checkout. Giữ số dư, đồ đã có và giá của bốn loại trước. Máy chủ vẫn tự tính giá và kiểm tra sở hữu.

`/health` có `arsenal:2`; client gửi `X-Gunny-Arsenal: 2` lúc đăng nhập. Khả năng này được giữ cố định theo phiên; danh sách phòng, vào phòng, mời và ghép trận tách máy dùng luật 8 vũ khí với máy cũ. Máy cũ đăng nhập tài khoản đang cầm vũ khí mới được yêu cầu cập nhật trước khi đọc dữ liệu không hỗ trợ.

## Cập nhật

1. Nếu chưa có shop 7.1.6, chạy migration shop trước.
2. Chạy riêng `202611050002_gunny_arsenal.sql` trong Supabase SQL Editor.
3. Cập nhật toàn bộ gói server, kiểm tra `arsenal:2`, `shop:1`, `items:2`, `cards:1` ở /health.
4. Cài JAR/JAD 7.1.7 cho E72 hoặc E6. Hai người chơi nên cùng bản mới.

## Kiểm tra

Kiểm tra 320×240 và 640×480: asset cầm/đạn/nổ, lấy đà, va chạm/phá đất, shop và túi tám loại. Hai cặp client thật trong MicroEmulator kiểm tra thuốc+tủ lạnh, cà chua+pháo với mạng nhận trễ, buff nhiều phát và POW: receiver phát lại hết, địa hình khớp server rồi mới đổi lượt. Kiểm tra server về giá/sở hữu, POW và đặc tính vật lý, cách ly phiên cũ/mới; bộ kiểm tra server cũ cũng chạy lại. Migration được kiểm tra cú pháp SQL/PLpgSQL, chưa chạy trên Supabase thật. Chưa thử trên Nokia thật.
