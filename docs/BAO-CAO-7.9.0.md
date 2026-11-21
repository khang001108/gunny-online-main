# Gunny Java 7.9.0 – VIP, cánh và đồng bộ thưởng

## Kết quả

Đã sửa trực tiếp client Java ME, backend Java, Web Admin và chuẩn bị SQL bổ sung. Đã build hai JAR/JAD hiện tại; không tạo ZIP Pi, không đẩy Git, không triển khai máy chủ, không đổi gunny.env/Environment/secret Supabase, không xóa dữ liệu người chơi.

### Crash và bạn bè

- `pcWidth(null)` trả 0, `pcShort(null)` trả chuỗi rỗng, hàm vẽ chữ bỏ qua null. Danh sách bạn bè chỉ công bố số lượng sau khi đọc đủ tên/trạng thái từ gói tin; không vẽ dữ liệu đang đọc dở.
- Danh sách bạn bè và danh sách mời cùng dùng trạng thái online server. TCP command socket mở/đóng cập nhật presence; socket sự kiện không làm tài khoản trông như còn online. Ngắt mạng im lặng dùng ngưỡng heartbeat 45 giây; danh sách đang mở cập nhật mỗi 2 giây.

### VIP và vật phẩm

- 12 mức, chi phí từng mức đúng 500/600/700/800/1000/1200/1400/1600/2000/2800/3800/4800, tổng 21.200. EXP lưu dạng tổng; client hiển thị phần EXP trong cấp hiện tại / yêu cầu cấp kế tiếp. EXP dư tự chuyển; VIP12 hiện MAX.
- 13 vật phẩm: thẻ 1–12 cộng 100–1.200 EXP, Full VIP lên ngay 21.200. Không tiêu hao khi đã MAX. Túi chỉ hiển thị số lượng server xác nhận; bấm lặp giữ nguyên vật phẩm/mã giao dịch đang chờ.
- Admin chọn tài khoản, loại thẻ, số lượng để cấp. Full VIP không được bán trong Shop và không có API cấp đồ dành cho client.
- Bật cheat từ tắt sang bật cộng 999 mỗi loại trong 13 loại. Lưu lại khi đang bật không cộng thêm. Tắt rồi bật lại có thể cấp thêm để test, giới hạn 999.999 mỗi loại.
- SQL khóa hàng tài khoản và khóa mã giao dịch; dùng lại mã với nội dung/tài khoản khác bị từ chối. EXP/vật phẩm/tiền/cánh và biên nhận cập nhật trong cùng giao dịch. Full VIP chỉ do service_role qua backend/admin cấp.
- VIP-state gửi bản profile đã xác nhận trong phiên qua TCP, không gọi Supabase theo từng lần kiểm tra 5 giây; Admin cập nhật phiên và đánh thức đồng bộ khi cấp thẻ/cánh.
- VIP vĩnh viễn mới độc lập với thẻ VIP có thời hạn từ túi phó bản cũ; giữ dữ liệu/quyền lợi cũ.

### Cánh, vòng và Bpow

| Cánh | HP khi đeo | Tiền | VIP cần |
|---|---:|---:|---:|
| Bướm | 500 | 200 | 1 |
| Thiên Thần | 1.000 | 500 | 3 |
| Ác Ma | 1.500 | 1.000 | 5 |
| Phượng Hoàng | 2.500 | 2.500 | 8 |
| Huyền Thoại | 4.000 | 5.000 | 12 |

- Shop thêm tab VIP cạnh Đạo cụ, khóa mua khi thiếu VIP. Mỗi cánh mua một lần, vĩnh viễn, nằm trong Thời trang. OK mặc/tháo, thêm ô Cánh ở dưới trái; mỗi lần đeo một cánh. Server trừ đúng tiền và tính HP, không đổi hitbox/vật lý.
- Giữ cả cánh thưởng phó bản cũ, dùng cùng ô trang bị. Phím W đổi cánh phó bản cũng đi qua tuyến trang bị mới.
- Lớp vẽ: vòng VIP → cánh → nhân vật. Dùng atlas có sẵn trong bộ cài, quay vòng 8 khung, cánh 4 khung đóng/mở; tia/sao theo mức. Không tạo ảnh lớn trong vòng lặp vẽ; cache theo cỡ 72/44/24, giải phóng khi chuyển màn và giải phóng hiệu ứng lớn sau Bpow.
- Đã nối vào Túi, Shop, thông tin bạn bè, phòng PvP/phó bản, loading, kết quả, nhân vật trong trận và ảnh lượt đánh. VIP0 không vẽ vòng.
- Bỏ tia vòng Bpow độc lập, giữ zoom. VIP có hiệu ứng tăng sao/tốc độ từ chính vòng của cấp đó; không vẽ trùng vòng của nhân vật ở cảnh nền khi cutin đang hiện.
- Màn dọc 240×320/480×640 xoay khung native và ánh xạ tọa độ chạm về khung 320×240; màn ngang không dùng ảnh đệm xoay.

