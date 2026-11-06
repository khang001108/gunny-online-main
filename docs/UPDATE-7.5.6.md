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

## Bản vá chẩn đoán trang bị

Server phân biệt mã lỗi RPC thay vì coi mọi HTTP 404 là thiếu API. PGRST202 là không tìm được chữ ký hàm trong cache API; P0002 là không có dòng dữ liệu cho truy vấn strict; 42501 là lỗi quyền; 42883 là một hàm được gọi không tồn tại. Game hiển thị tên API/mã lỗi, Render Logs có dạng `Supabase RPC gunny_equip HTTP 404 code ...`, không ghi request, mật khẩu hay API key. Thay đổi này sửa chẩn đoán; chưa kết luận hay tự sửa nguyên nhân ở Supabase thật.

Chỉ cần deploy lại server, không cài lại JAR và không chạy lại SQL gộp. Nếu lỗi vẫn xuất hiện, thử mặc đồ một lần rồi lấy dòng mã lỗi trong Render Logs hoặc popup. File `supabase/diagnose-equip.sql` chỉ đọc cấu trúc/quyền, không thay đổi tài khoản hay trang bị.
