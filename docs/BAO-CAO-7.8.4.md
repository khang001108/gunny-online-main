# Bản kiểm thử 7.8.4 — Bộ Lạc Tà Thần và cường hóa

Đã sửa trực tiếp source client/server trong repository. Chưa triển khai lên Pi 5, chưa chạy SQL trên Supabase thật và chưa kiểm tra trên điện thoại vật lý.

## Nội dung đã lập trình

| Phần | Hành vi |
| --- | --- |
| Phòng | Bộ Lạc Tà Thần trong hộp tạo phòng và chọn phó bản; 1–3 người thật; mời, sẵn sàng, bắt đầu; Dễ/Thường/Khó |
| Ải 1 | Rừng nguyên sinh; thổ dân giáo, ném lao, tinh nhuệ; 2/3/4 đợt theo độ khó; diệt hết để thắng |
| Ải 2 | Đậu Thần có HP riêng; quái ưu tiên tấn công NPC; hồi máu nhóm cứu NPC; NPC chết thì thua; sống qua các đợt thì đi tiếp |
| Ải 3 | Hai Bulan có HP riêng; hồi sinh người đã chết sau hai vòng nếu người còn lại sống; vật tổ Xiềng Xích khóa góc, Mỏi Mệt trừ thể lực; bắn phá để gỡ trạng thái |
| Ải 4 | Sulan đổi dạng và hồi thanh HP khi bị hạ lần đầu; dạng sau tăng sát thương, giảm sát thương nhận và bắn ba tia; vật tổ hồi HP, vật tổ nổ cảnh báo hai vòng; Đậu Thần hỗ trợ nếu đã cứu ở ải 2 |
| Kết quả | Hoạt ảnh kết thúc trước bảng thưởng; xu và EXP theo độ khó; hai lượt mở thẻ/người; đi tiếp sau khi đội nhận xong |
| Thưởng | Đá 10–100 / 50–200 / 100–300; bộ thẻ cuối Thường có 3 thẻ VIP Thương Cổ, Khó có 6 thẻ VIP và đúng 1 thẻ cánh; các thẻ còn lại là đá |
| Lưu và kết nối lại | Checkpoint theo lượt và khi kết thúc; lưu HP, trạng thái, giai đoạn và đội trong Supabase; đăng nhập lại gắn về đúng chỗ trong phòng đang tồn tại, giữ HP |
| Nhận thưởng | Kết quả và vật phẩm do server xác định; khóa giao dịch theo trận; biên nhận theo người/lượt và thẻ duy nhất; gửi TCP lại không cộng thêm |

AI có trạng thái IDLE, SELECT_TARGET, MOVE, AIM, ATTACK, SUMMON, SPECIAL, DEAD. Dùng lại `Battle`, vật lý đạn, va chạm, lượt, `Room` và TCP; không thay bằng trận giả hoặc hệ chiến đấu riêng. Đáy nền không bị phá; các phần đất phía trên dùng hệ đục đất hiện có.

Cường hóa đổi từ tích EXP sang xác suất. Giữ trần +15; thất bại đến +4 giữ cấp, từ +5 giảm một cấp nếu thiếu bùa bảo vệ. Có đá 1–5, dùng 1–3 viên/lần; bùa may mắn tăng tương đối 25%, giá 300 tiền mua sắm; bùa bảo vệ giá 500. Đá/bùa tiêu hao cả khi thất bại. Bảng xác suất là thông số cân bằng mới, không được xác nhận là bảng của Gunny PC gốc. Công thức và giao dịch được server/SQL kiểm tra lại.

Đã thêm spawn PvP ngẫu nhiên, tăng tốc mô phỏng bay đạn 25% mà giữ các bước vật lý, giảm bán kính đục đất khoảng 10%, sửa hoạt ảnh Nữ Vương khi trúng tiếp trong cùng lượt, và hiển thị cấp trang bị của đúng người đang xem.