### Lật thẻ đồng thời và nối lại

- Settlement bắt đầu khi trận kết thúc, không đợi mọi người gửi ACK/nhấn mở thẻ. Client đủ điều kiện vào bộ thẻ khi server xác nhận đã lưu.
- Mỗi người tự nhận lượt của mình; claim và trạng thái thẻ đánh thức đồng bộ TCP cho các máy khác. Giữ chống chọn trùng ô và nhận trùng lượt của storage hiện tại.
- Nhận đủ lượt được về phòng ngay, không phải đợi đồng đội hay đợi mở cả bộ thẻ. Phòng chờ hiển thị người đang nhận thưởng; không bắt đầu/chuyển ải khi đội chưa hoàn tất điều kiện.
- Tà Thần vẫn 3 trận / 9-9-18 thẻ. Khi mọi người quay lại mới sang ải tiếp theo, giữ kết quả Đậu Thần.
- Khi kết nối lại, gắn phiên đăng nhập mới vào ghế cũ, giữ lượt đã chọn và cho nhận tiếp. Khôi phục được cả khi token cũ đã hết hạn nhưng phòng thưởng còn giữ.
- **Giới hạn:** phòng thưởng được giữ trong bộ nhớ server tối đa 30 phút sau kết thúc. Chưa phục hồi giao diện thưởng qua việc khởi động lại server hoặc sau cửa sổ này. Biên nhận/vật phẩm đã cộng vẫn nằm trong database và không bị cộng lại.

## Asset gốc và phần chưa đủ

Đã rà 804 SWF trong bộ source/resource hiện có. Nguồn cụ thể ghi trong `client/VIP-ASSETS.json`.

- Có đúng badge VIP1–9 trong `coreiconandtip.swf`. **Thiếu badge VIP10–12**: dùng chữ VIP10/VIP11/VIP12, không lấy icon cấp khác giả làm icon tương ứng.
- Có 3 mẫu `circlelight/1..3.swf`. Phối thành 12 mức có chuyển động/số tia khác nhau; **chưa có 12 animation vòng gốc riêng biệt** để dùng nguyên bản.
- Có ảnh thẻ VIP chung và vippack; dùng chúng với nhãn V1–V12/MAX. **Chưa có bộ 12 icon thẻ EXP riêng**.
- Cánh dùng ảnh gốc wing027/006/007/005/030; cánh phó bản dùng wing002. Khung đóng/mở tạo từ nửa cánh gốc được phản chiếu, không tải cả timeline Flash trên điện thoại.

Vì thiếu asset trên, phần yêu cầu “đúng đủ 12 icon và 12 bộ vòng gốc” chưa hoàn tất nguyên bản. Có thể thay bổ sung các ảnh đó mà giữ nguyên dữ liệu VIP/gameplay.

## Build và dữ liệu

1. Nếu database đã ở 7.8.x: chạy `supabase/migrations/202611220001_vip_wings.sql` trong SQL Editor trước khi cập nhật. File chỉ thêm trường/bảng/hàm và chuyển cánh cũ đang đeo sang slot mới; không xóa/reset tài khoản.
2. Database cũ hơn dùng `supabase/UPDATE-CURRENT.sql`; cài mới dùng `supabase/GUNNY-ALL.sql`. SQL VIP đã được gộp vào cả hai. Không chạy đồng thời cả ba file.
3. `python client/build.py --tools D:/gunny/GunnyNokia/tools` build CLDC 1.1/MIDP 2.0 và preverify. Cần JDK trong PATH. Hai bộ cài nằm trong `public/downloads`, tên kết thúc Online-790.
4. Backend: `./verify.ps1` trên Windows hoặc `bash verify.sh` trên Linux. `docker build -t gunny-online:7.9.0 .` dành cho Pi/Xubi có Docker; runtime hỗ trợ Java17+. Chưa chạy Docker build trong máy thử này vì không có Docker.
5. Đẩy toàn bộ source, asset, bốn JAR/JAD mới và việc xóa bộ cài cũ lên Git. Xubi Deploy đúng nhánh; kiểm tra `/health` và `/download/` cùng hiện 7.9.0. Không cần thay env/secret/cổng đang dùng.

