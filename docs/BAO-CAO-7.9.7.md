# Gunny Java 7.9.7 — Tà Thần theo video

Đã sửa source client/server và build hai bộ cài Java ME cho C3/E72 và E6. Chưa cập nhật máy chủ production.

## Thay đổi

Ba ải: hạ20 Lính/Chiến Sĩ Bộ Lạc → hai anh em Bulan → Sulan vàng chuyển sang đỏ trong cùng trận. Bỏ ải bảo vệ Đậu Thần bị gán sai vào bước2. Đậu Hà Lan xuất hiện trong cơ chế của Sulan đỏ.

Thay hình và hoạt ảnh tạm bằng timeline Flash của đúng NPC, map1122/1124/1126, thẻ loading theo ba số trong video, ảnh nền loading xanh/đỏ. Lính áp sát mới đâm; chiến sĩ ném lao; Bulan/Sulan có vật ném riêng; dạng đỏ dùng lửa và đánh gần. Có thoại theo hành động, camera giới thiệu, vật tổ HP/Mệt Mỏi/Xiềng Xích/Nổ, gọi lính, chuyển dạng và hoạt ảnh chết.

Giới hạn số vật tổ/lính triệu hồi cùng loại để không cộng dồn vô hạn. Luật, mask đất và ID đạn được đồng bộ client/server. Có capability `tribal797`: client cũ nhận thông báo cập nhật khi vào Tà Thần, tránh nhận ID đạn không hỗ trợ.

## Cài và cập nhật

- C3/E72: `public/downloads/Gunny-E72-2009-Online-797.jar` và JAD cùng tên.
- E6: `public/downloads/Gunny-E6-Online-797.jar` và JAD cùng tên.
- Chép cả cặp JAR/JAD vào cùng thư mục trên điện thoại, mở JAD để cài.
- Cần cập nhật cả server và bộ cài7.9.7. Repo đã có source, tài nguyên backend và trang tải mới; chưa push hoặc deploy production. Không có SQL, env, secret hay cổng mới so với7.9.6.
- Build client: `python client/build.py --tools D:/gunny/GunnyNokia/tools`. Build server/Docker theo README hiện có.

## Kiểm tra

- `verify.ps1`: qua toàn bộ bộ kiểm tra server; log `backend/build/verify797.log`.
- `TribalCheck`:84 kiểm tra luật/thưởng/phòng.
- `TribalVideo797Check`:30 kiểm tra đâm/đạn thật, kỹ năng, chuyển dạng, chặn client cũ và chơi hết ba ải trong mô phỏng.
- `TribalTcpCheck`:118 kiểm tra vòng đời TCP ba người, ba ải, reconnect và nhận thưởng chống lặp.
- `UiTribal797Check`: qua hai JAR thật trong MicroEmulator,320×240 heap32MB và640×480 heap128MB; đọc snapshot server, vẽ map/phòng/loading, animation, bốn payload, giải phóng cache và về PvP.
- `Ui796Check`: qua cả hai bản, gồm VIP/cánh/ba lô/loading/HP cao và các luồng cũ.
- `client/tools/verify_tribal797.py`:111 kiểm tra gói cài và asset; JAD đúng byte size, CRC, bytecode46/StackMap, không còn sprite NPC tạm, đủ lưới animation, alpha đất/va chạm giống server ở cả hai cỡ.
- Size/hash: `BUILD-7.9.7.json`. Ảnh chụp: `client/build/tribal797-*.png`; ảnh tổng hợp `TA-THAN-7.9.7.jpg`.

Chưa thử trên điện thoại Nokia thật. Các phần cân chỉnh và câu bị che trong video được ghi rõ trong `TA-THAN-VIDEO.md`; không khẳng định mọi thông số giống máy chủ Mobi.
