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


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