Giảm tải bằng cách bỏ việc đọc lại ảnh nhân vật mỗi lần cập nhật hồ sơ không đổi trang phục; cache nút Tiệm rèn; không tải hình người ở ô NPC/ô trống của phó bản; giữ cơ chế tải trước khung bắn của repo mới nhất; giải phóng ảnh theo ải và đóng luồng nhạc khi chuyển bài. Nhạc bốn ải lấy từ tài nguyên 1124, 1125, 1126, 1122, chỉ tải bài đang dùng.

## Sử dụng và cấu hình

- Trong hộp tạo phòng viễn chinh: phím **1** chọn Kiến, **2** chọn Tà Thần; chạm tên phó bản để đổi. Có thể đổi và chọn độ khó qua phần chọn phó bản trong phòng trước ải đầu.
- Trong Túi: có cánh thì phím **W** mặc/tháo; trạng thái lưu server, mọi người trong trận nhìn thấy. Cánh không bán trong shop.
- `backend/tribal.properties` gom HP, sát thương, số đợt, thời gian hồi sinh và khoảng thưởng đá. Đặt `GUNNY_TRIBAL_CONFIG` tới file này; khởi động lại server sau khi chỉnh.
- Chạy **một file nâng cấp** `supabase/UPDATE-7.7.0-TO-7.8.4.sql` trước khi thay server. Dành cho database đã có bản 7.7.0 trở lên; không thay bằng file tạo mới. `GUNNY-ALL.sql` dành cho cài mới.
- Không tự chạy SQL, sửa file secret hoặc cập nhật máy chủ đang hoạt động. Tạo phòng Tà Thần kiểm tra API lưu dữ liệu trước, để báo thiếu SQL ngay.

## Build

Server cần JDK 17 trở lên, tại gốc repo:

```sh
mkdir -p backend/build
javac --release 17 -encoding UTF-8 -d backend/build backend/src/*.java
java -Djava.awt.headless=true -cp backend/build GunnyServer
```

Trong gói Pi có sẵn `gunny-server-7.8.4.jar`. Sau khi cung cấp biến môi trường Supabase hiện có, chạy `bash deploy/pi5/run-784.sh`; script không sửa file môi trường. Nếu dùng dịch vụ cũ, làm theo `deploy/pi5/README.md`, đồng thời chép file cấu hình mới đến đúng đường dẫn.

Client trong repo giữ tài nguyên từ bộ cài 7.8.4 đã kèm, cần ECJ, ProGuard và thư viện CLDC/MIDP:

