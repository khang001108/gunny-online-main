# Gunny Java 7.9.15 — Cơ chế Sulan đỏ

Đã sửa client/backend và build JAR/JAD. Không có SQL hay env/secret mới; chưa triển khai production.

- Cây đậu quay về phía boss khi xuất hiện và khi đến lượt.
- Lửa địa ngục dùng animation vung tay gốc bốn lần, mỗi lần phóng ba quả lửa từ boss: tổng 12 quả, không phụ thuộc số người. Ba quả phân bố qua các nhân vật còn sống; một người thì cả ba nhắm người đó với độ lệch mục tiêu nhỏ. Bỏ cơ chế mưa lửa từ trên của bản trước.
- Cháy chỉ gây sát thương lúc bắt đầu lượt của chính người bị cháy, không còn trừ máu theo giây/frame. Giữ mức Dễ/Thường/Khó 30/55/80 và giảm một nửa khi đứng gần cây đậu sống. Cháy tiếp tục đến khi được dập, không cộng dồn nhiều tầng.
- Băng nổ gần người bị cháy dập được trạng thái cháy và vùng lửa xung quanh, kể cả bắn vào chính mình/đồng đội. Vẫn giữ quy tắc băng đóng băng đối thủ hiện có. Cây đậu sống dùng lượt của nó để dập toàn bộ lửa; đậu chết không thể dập.
- Sulan đỏ gọi vật tổ HP gốc tại đầu lượt boss nếu vật tổ cũ đã bị phá. Mỗi lượt boss, vật tổ sống hồi một lần 5% HP tối đa, không vượt máu tối đa. Dành riêng slot12 cho vật tổ, slot6 cho đậu; giữ đủ 5 lính và 2 chiến sĩ tại 7 slot viện binh khác.
- Ngọn lửa hung tàn dùng animation dẫm chân gốc, rung camera và chuyển hướng nhìn về nhân vật trước khi phát nổ tại vị trí nhân vật/cây đậu. Chiêu đất gây sát thương trực tiếp, không tự gắn thêm trạng thái cháy.
- Sulan đỏ mức Dễ giảm từ 14.000 xuống 8.000 HP, khoảng 43%. Mức Thường/Khó và Sulan vàng giữ nguyên. Vật tổ hồi 400 HP mỗi lượt boss ở mức Dễ.

## Xác minh

- `Sulan7915Check`: hướng đậu, HP Dễ, vật tổ hồi một lần đúng5%, không hồi theo frame, cháy theo lượt, băng tự dập lửa, đậu chết, đủ12 quả lửa và rung/chuyển camera chiêu đất.
- `Sulan7912Check`:148 kiểm tra đã cập nhật theo cơ chế cháy/lửa mới. `SulanAdds7913Check`:174 kiểm tra viện binh vẫn qua; bài chơi trọn chiến dịch vẫn thắng trong200 lượt.
- `verify.ps1`: bộ kiểm tra server, TCP, phó bản, PvP, đồ, VIP/cánh, thưởng và reconnect; log `backend/build/verify-7915.log`.
- Hai JAR phát hành thực tế qua `UiFix7914Check`: E72/Asha320×240 heap32MB, E6640×480 heap128MB; render/snapshot/chunk, hồi phục gói, slot NPC6–13, điều kiện/phímM, phòng/map/PvP.
- Build/preverify CLDC/MIDP và đối chiếu class client/server, version/size JAD, hash tại `docs/BUILD-7.9.15.json`.

Chưa thử trên Nokia vật lý hay mạng production. Cập nhật cả backend và client lên7.9.15.
