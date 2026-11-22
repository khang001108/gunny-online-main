# Gunny Java 7.9.12 — Sulan, ải cuối Bộ Lạc Tà Thần

Đã sửa trực tiếp source và build hai bộ cài hiện tại. Chưa triển khai production, chưa sửa env/secret và không có SQL mới. Dữ liệu người chơi không bị thay đổi. Cập nhật backend và cài lại client cùng bản 7.9.12.

## Diễn biến trận

- Giữ camera ở nhân vật 2 giây, chuyển sang Sulan vàng rơi xuống; rung đất, lời thoại chặn nghi lễ. Tối cảnh, hiện chữ vàng “Thủ lĩnh bộ lạc” theo nhịp giới thiệu Nữ Vương Kiến; tiếng gầm, hiệu ứng nổ rồi mới trả lượt cho đội.
- Sulan vàng chỉ phi giáo lần lượt theo từng người còn sống, tối đa 3 người. Có hai lời thoại khi trúng đòn. Bỏ chu kỳ gọi vật tổ cũ khỏi AI ải này.
- Hạ dạng vàng không kết thúc trận: vỡ mặt nạ, lời thoại mượn thể xác, thức tỉnh dạng đỏ. Lời thoại của Sulan đỏ dùng chữ đỏ. Cây đậu mọc phía trước đội và giới thiệu khả năng giảm sát thương.
- Ba câu chuyện xuất hiện lần lượt khi đến lượt người chơi, không khóa điều khiển. Dạng đỏ có ba câu phản ứng khi trúng đòn.
- Sulan đỏ luân phiên: ba loạt cầu lửa, mỗi loạt nhắm lần lượt từng người còn sống; tiếp theo là giơ tay gây nổ ở tất cả vị trí nhân vật và cây đậu. Có lửa mặt đất và trạng thái cháy đi theo người.
- Cây đậu có lượt riêng sau Sulan đỏ: dùng animation phát sáng `renew` gốc, xóa cả lửa mặt đất và cháy trên người. Không tự hồi sinh khi chết. Khi cả đội bị hạ, cây đậu còn sống cũng không thể kéo dài trận.
- Dạng đỏ chết: lời cuối, nổ lửa, animation cháy đen và rơi mặt nạ gốc rồi mới báo thắng/nhận thưởng.

## Luật sát thương và đồng bộ

`SulanRules.java` dùng chung nguyên byte giữa backend và client. Toàn bộ HP, lượt, người bị nhắm, lửa và lời thoại do server quyết định. TCP tiếp tục dùng snapshot/chunk/replay hiện có; thêm phần dữ liệu giới hạn kích thước sau khi hai bên thỏa thuận khả năng `sulan7912`. Chặn trộn hai kiểu gói trong phòng Tà Thần qua join, lời mời, tìm đội nhanh và reconnect.

Các trị số chưa có công thức gốc trong ảnh là cân chỉnh Java ME, không tuyên bố khôi phục được công thức server PC:

| Luật | Dễ | Thường | Khó |
|---|---:|---:|---:|
| HP cây đậu | 1.800 | 1.200 | 700 |
| Lửa hung tàn đánh cây đậu | 260 | 430 | 720 |
| Cháy mỗi giây | 30 | 55 | 80 |

Nhân vật đứng cách cây đậu sống tối đa 75 đơn vị được giảm 50% sát thương lửa. Cây đậu chỉ nhận 20% sát thương từ đòn của đồng đội. Không còn cửa sổ nhân ba sát thương lên Sulan của logic cũ. Cháy không cộng dồn nhiều lần trên một người trong cùng giây; cây đậu dập toàn bộ trạng thái cháy. Nhịp cháy chạy theo clock server, không tăng tốc bởi gói thao tác nhiều lần. Trận vẫn giữ giới hạn 200 lượt.

## Asset và bộ nhớ

- Dùng atlas gốc đã có từ `living121.swf`, `living122.swf`, `living123.swf`, giáo và hiệu ứng nổ/lửa có sẵn. Không dùng ảnh tham khảo làm nền chiến đấu.
- Mảnh mặt nạ lấy bitmap 3 của `living121.swf`. Animation chết đỏ dùng đúng hàng `die`, bỏ frame trả về tư thế đứng cuối timeline.
- Chữ tiêu đề ghép từ glyph vàng đang dùng của game; lời thoại đỏ đổi bảng màu glyph gốc. Script tái tạo và thông tin nguồn ở `client/tools/sulan_assets.py` và `client/source-assets/tribal797/sulan-text.json`.
- Tối đa 9 vùng lửa và 7 trạng thái cháy; cache dòng thoại khi đổi câu, giải phóng sprite Sulan vàng sau chuyển dạng và giải phóng cache khi rời trận. Không cấp phát particle mới theo từng frame render.

## Kiểm tra đã chạy

- `verify.ps1`: qua toàn bộ bộ kiểm tra backend hiện có, gồm TCP, dungeon, reward retry, reconnect, VIP/cánh, cheat, phòng, PvP và các hợp đồng lưu trữ giả lập. Log cục bộ `backend/build/verify-7912.log`.
- `Sulan7912Check`: 144 kiểm tra cảnh mở đầu, hai dạng, 1–3 người, số lượt phi giáo/cầu lửa, cây đậu và độ khó, 20% sát thương đồng đội, cháy một giây, miễn tăng tốc clock từ input, ba đoạn chuyện không khóa điều khiển, cảnh chết và phần dữ liệu TCP mới.
- `UiSulan7912Check`: chạy **hai JAR phát hành thực tế** ở 320×240 heap 32MB và 640×480 heap 128MB; render cảnh mở đầu, chuyển dạng, chữ đỏ, cây đậu, lửa, nổ, cháy đen; đọc snapshot, chunk, hồi phục gói và TCP thật. Kiểm tra lại bộ chọn map, yêu cầu cấp và PvP.
- Compile/preverify CLDC 1.1/MIDP 2.0, kiểm tra phiên bản manifest/JAD, kích thước và các class/asset trong JAR. Hash nằm ở `docs/BUILD-7.9.12.json`. `.dockerignore` giữ đúng bộ cài 7912 để ảnh Docker cung cấp đúng bản tải mới.

Chưa đo FPS hay chạy trên Nokia E72/E6/Asha vật lý, chưa thử trên máy chủ đang hoạt động. Đây là bản thích nghi animation/nhịp chơi cho Java ME, không phải trình chạy Flash PC nguyên bản.
