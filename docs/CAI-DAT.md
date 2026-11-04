# Cập nhật Gunny Java 7.0.0

Cập nhật cả game trên các máy và toàn bộ gói server. Không cần SQL mới nếu đã chạy ba migration hiện có, gồm `202611040002_gunny_social_shared.sql`. Không ghi đè migration đã chạy.

## Phòng và đạo cụ

Khung bên phải dùng nền, ô xanh Z/X/C, ô đạo cụ, ảnh mời, ảnh server và nút bắt đầu/sẵn sàng lấy từ asset gốc. Sắp lại kích thước cho E72 320×240 và E6 640×480, không cắt giao diện từ ảnh tham khảo. Mục “Vật phẩm” đổi thành “Trong trận”.

Chọn một ô Z, X hoặc C, rồi chọn đạo cụ trong danh sách bên dưới. Chọn đạo cụ đã nằm trong ô khác sẽ đổi chỗ hai ô. Bộ ba lưu trên điện thoại. Khi Online, server xác nhận bộ ba; đổi đạo cụ sẽ hủy sẵn sàng và tìm trận để người chơi xác nhận lại. Chỉ đổi trước trận.

Mỗi ô dùng một lần trong lượt của chính người đó và có lại ở lượt kế tiếp. Tất cả đạo cụ tốn thể lực; không tốn xu, không dùng hết vĩnh viễn.

| Đạo cụ | Thể lực | Hiệu lực |
| --- | ---: | --- |
| Máy bay | 150 | Bắn máy bay đến điểm đáp theo vật lý bay cũ; thay cho đạn gây sát thương. |
| Hồi máu | 150 | Hồi tối đa 500 HP cho bản thân. Đầy máu thì không trừ thể lực/lượt dùng. |
| Hồi máu nhóm | 180 | Hồi tối đa 300 HP cho mỗi đồng minh còn sống, gồm bản thân. Không hồi đối thủ. |
| Đóng băng | 150 | Gắn vào loạt đạn trong lượt; đối thủ thực sự nhận sát thương bị mất một lượt. Hiển thị đóng băng một giây rồi chuyển lượt. Bắn hụt không đóng băng. |
| Tàng hình | 120 | Ẩn nhân vật và chấm bản đồ nhỏ với đối thủ đến lượt kế tiếp của người dùng. Đồng đội vẫn thấy; server vẫn tính va chạm. |
| Trợ ngắm | 90 | Hiện đường đạn dự kiến theo góc, lực và gió hiện tại đến hết lượt. Không tự căn góc/lực. |

Không thể bật đạo cụ trong lúc căn lực hoặc đang có đạn bay. Máy bay và đóng băng không bật chung. Các buff Q/W/E/R/T và B vẫn giữ luật cũ; đạo cụ và buff cùng tiêu thể lực của lượt đó. Mỗi lượt có 240 thể lực, di chuyển cũng tiêu thể lực.

Trong trận, hàng buff và ba ô đạo cụ chuyển sang bên trái. **Z/X/C dùng ba ô đạo cụ; F đổi vũ khí** (trước đây là X). Các nút cảm ứng dùng đúng vị trí mới. Trên máy có bàn phím QWERTY dùng phím chữ tương ứng.

Vùng Quan chiến hiện hiển thị “Đang đóng”; bản này chưa bổ sung chế độ người xem. Nút server hiển thị thông tin máy chủ hiện tại, không tự chuyển máy chủ khi đang ở phòng.

## Online

Server tự kiểm tra bộ đạo cụ, lượt dùng, thể lực, va chạm và hiệu ứng bằng cùng `Battle.java` với game. Client chỉ gửi ý định dùng ô Z/X/C, không gửi HP, sát thương hoặc kết quả tự khai. Trạng thái đạo cụ đi trong các đoạn phát lại đã xử lý, giữ cơ chế chờ cả hai máy xem xong trước lượt mới.

`/health` có `items:1`. Các máy trong cùng phòng cần bản 7.0.0 để dùng giao thức đạo cụ; không ghép phòng khác khả năng giao thức. Upload toàn bộ nội dung gói server vào repo GitHub hiện tại, đợi Render triển khai, rồi cài game mới cho các máy.

Đã kiểm tra luật game, giao thức HTTP localhost và giao diện MicroEmulator ở hai độ phân giải. Chưa kiểm tra trên Nokia vật lý hoặc server Render/Supabase thật.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
