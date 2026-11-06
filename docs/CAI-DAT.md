# Nhạc gốc và Mộc Diệp Trấn — 7.5.6

Cập nhật cả server và JAR/JAD 7.5.6 cho các máy cùng chơi: map số 4 đã thay địa hình. Không có SQL mới. Các sửa lỗi máy bay, POW và phím W của 7.5.5 được giữ lại.

| Cảnh | Mã nhạc trong Resource/sound |
| --- | --- |
| Sảnh chính, đăng nhập | 062 |
| Chiến thắng | 063 |
| Thua cuộc | 064 |
| Sảnh online, phòng online, tổ đội, phòng offline | 065 |
| Đu Quay (tên cũ Làng Mặt Trời) | 053 |
| Rừng Nhiệt Đới (tên cũ Rừng Rễ Cây) | 054 |
| Lớp Học Ma Thuật (tên cũ Phòng Giả Kim) | 1004 |
| Phố Nấm Lùn (tên cũ Đảo Trên Mây) | 059 |
| Mộc Diệp Trấn (thay Bờ Biển Nắng) | 1035 |
| Đáy Biển | 055 |

Map Mộc Diệp Trấn lấy nguyên asset `Resource/image/map/1035`, gồm nền, địa hình, thumbnail, banner và mask va chạm cho E6. Server dùng cùng hình địa hình land4. Năm map còn lại chỉ đổi tên/nhạc theo bảng, giữ hình và địa hình.

Nhạc là các khung MP3 gốc tách khỏi FLV, không cắt ngắn hoặc nén lại. Nhạc nền lặp; thắng/thua phát một lần. Tuỳ chọn bật/tắt âm thanh vẫn có hiệu lực. JAR E72 khoảng 13,1 MB và E6 khoảng 35,1 MB vì giữ đầy đủ nhạc gốc.

Đã kiểm tra đủ luồng MP3, chọn nhạc theo màn, tên map và va chạm client/server ở hai độ phân giải. Cần nghe thử trên Nokia thật để xác nhận khả năng phát âm thanh của máy.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
