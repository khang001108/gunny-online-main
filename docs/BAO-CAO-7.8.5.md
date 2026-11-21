# Gunny Java 7.8.5

Đã sửa source và build client/server. Chưa cập nhật Pi 5 hoặc chạy SQL trên Supabase thật. Chưa đo FPS/độ trễ trên E72 vật lý; các mức giảm dưới đây là số byte của lượt kiểm thử, không phải tỷ lệ giảm lag.

## Giảm tải đồng bộ

- Địa hình máy nhận giữ nguyên mảng ảnh, chỉ đục những hố mới theo revision; không dựng lại cả map ở mỗi đoạn hành động. Máy bắn vẫn đối chiếu kết quả cuối với server.
- Client/server thương lượng cờ delta. Mỗi đoạn bắt đầu bằng snapshot đầy đủ, sau đó chỉ truyền những byte thay đổi khi nhỏ hơn bản đầy đủ. Giữ cơ chế nhận trước và xác nhận đã phát xong; không bỏ khung, va chạm hoặc lượt.
- Bộ giải mã giới hạn chiều dài, kiểm tra vùng sửa và từ chối dữ liệu thiếu/sai. Máy cũ tiếp tục dùng định dạng cũ; danh sách phòng, ghép đội, mời và kết nối lại không trộn hai định dạng.
- Lượt 6 nhân vật đã đo: bắn 34.846→11.915 byte (66%); di chuyển 23.854→4.996 (80%); POW 127.668→28.094 (78%). Một lượt Tà Thần 13 đơn vị: 145.684→32.664 (78%). Các lượt sinh ngẫu nhiên nên số byte mỗi lần chạy có thể khác.
- Video cho thấy hai máy lệch thời điểm hiển thị. Không thể kết luận toàn bộ nguyên nhân từ video. Cần cài cả hai client mới và backend mới rồi kiểm tra lại trên điện thoại thật/mạng thật.

## Đá thưởng

Tỷ lệ dưới đây áp dụng cho thẻ đá ở Huyệt Ma Kiến và Bộ Lạc Tà Thần, không thay vị trí thẻ cánh/VIP cuối Tà Thần. Huyệt Ma Kiến offline cũng dùng cùng bảng tỷ lệ, lưu đúng từng cấp đá vào RMS riêng của offline.

| Độ khó | Lv1 | Lv2 | Lv3 | Lv4 | Lv5 |
|---|---:|---:|---:|---:|---:|
| Dễ |65%|25%|8%|2%|0%|
| Thường |25%|35%|25%|12%|3%|
| Khó |5%|15%|30%|35%|15%|

Server/SQL chọn cấp đá một lần và lưu biên nhận. Gửi lại yêu cầu, mở lại cùng thẻ không cộng thêm. Giữ chính xác số đá Lv1 đang có kể cả khi ví đã chạm trần 999.999. Biên nhận cũ đã nhận trước cập nhật giữ nguyên cấp Lv1.

## Cheat tài khoản test

Trong `/admin/`: chọn tài khoản → bật **Cho phép dùng cheat bằng phím G** → **Lưu quyền cheat**. Tài khoản đăng nhập lại, nhấn G. G của người không được cấp quyền vẫn mở chat.

- Full đá Lv1–5 và hai loại bùa: 999.999.
- Nâng trang bị sở hữu có hỗ trợ cường hóa lên +15.
- Cấp nhân vật 70.
- Xu chiến đấu và tiền mua sắm: 999.999 mỗi ví.
- Bật/tắt bất tử; áp dụng chiến đấu server, cả sát thương quái và rơi ngoài map.
- Qua ải phó bản đang đánh; hủy đoạn bắn cũ, lưu chiến thắng và nhận thưởng theo luồng thật.

Server kiểm tra quyền từ kho dữ liệu, không tin cờ trên client. Thu hồi quyền đồng thời tắt bất tử. Đá/tiền/trang bị đã cấp là thay đổi thật, không tự hoàn lại khi tắt quyền.

## Cập nhật và build

1. Supabase SQL Editor: chạy một file `supabase/UPDATE-7.7.0-TO-7.8.5.sql` cho database đã có 7.7.0 trở lên, kể cả đã chạy 7.8.0/7.8.4. File bao gồm cường hóa, Tà Thần, quyền cheat và đá phân cấp. Không chạy thêm các migration lẻ. `GUNNY-ALL.sql` dành cho cài mới.
2. Chép gói Pi mới và build lại dịch vụ hiện tại từ Dockerfile, hoặc chạy JAR đã kèm bằng `bash deploy/pi5/run-785.sh` với biến môi trường sẵn có. Giữ file `gunny.env` và khóa Supabase hiện tại; gói không chứa khóa hay file môi trường thật.
3. Cài JAR/JAD 7.8.5 tương ứng trên tất cả máy cùng chơi. Không cần đổi tài khoản hay xóa dữ liệu.

Server: JDK17+, `javac --release 17 -encoding UTF-8 -d backend/build backend/src/*.java`. JAR kèm chỉ chứa class sản xuất, không có test.

Client trong repo: `python client/build.py --tools <thư mục ECJ/ProGuard/CLDC/MIDP>`; cần Java trong PATH. Script giữ asset từ bộ cài 7.8.5 có sẵn và preverify CLDC1.1/MIDP2.0. Trên máy phát triển còn có `D:/gunny/GunnyNokia/build-785.ps1` để xuất lại toàn bộ tài nguyên và bộ cài.

## Kiểm thử

- `verify.ps1`: toàn bộ suite hiện có chạy qua trước đóng gói; chạy bổ sung Online785Check, TribalTcpCheck và các test bị ảnh hưởng sau sửa cuối.
- 24 kiểm tra cheat/admin/quyền, đá và nhận thưởng trùng; 86 kiểm tra luật Tà Thần; 162 kiểm tra vòng đời TCP tổ đội qua 4 ải/kết nối lại/nhận thưởng; 205 kiểm tra expansion; 519 kiểm tra gameplay.
- 1.002 ca delta round-trip/giải mã sai, cùng phát lại đường bắn thật 2 người/6 người/13 đơn vị. Kiểm tra đạn không xuyên đất, đủ nổ/hố và kết thúc lượt; mảng địa hình không bị cấp phát lại theo khung.
- Giả lập 320×240 và 640×480: menu G qua TCP, quyền mặc định, năm thao tác hồ sơ, qua ải tới bảng kết quả, thu hồi quyền; kiểm tra PvP trở về phòng và hoạt ảnh Kiến không hỏng.
- Admin JavaScript qua kiểm tra cú pháp. JAR/JAD kiểm tra CRC, phiên bản, kích thước khớp và preverify.

Giới hạn: SQL chưa thực thi trên PostgreSQL/Supabase thật; TCP dùng kho dữ liệu trong bộ nhớ trong các bài kiểm thử. Chưa triển khai production hoặc kiểm tra mạng di động/E72 vật lý, nên chưa thể xác nhận đã hết giật trên máy thật.

## Bản vá admin LAN

Sửa đăng nhập HTTP nội bộ (đúng Origin/Host và cổng), cookie phiên LAN, giữ Secure cho HTTPS. Không cho phép HTTP tên miền công khai hoặc yêu cầu khác Origin. Thêm fallback tạo UUID bằng crypto.getRandomValues để quản lý tài khoản/cộng tiền hoạt động trên HTTP LAN. 26 kiểm tra HTTP dùng cấu hình production, suite admin cũ và 24 kiểm tra cheat đều qua; 1.000 UUID fallback hợp lệ, không trùng. Chỉ cập nhật backend và public/admin; không cần SQL hoặc JAR điện thoại mới.
