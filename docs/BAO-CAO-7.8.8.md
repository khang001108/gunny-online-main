# Gunny Java 7.8.8

## Kết quả

- Thẻ chưa chọn sau khi mở cả bộ không còn chữ “Chỉ xem”. Hiện xu/số thưởng; lần đồng bộ phòng tiếp theo giữ nguyên cả loại quà lẫn số lượng. Những thẻ chưa chọn chỉ để xem, không cộng vào túi.
- Shop Đạo cụ đi theo cột/hàng như các tab khác. Có chuyển trang bằng nút mũi tên; lên từ nút trang quay về ô hợp lệ. B mở xác nhận mua, nút ghi Mua(B). Bảng thông tin vẽ sau ví/nút mua để luôn nổi trên cùng.
- Chủ phòng chờ chọn lại Kiến/Tà Thần được từ mọi ải; vẫn chặn khách và phòng đang đánh. Phòng mới bắt đầu ở ải 1. Đổi phó bản cũng đổi bản đồ chiến đấu và xóa trạng thái hỗ trợ Đậu Thần của lượt cũ.
- Khung xem trước thưởng có viền; nhân vật được dựng đúng kích thước và giới hạn trong ô, cấp ở bên cạnh phía dưới. Áp dụng cả offline và online. Minimap chỉ có Độ khó / Cấp / LC đề cử.
- HP gốc = 2000 + floor(3000 × (cấp − 1) / 69). Cấp 1: 2000, cấp 70: 5000. HP trang bị/cường hóa cộng thêm riêng; tăng Thủ theo cấp không bị cộng lặp vào HP. Client/server/Túi dùng cùng công thức. Không sửa HP cấu hình của quái/Boss.

## Source chính đã sửa

- `backend/src/Dungeon780.java`: tạo/chọn lại ải và cập nhật bản đồ đúng.
- `backend/src/Progression.java`, `backend/src/ItemRules.java` và bản cùng tên trong `client/src`: HP theo cấp.
- `client/src/BagScreen.java`: HP trong Túi khớp chiến đấu.
- `client/src/OnlineClient.java`, `OnlineResult.java`, `RewardCards780.java`: giữ và hiển thị bộ thẻ thưởng đã mở.
- `client/src/ShopScreen.java`, `StoneShop.java`: điều hướng, thông tin nổi và phím B.
- `client/src/DungeonScreen.java`, `RoomLoadout.java`: nhân vật/phần thưởng/mô tả phòng phó bản.
- `backend/src/GunnyServer.java`, `AdminServer.java`, `Dockerfile`, `public/index.html`, `public/https.html`: phiên bản 7.8.8, link bộ cài hiện tại.
- `client/build.py`: build lại hai JAR/JAD hiện tại, không tạo ZIP; tài nguyên lấy từ JAR hiện tại.
- `README.md`, hướng dẫn client/deploy, `.dockerignore`, `.gitignore`, hai script verify: giữ bản hiện tại và bỏ backup khỏi Git.
- `supabase/UPDATE-CURRENT.sql`: đổi tên bản SQL gộp cuối cùng; không thêm thay đổi database trong bản này. Giữ `GUNNY-ALL.sql` và source migration.
- Thêm `backend/tests/Update788Check.java`, `client/tests/Ui788Check.java`; cập nhật kiểm tra URL phiên bản và kỳ vọng HP.

Source client cũng được đồng bộ vào `D:/gunny/GunnyNokia/src` để không build nhầm bản cũ.

## Kiểm thử thực hiện

- `verify.ps1` hoàn tất: quà, admin LAN, cường hóa, Tà Thần, TCP/PvP, Supabase transport mô phỏng, đăng nhập, shop/trang bị, cấp độ, gió và các luồng phòng đang có.
- Kiểm tra bổ sung: Update788Check, TribalTcpCheck, PlayerUiCheck, ChunkPrefetchCheck, SupportPageCheck, MatchReturnRegressionCheck, ReturnRoomCheck, Expansion780Check, Gameplay771Check, TcpGameplayCheck, Tcp770Check, VirtualAuth770Check, RetentionAdmin770Check.
- Build Java ME CLDC 1.1/MIDP 2.0 và preverify thành công. Chạy Ui788Check trực tiếp bằng JAR phát hành ở 320×240 và 640×480: điều hướng/phân trang Shop, B mua, mở chọn ải ở stage 0 và 2, nhân vật không vượt khung, bộ thẻ giữ số/loại sau packet phòng thật từ backend.
- Protocol630Check kiểm tra HTTP tải JAR/JAD hiện tại, Range/HEAD, manifest/phiên bản/cổng và chặn đường dẫn riêng tư. AdminLanCheck: 26 kiểm tra đạt.
- Hai JAR kiểm tra CRC, StackMap, manifest, kích thước khớp JAD; E72 giữ ZIP_STORED không extra/data descriptor. Shared Battle/ItemRules/Progression/ForgeRules khớp client/server.
- `git diff --check` đạt. Python build/updater kiểm tra cú pháp được.

Chưa thử bản mới trên Nokia vật lý, chưa deploy lên Pi/Xubi và chưa chạy Docker ARM64 tại máy này. Bộ kiểm thử updater Python dành Linux không chạy được trên Windows vì thiếu `fcntl` (WSL chưa có kernel). Không gọi Supabase thật và không sửa file cấu hình bí mật đang dùng.

## Build và cập nhật

Tại repo hiện tại:

```powershell
./verify.ps1
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Cần Java 17+ trên PATH cho backend; client cần Java/Python và ecj/proguard/CLDC/MIDP trong thư mục tools. Script client giữ tài nguyên của JAR hiện tại, không cần phiên bản cũ.

```sh
docker build -t gunny-online:7.8.8 .
```

Đẩy toàn bộ thay đổi (cả file xóa và JAR/JAD mới) lên Git; Xubi Deploy từ nhánh đó. `/health`, `/download/`, admin header phải là 7.8.8. Điện thoại cần cài JAR/JAD mới; cập nhật backend không tự thay client đã cài.

Không có SQL mới riêng cho 7.8.8. Nếu schema đã đủ 7.8.7 không cần chạy lại SQL. Giữ nguyên Environment trên Xubi. Không có gói ZIP Pi.

## Dọn dung lượng

Đã xóa 680 mục được xác minh trong D:/gunny, tổng 10,240,459,301 byte (khoảng 10.24 GB / 9.54 GiB): archive, backup, thư mục phát hành cũ, website/server bản sao cũ, bộ cài và ảnh/build kiểm thử cũ. Xem `CLEANUP-7.8.8.csv` cho đường dẫn và dung lượng.

Giữ source đang dùng, tools build, tài nguyên gốc `Resource`, assets1/assets2/source-assets, cấu hình hiện tại, migration schema, source kiểm thử hồi quy và JAR/JAD 788. Những file Java/test có số phiên bản trong tên vẫn là source được tái sử dụng, không phải bản cài cũ. Git không bị viết lại lịch sử; các commit cũ vẫn có thể chứa bộ cài đã xóa. Repo clone nhẹ dùng `git clone --depth 1 <URL>`.

## Bộ cài

- `Gunny-E72-2009-Online-788.jar`: 21,471,747 byte, SHA-256 `cf0e63c3fdacb9c542a342e549a714b732ed3f08052c6f2290de576a92a65c60`.
- `Gunny-E6-Online-788.jar`: 50,455,156 byte, SHA-256 `59edf957ae87928e99738b555db01353c7ae50c54877cc2a21954327e4fdab95`.
