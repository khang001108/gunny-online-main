# Gunny Java 7.9.8

Đã chỉnh source và build JAR/JAD E72/E6. Chưa deploy production, không có SQL/env/secret mới.

## Ải đầu Bộ Lạc Tà Thần

- Mở đầu: chiến sĩ lùi về sau, lính bộ lạc đi lên; tiếp đến hành động di chuyển và phi lao của chiến sĩ. Dùng pose đi bộ gốc, cập nhật nhịp chân cả phần di chuyển được kéo dài.
- Lượt lính gọi thêm tối đa5 con, cộng dồn không quá10 lính đang sống. Giữ mục tiêu20 lính của ải; các lượt gọi không vượt tổng số cần thiết để hoàn thành mục tiêu.
- Chiến sĩ tiến hoặc lùi theo lượt rồi ném3 lao theo quỹ đạo cong. Chết được hồi sinh ở vòng kế tiếp khi mục tiêu lính chưa hoàn tất; giết lại chiến sĩ không trừ thêm mục tiêu20 lính. Sau khi hoàn tất lính và hạ chiến sĩ cuối, ải vẫn kết thúc bình thường.
- Kéo dài bước đi của lính thêm12 nhịp; giữ nguyên mốc kỹ năng của Bulan/Sulan ở các ải khác.
- Vệt bay phi lao dùng sprite trail của vũ khí nhân vật đang có trong source, thay hình chữ nhật/hạt tự vẽ. Giữ hình phi lao NPC và xoay theo vận tốc, quỹ đạo do server quyết định.

## Phòng chờ

- Chọn phó bản theo hai thẻ nằm trong lưới; OK chọn thẻ, chọn độ khó, OK đến Xác nhận, OK mới gửi yêu cầu đổi map. Có điều khiển chạm tương ứng.
- Ô thứ tư thay ảnh ải bằng khung nguồn toolbar và các ô thưởng nguồn Túi, trình bày dạng lưới; hiển thị đá, đồ hiếm, túi quà và tỷ lệ từng món theo ải. Chiều cao khung thưởng80 bằng các ô chờ đã kéo dài từ68 lên80.
- Mini map lấy banner ngang theo phó bản đang chọn, không thay ảnh theo từng ải sau khi vượt màn. Dòng LC đề cử đổi thành Ải:1-2 hoặc1-3.
- Đổi map khi đang có tiến trình trong phòng chờ phải xác nhận cảnh báo mất tiến trình; hủy giữ nguyên, tiếp tục mới mở bảng chọn, xác nhận cuối mới gửi thao tác reset. Vẫn chặn đổi map khi trận đang diễn ra để không bỏ dở phần thưởng/gói hành động.

## Kiểm tra

- `Raid798Check`: mở đầu lùi/đi bộ, hai đợt lên10 lính và giữ giới hạn, hồi sinh không giảm mục tiêu, di chuyển rồi ném3 lao; kiểm tra lại luật hoàn thành cả phó bản.
- `verify.ps1`: toàn bộ script server qua, gồm các kiểm tra phó bản, chiến đấu TCP, phần thưởng, đăng nhập, Shop, VIP, cheat và hồi quy. Log tại `backend/build/verify798-release.log`.
- `UiTribal798Check`: hai JAR thực320×240 heap32MB và640×480 heap128MB; các ải3/4/6, gói NPC14/13 đúng byte, native animation/payload, loading, reconnect, PvP. Kiểm tra thêm chọn độ khó qua hai bước OK, cảnh báo/hủy đổi map, chỉ gửi reset khi xác nhận cuối và ảnh mini map giữ nguyên qua màn.
- Đã xem ảnh phòng chờ và chọn thẻ từ giả lập. Compile/preverify thành công. Manifest/JAD, kích thước và hash tại `docs/BUILD-7.9.8.json`.

Chưa thử Nokia vật lý hoặc đo FPS thực. Không xóa dữ liệu người chơi và không chạy SQL trên database thật.
