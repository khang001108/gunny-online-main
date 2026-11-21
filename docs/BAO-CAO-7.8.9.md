# Gunny Java 7.8.9

## Đã sửa

- Nhận thẻ có hàng chờ thao tác riêng, ưu tiên trước các lượt đồng bộ nền. Bấm một lần lưu cả số lượt và vị trí thẻ; bấm lặp không thay yêu cầu đang nhận. Dòng “Đang nhận thưởng” chỉ phụ thuộc yêu cầu claim, không phụ thuộc cờ busy chung. Mở bộ thẻ cũng được xếp chờ. Ngắt kết nối/reset trận xóa yêu cầu cũ.
- Tà Thần gồm Rừng nguyên sinh → Bảo vệ Đậu Thần → Sulan (hai hình dạng trong cùng trận Boss). Hai ải đầu: 9 thẻ, một lượt mỗi người. Boss: 18 thẻ, hai lượt mỗi người. Server kiểm tra số lượt và vị trí; client không quyết định thưởng. Giữ cơ chế cánh/vũ khí hiếm ở bộ thẻ Boss.
- Bỏ toàn bộ ảnh `stone780.png` khỏi bộ cài, dùng hình đá Lv1 trắng trong bộ thẻ, thêm nhãn Lv1. Vật phẩm/số đá người chơi đã sở hữu không bị xóa.
- Khoảng số hiển thị bằng dấu ASCII: x20-70, x50-100.
- Di chuyển chọn đá trong Tiệm rèn cập nhật ngay grade, hình trong ô và tỉ lệ, không cần OK. Chỉ đá đang sở hữu xuất hiện, bảng đá luôn ở trang 1.
- Chữ kết quả lấy trực tiếp `StoreIISuccessBitAsset` / `StoreIIFailBitAsset` trong ddtstore.swf. Hiệu ứng sáng quanh đồ dùng bitmap circlelight và sinplelight gốc, có sáu khung sáng mờ dần cùng sao nhỏ. Không còn bật/tắt chớp toàn ô hoặc bảng STRENGTH bay lên. Ảnh nạp/cache khi mở rèn, giải phóng theo màn, không tạo ảnh trong vòng lặp vẽ.

## File chính

- `client/src/OnlineClient.java`, `OnlineResult.java`, `RewardCards780.java`: hàng chờ và hiển thị thẻ.
- `client/src/DungeonRewards.java` và bản cùng tên trong `backend/src`: số thẻ/lượt và số ải hiển thị.
- `backend/src/Dungeon780.java`, `TribalDungeon.java`, `Expansion780.java`, `DeckReveal.java`, `GunnyServer.java`: tuyến ba trận, nhận/mở cả bộ thẻ và xác nhận số lượt.
- `client/src/TribalArt.java`, `MapPicker.java`, `PhoneCanvas.java`: số ải/tên mô tả.
- `client/src/ForgeScreen.java`, `BagScreen.java`: xem trước đá, hiệu ứng/kết quả và dấu khoảng số.
- `client/assets1`, `client/assets2`, `client/ASSETS.json`: ảnh gốc tối ưu cho từng cỡ màn và nguồn gốc tài nguyên.
- `client/build.py`: chèn ảnh hiện tại và loại ảnh đá/flash cũ khi build; JAR/JAD 789.
- Metadata server/admin/Docker/trang tải/hướng dẫn lên 7.8.9; bỏ bộ cài 788.
- `backend/tests/TribalTcpCheck.java`, `client/tests/Ui789Check.java`: kiểm tra các thay đổi thực tế.

## Tương thích dữ liệu

Không có SQL mới cho 7.8.9. Adapter chơi ba trận dùng danh tính ải lưu sẵn 1, 2, 4 (Boss vẫn là 4 trong database), tương ứng giao diện 1, 2, 3. Nhờ vậy phần thưởng cuối, chống nhận trùng, ràng buộc database và dữ liệu cũ giữ nguyên. Trận Bulan không còn trong tuyến chơi; bộ luật AI cũ vẫn được giữ làm source tương thích và kiểm thử hồi quy.

Cần cập nhật đồng thời backend và JAR/JAD mới để hai phía dùng cùng luật 9/9/18. Không sửa Supabase thật, Environment hoặc gunny.env trong phiên làm việc này. Không tạo ZIP Pi.

## Kiểm thử

- `verify.ps1` hoàn tất, gồm quà, cường hóa, TCP/PvP, Tà Thần, shop/trang bị, phòng, Supabase transport mô phỏng và admin.
- `TribalTcpCheck`: 118 kiểm tra TCP với 3 tài khoản, đủ 3 trận, bắn thật, vào lại sau mất kết nối, từ chối thẻ thứ 10/lượt thứ 2 ở ải vượt, mở bộ thẻ đúng 9/9/18, nhận lặp không cộng trùng và giới hạn một cánh trong bộ cuối.
- Kiểm tra riêng Expansion780Check, MatchReturnRegressionCheck, Gameplay771Check, Protocol630Check đạt.
- `Ui789Check` chạy trực tiếp trên hai JAR phát hành ở 320×240 và 640×480: bấm một lần khi busy/pending đồng bộ nền, không ghi đè thẻ đang nhận, xem trước cấp đá trước OK, ảnh chữ/giao diện rèn thành công/thất bại, dấu '-' và bố cục 9/18 thẻ. Ảnh kiểm thử nằm trong `client/build/*789*`.
- Build backend Java 17 và CLDC/MIDP client, preverify thành công. CRC, manifest, JAD size và StackMap đều đạt; E72 giữ ZIP_STORED không extra/data-descriptor.
- Kiểm tra tài nguyên trong JAR khớp từng ảnh native overlay; không còn stone780/forge780-success/forge780-success-slot. Source Battle, ItemRules, ForgeRules, Progression, TribalRules, DungeonRewards khớp hai phía.
- `git diff --check` và kiểm tra cú pháp Python đạt.

Chưa thử bản mới trên Nokia vật lý, chưa deploy lên Pi/Xubi. Các kiểm tra database dùng bộ nhớ/transport mô phỏng, không thay dữ liệu thật.

## Build

```powershell
./verify.ps1
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

```sh
docker build -t gunny-online:7.8.9 .
```

Đẩy cả source, ảnh native overlay, file xóa và bốn JAR/JAD 789 lên Git; Deploy Xubi rồi cài lại client 7.8.9 từ trang tải. Bộ cài hiện tại là nguồn tài nguyên cho rebuild, không cần giữ JAR cũ. Danh sách dọn dẹp dùng `CLEANUP-CURRENT.csv`.

## Bộ cài

- `Gunny-E72-2009-Online-789.jar`: 21,514,206 byte; SHA-256 `7332d402b026e6bdbbf37cba6d1ba8a1d04ca56325ee8c89c3176ac0c4a6c706`.
- `Gunny-E6-Online-789.jar`: 50,573,805 byte; SHA-256 `91a849fd8cc6849ab2def3da0385e40069e1971f274dc742082d3adb0dafe274`.
