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