**SQL chưa được chạy trên Supabase thật**, chưa kiểm tra RPC trong PostgreSQL thật. Đã kiểm tra logic với Memory store và transport Supabase giả lập của bộ hồi quy; cần xác nhận migration trên database thử trước khi đưa production vào dùng VIP.

## Kiểm thử

- 55 lớp kiểm tra backend trong hợp của verify.ps1/verify.sh và DeckRevealCheck: PASS. Bao gồm PvP, party, rời phòng, AI, đường đạn, phó bản cũ/Tà Thần, shop/trang bị, cường hóa, quà, auth, Admin, transport Supabase.
- `Vip790Check`: 68 kiểm tra về mọi ngưỡng cấp, dùng đủ 212 thẻ 100 EXP để đạt VIP12, EXP dư, Full VIP, khóa mua/giá/HP cánh, idempotency, 10 yêu cầu đồng thời cùng mã, quyền Admin và +999 cheat.
- `VipTcp790Check`: 20 kiểm tra TCP thật về capability, số thẻ, dùng/mua/mặc/tháo, gửi lại gói và dữ liệu sau đăng nhập lại.
- `CardsPresence790Check`: 21 kiểm tra TCP thật: hai người nhận đồng thời; P1 về phòng trước; P2 tiếp tục; chấm xanh khi mở/đóng socket; kết nối lại sau ngưỡng dọn 45 giây/3 phút và token hết hạn; retry không cộng trùng; đội chỉ sang boss sau khi cả hai về phòng; tổ đội ghép trận hiện đúng đồng đội nhận thưởng ở phòng trận khác và rebind cả ghế phòng cũ sau reconnect.
- `TribalTcpCheck`: 118 kiểm tra tuyến ba người/ba trận, bắn, reconnect và nhận thưởng. Phần online sử dụng server loopback, không dùng tài khoản thật.
- `Ui790Check`: PASS cả 320×240 và 640×480. Vẽ 12 mức VIP, inventory, cánh, shop khóa/mua, popup mời tên null, bạn bè, Bpow VIP0/VIP12, khung dọc, gói roster server→client, hai phòng chờ. Chạy thêm 6 vòng vào trận/vẽ 60 khung/quay lại Túi, kiểm tra giải phóng cache.
- Hồi quy `Ui788Check`, `Ui789Check`, `ServerSettingsCheck`: PASS hai độ phân giải.
- **Bộ nhớ:** giả lập E72 dùng heap32MB qua các bài trên. E6 dùng heap128MB qua; ép giả lập E6 xuống32MB vẫn OutOfMemoryError, kể cả bài đăng nhập cũ. Không quy kết đây là dung lượng heap thực của Nokia vì MicroEmulator/AWT dùng bộ đệm ảnh khác máy thật.
- Gate schema/phiên bản của updater: 4 bài chỉ đọc qua. Bộ kiểm tra rollback/deploy Linux không chạy đầy đủ trên Windows (thiếu fcntl/fchmod); không sửa bảo vệ quyền file Linux để né lỗi này.
- Node kiểm tra cú pháp Admin JS; Python kiểm tra build/updater; ZIP CRC, version MANIFEST/JAD/HTTP, JAR size, bytecode46 và StackMap CLDC: PASS. Hash bộ cài ở `docs/BUILD-7.9.0.json`.
- **Chưa có Nokia E72/E6/Asha302 vật lý để kiểm tra FPS, heap native và lag mạng thực tế.** Không khẳng định hết văng hoặc hết lag trên thiết bị thật.

Danh sách file thay đổi đầy đủ: `docs/FILES-CURRENT.txt`. Ảnh chụp giả lập nằm trong `client/build/vip790-*.png`, thư mục build được bỏ qua khi đẩy Git.