```powershell
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Source tài nguyên gốc đầy đủ trên máy hiện tại còn ở `D:/gunny/Resource`. Tại `D:/gunny/GunnyNokia`, chạy `./build-784.ps1` để xuất lại asset mới, nhạc, preverify và tạo bộ cài trong `dist/784`. Tên ứng dụng/vendor giữ như cũ để giữ dữ liệu RMS.

## Kiểm tra đã chạy

- 86 kiểm tra luật Tà Thần: đợt quái, hồi NPC, thất bại, hồi sinh hai Boss, phá vật tổ, biến dạng, nổ trì hoãn, thưởng và nối lại chỗ chơi.
- 162 kiểm tra vòng đời TCP thực với ba kết nối tài khoản: tạo/join/ready/start, bắn thật, bốn lần chuyển cảnh, đăng nhập lại, thẻ dùng chung, gửi nhận thưởng lặp và thoát. Các lần hạ quái để kiểm tra chuyển ải dùng trạng thái kiểm thử được điều khiển; không phải một lượt chơi cân bằng đầy đủ từ đầu đến cuối.
- Kiểm tra xác suất cường hóa, tiêu hao, chống gửi lặp/đồng thời, giảm cấp/bùa, spawn thay đổi.
- Huyệt Ma Kiến: 205 kiểm tra hiện có; bộ kiểm tra TCP PvP, trả phòng, đạo cụ, trang bị, khung bắn, tải trước, UI người chơi và tải bộ cài.
- Chạy các bộ kiểm tra trong `verify.ps1`; sửa fixture cũ để phản ánh bán kính đào đất, tên phòng và bộ cài mới. Không bỏ kiểm tra chức năng.
- Chín bộ kiểm tra giao diện client hiện có qua ở 320×240 và 640×480. Chụp thật renderer từ snapshot TCP của cả bốn ải mới và hai ải Kiến, ở hai kích thước. Kiểm tra cường hóa và animation Nữ Vương lặp ở cả hai.
- Build CLDC 1.1/MIDP 2.0, preverify, kiểm tra ZIP/CRC, phiên bản và kích thước JAD/JAR; xác nhận các dòng nhạc MP3.

## Giới hạn và tài nguyên cần hoàn thiện

1. Chưa chạy migration trên PostgreSQL/Supabase thật; kiểm tra lưu thưởng hiện tại dùng kho kiểm thử có giao dịch và chống nhận trùng. Cần xác nhận migration và một trận thật sau khi triển khai.
2. Chưa đo FPS, bộ nhớ và mạng trên Nokia E72, E6, Asha 302 vật lý; không khẳng định đã hết giật trên máy thật. Giao diện đã kiểm tra theo hai độ phân giải của các máy này.
3. Bản đồ dùng nền gốc 1124/1125/1126/1122, dựng vùng đất chơi phù hợp engine hiện tại. Sprite dùng bitmap gốc Living115–118, 124–127, 129; ánh xạ Đậu Thần/Bulan/Sulan và các tư thế đang là bản tạm hợp lệ. Cần database NPC template gốc và bộ hoạt ảnh đầy đủ để xác nhận đúng từng nhân vật, chuyển dạng, trúng đòn/chết. Cánh dùng ảnh wing002; chưa có bộ chuyển động cánh đầy đủ.
4. Nối lại hỗ trợ khi tiến trình server và phòng vẫn còn. Có checkpoint trong database, nhưng chưa khôi phục trận đang đánh sau khi khởi động lại toàn bộ server. Phòng không còn ai kết nối sẽ hết hạn theo cơ chế dọn phòng hiện có.
5. Chỉ số, vòng hồi sinh, giá bảo vệ và bảng xác suất mới cần chơi cân bằng thêm; chưa xác nhận tương đương nguyên bản PC.

## File chính

- Mới: `backend/src/TribalRules.java`, `TribalDungeon.java`, `TribalConfig.java`, `TribalMemory.java`, `Crafting.java`; hai migration ngày 20261121; cấu hình `backend/tribal.properties`; kiểm tra `TribalCheck`, `TribalTcpCheck`, `CraftingChanceCheck`; `client/src/TribalRules.java`, `TribalArt.java`.
- Sửa combat/phòng/TCP/lưu trữ: `Battle`, `GunnyServer`, `Dungeon780`, `Expansion780`, `CloudStore`, `TcpGameServer`, `SocialServer`.
- Sửa UI/client: `OnlineClient`, `DungeonScreen`, `OnlineCreate`, `OnlineResult`, `RewardCards780`, `ForgeScreen`, `ForgeRules`, `EconomyInventory`, `StoneShop`, `ShopScreen`, `ShopCatalog`, `BagScreen`, `PhoneCanvas`, `MapPicker`, `OnlineSocial`, `ItemTooltip`, `OnlineVisuals`, `OnlineMotion`, `OfflineDungeon`, `GamePresentation`.
- Tài nguyên: `backend/assets/land8..11.png`, `hard8..11.dat`; asset/nhạc client nằm trong JAR; file tạo asset ở thư mục GunnyNokia trên máy hiện tại. Thêm bộ cài 784, cập nhật trang tải cục bộ và các script build/kiểm tra. Không push hay triển khai.

Danh sách file đầy đủ: [FILE-CHANGES-7.8.4.txt](FILE-CHANGES-7.8.4.txt).
