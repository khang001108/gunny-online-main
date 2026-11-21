# Gunny Java 7.9.3 — kiểm tra cục bộ, chưa deploy

Bảng xếp hạng dùng huy hiệu cấp từ `PhoneCanvas.levelIcon` và icon vũ khí đang có trong `PhoneCanvas.weapons`. Đã bỏ ngôi sao rèn và màu riêng tự thêm vào từng hạng. Giữ khung cửa sổ gốc, danh sách sáng, phím cuộn và xem hồ sơ; loại tài khoản bật cheat.

## Quy đổi đã thực hiện

- VIP đầy: 100 EXP = 100 xu mua sắm. Khi thẻ vừa đủ đạt MAX, phần EXP dư cũng đổi xu. Thẻ Full VIP khi đã MAX đổi 9.500 xu. Không hạ EXP cũ của người chơi.
- Trong Túi, OK vào đá rồi xác nhận Đổi: mỗi đá Lv1/2/3/4/5 đổi 100/200/400/800/1.600 xu mua sắm. Mềm trái vẫn xem thông tin. Hủy không trừ đá.
- Server quyết định số xu, trừ vật phẩm và cộng ví trong cùng giao dịch. Mã giao dịch chống nhận trùng khi gửi lại. Ví vượt 999.999 thì giữ nguyên vật phẩm và báo đầy ví; không báo sai mật khẩu.
- Đá có thể mua trong Shop với giá cũ 80/160/320/640/1.280. Vì tỷ lệ đổi người dùng yêu cầu cao hơn giá mua, mua rồi đổi lại sẽ tăng xu. Chưa tự sửa giá Shop hoặc thêm điều kiện hạn chế ngoài yêu cầu.

## Các thay đổi đang cùng bản này

HP gốc cấp70 = 30.000; loading gửi phần trăm từng người bằng TCP; chờ kết quả2 giây rồi10 giây chọn thẻ, hết hạn server tự nhận phần còn thiếu trước khi về sảnh/phòng. Người rời trước không làm mất thưởng đồng đội. Thêm một số bot cấp60–70 có đồ+15 và cánh. Đổi lối vào offline thành xếp hạng online; hiển thị đăng nhập sau câu hỏi âm thanh. Shop OK thử cánh, B/nút Mua mới mua; Túi hai lần OK chọn rồi mặc cánh. Điều chỉnh cánh theo điểm vũ khí và tâm Bpow.

## SQL trước khi cập nhật Xubi

Nếu database đã có VIP9 của7.9.2, chạy lần lượt:

1. `supabase/migrations/202611230001_stats793.sql`
2. `supabase/migrations/202611230002_conversion793.sql`

Hai migration cũng đã gộp vào `supabase/UPDATE-CURRENT.sql` và `supabase/GUNNY-ALL.sql`. Không có env/secret mới. Không xóa dữ liệu người chơi. Chưa chạy SQL trên PostgreSQL/Supabase thật trong phiên này; máy hiện tại không có công cụ PostgreSQL để kiểm tra trực tiếp. SQL cần được thử ở database thử trước khi cập nhật server thật.

## Kiểm thử

- `verify.ps1`: toàn bộ script qua, gồm quy đổi, VIP/cánh, TCP, Admin, Shop, thưởng, phó bản và các kiểm tra hồi quy hiện có.
- `Conversion793Check`:46 kiểm tra gồm13 loại thẻ, dư EXP,5 cấp đá, gửi lại, ví đầy, cập nhật gói inventory.
- `Ui793Check`: hai JAR thực7.9.3,320×240 với heap32MB và640×480 với heap128MB. Kiểm tra icon gốc, xác nhận/hủy đổi đá, Shop/Túi, loading, thưởng và xoay màn hình.
- JAR đã compile/preverify; manifest/JAD7.9.3, kích thước và đường dẫn khớp. Hash trong `docs/BUILD-7.9.3.json`.
- Chưa thử máy Nokia vật lý hoặc đo FPS thực; chưa deploy production.

## Còn thiếu từ yêu cầu trước

Tỷ lệ phát thẻ EXP VIP1–9 trong túi phó bản chưa thay: dãy60%/50%/.../5% cần chốt cách hiểu vì tối đa một thẻ hoặc không nhận. Quy tắc cũ vẫn giữ nguyên; không tính mục này đã hoàn thành. Phần offline đã bỏ đường vào và mô phỏng phó bản riêng, nhưng vẫn giữ các hàm vật lý/renderer dùng chung cho online.
