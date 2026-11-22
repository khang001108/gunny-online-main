# Gunny Java 7.9.17 — Crash RoomLoadout.hud chỉ số81

Log người dùng gửi chạy7.9.15. Đã vá trên source hiện tại, giữ các tối ưu7.9.16 và build hai JAR/JAD7.9.17. Chưa deploy; không có SQL/env/secret mới, không đổi đạo cụ hay dữ liệu tài khoản trên server.

## Lỗi và cách xử lý

`RoomLoadout.hud` trước đây tra `ITEM_COST[u.kit[k]]` mà không kiểm tra mã. Mảng có6 phần tử; giá trị81 làm văng luồng vẽ. `grayIcon` và bước kiểm tra thể lực trong `use` cũng có đường truy cập tương tự. Chưa đủ bằng chứng từ log để kết luận tại sao dữ liệu lại có mã81.

- HUD lấy mã đạo cụ vào biến cục bộ, kiểm tra0–5 trước khi truy cập mảng hoặc icon. Khi mã chưa hợp lệ, vẫn vẽ ô/phím, không vẽ icon sai. Không sửa ngầm thành vật phẩm khác.
- Nút sử dụng kiểm tra ô0–2 và mã đạo cụ trước khi tra thể lực hoặc gửi lệnh. Mã lỗi không trừ thể lực và không gửi command. `grayIcon` cũng kiểm tra giới hạn.
- Decoder đọc đủ ba mã vào biến cục bộ, kiểm tra phạm vi và cấu hình không trùng trước khi công bố vào bộ trang bị. Khung lỗi bị từ chối bằng IOException, không đưa81 vào trạng thái đang vẽ. Thông báo gồm ba mã, chỉ số nhân vật và lượt để hỗ trợ chẩn đoán nếu tái diễn; không ghi token/mật khẩu.
- Kiểm tra ID nhân vật đang đánh, NPC và vũ khí trước khi công bố; chỉ công bố active actor sau khi đã nhận phần số lượng/NPC. Giảm nguy cơ hiển thị dữ liệu chưa đầy đủ khi khung bị lỗi.
- Cả snapshot đầy đủ và luồng chunk có thể yêu cầu phục hồi bằng snapshot đầy đủ trên TCP. Thử tối đa2 lần; nếu dữ liệu vẫn hỏng thì báo lỗi kết nối có kiểm soát, không để lỗi chỉ số mảng thoát lên luồng render. Không gửi lại thao tác thưởng/mua/sử dụng trong bước phục hồi.

## Kiểm tra

- `UiHud7917Check`, hai **JAR phát hành thực tế**:320×240 heap32MB và640×480 heap128MB.
- Chèn mã6/81/255 vào snapshot server được serialize thật: decoder từ chối và không công bố mã lỗi cho renderer. Gói đầy đủ có81 làm client yêu cầu phục hồi, giữ đăng nhập; gói sửa hợp lệ khôi phục trạng thái đạo cụ.
- Đặt trực tiếp-1/6/81/255 vào từng ôZ/X/C: HUD không văng; nút sử dụng không trừ năng lượng/gửi lệnh. Kiểm tra ô ngoài0–2 và `grayIcon` ngoài mảng.
- Kèm kiểm tra snapshot/chunk/TCP, cache, mapping font, slot NPC, phó bản, phòng/map, PvP và các chức năng của7.9.16.
- `verify.ps1` qua; log cục bộ `backend/build/verify-7917.log`. Compile/preverify CLDC/MIDP; manifest/JAD và class đóng gói được đối chiếu. Hash ở `docs/BUILD-7.9.17.json`.

Chưa thử trên Nokia vật lý hoặc production. Cần cài lại **JAR7.9.17**, không chỉ thay JAD; bản7.9.15/16 không có bản vá này.
