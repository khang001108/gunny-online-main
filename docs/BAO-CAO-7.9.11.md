# Gunny Java 7.9.11

Huyệt Ma Kiến yêu cầu Lv15; Bộ Lạc Tà Thần yêu cầu Lv30 cho mọi độ khó. Hiện yêu cầu trên thẻ chọn phó bản và dòng Cấp của mini map. Khi chưa đủ cấp, client hiển thị yêu cầu và cấp hiện tại, không gửi thao tác tạo phòng hoặc chọn độ khó/đổi map/bắt đầu không hợp lệ.

Server vẫn quyết định điều kiện: kiểm tra tạo phòng, vào phòng trực tiếp, nhận lời mời, tìm đội nhanh, đổi phó bản và từng thành viên khi bắt đầu trận. Thông báo server nêu tên nhân vật chưa đủ cấp, giúp chủ phòng biết thành viên nào bị chặn. Tài khoản cheat cũng phải đủ cấp.

Tìm đội nhanh bỏ qua phòng Tà Thần đối với người dưới30 để tìm phòng Kiến còn chỗ; nếu chỉ có phòng Tà Thần phù hợp thì báo đúng yêu cầu30. Người dưới15 bị chặn bằng thông báo Kiến yêu cầu15.

Lv30 của Tà Thần được cố định trong `TribalRules.MIN_LEVEL` dùng chung client/server. Cấu hình legacy `min_level` không còn được dùng để hạ hoặc tăng yêu cầu; file cấu hình trong source ghi30 để dễ đọc. Không cần sửa env/secret hoặc chạy SQL. Không sửa EXP hay dữ liệu người chơi.

## Kiểm tra

- `DungeonLevels7911Check`:17 kiểm tra TCP thật, các mốc14/15 và29/30, tạo/vào phòng, lời mời, tìm đội nhanh, đổi map có thành viên thiếu cấp, chặn start trước khi thay đổi trận và không bypass bằng cheat.
- `UiLevels7911Check`: hai JAR thực E72 320×240 heap32MB và E6 640×480 heap128MB; yêu cầu trên thẻ, thông báo Lv29 cần30, chặn OK chọn thẻ thiếu cấp, cho chọn ở đúng cấp và không xếp hàng lệnh tạo phòng không hợp lệ. Kèm kiểm tra UI/phó bản/TCP của bản trước.
- `verify.ps1`: các kiểm tra server hiện có; log `backend/build/verify7911.log`. Fixture Tà Thần trước dùng Lv20 được cập nhật Lv30 theo yêu cầu mới.
- Compile/preverify và build JAR/JAD7.9.11; hash và kích thước trong `docs/BUILD-7.9.11.json`.

Chưa deploy production, chưa thử Nokia vật lý. Không có migration mới.
