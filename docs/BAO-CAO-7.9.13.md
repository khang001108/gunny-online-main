# Gunny Java 7.9.13 — Viện binh ở ải boss Sulan

Đã sửa source client/backend và build JAR/JAD 7.9.13. Chưa triển khai production. Không cần SQL mới, không thay env/secret và không sửa dữ liệu người chơi.

## Gọi lính

- Sau khi kết thúc mỗi 2 lượt **của chính boss**, Sulan vàng gọi bù đủ **3 lính bộ lạc + 1 chiến sĩ**. Sulan đỏ gọi bù đủ **5 lính bộ lạc + 2 chiến sĩ**. Lượt nhân vật, cây đậu hay lính không tăng bộ đếm này.
- Chuyển vàng sang đỏ bắt đầu bộ đếm 2 lượt mới. Lính vàng còn sống vẫn tiếp tục chiến đấu; không bị xóa, dịch chuyển hay hồi máu. Đợt gọi đỏ bù phần đội hình còn thiếu.
- Hiện dùng cách **gọi bù đủ**, không cộng dồn vô hạn, để giữ số đơn vị và bộ nhớ Java ME hiện có. Đây là lựa chọn tạm thời đã nêu trong cuộc trao đổi khi chưa có trả lời về việc cộng dồn.
- Lính mới xuất hiện trước mặt boss theo hướng boss đang nhìn, trên nền đất hợp lệ. Không sinh đè nhân vật hoặc cây đậu. Giữ riêng ô cây đậu ngay cả trước khi nó xuất hiện hoặc sau khi nó chết.
- Dùng động tác gọi lính và sprite/animation lính gốc. Chuỗi chiêu Sulan, lời thoại, chuyển dạng, kết liễu và thưởng vẫn giữ từ 7.9.12.

## Lượt đánh và mục tiêu

- Thứ tự: boss → cây đậu nếu còn sống ở dạng đỏ → nhóm lính bộ lạc → từng chiến sĩ → đội nhân vật. Nhóm lính đi bộ và đánh cùng một lượt; mỗi chiến sĩ có một lượt riêng, dùng ba lao theo đường bay cũ.
- Hai loại lính chọn đối thủ sống gần nhất trong số nhân vật và cây đậu. Đổi mục tiêu khi đối thủ chết. Các đòn đánh thực sự trừ HP server, không chỉ phát animation.
- Giáo/cầu lửa của phe Sulan đi qua lính cùng phe, tránh bị chặn ngay khi vừa phóng. Đạn của người chơi và luật cây đậu nhận 20% sát thương đồng đội giữ nguyên.
- Hạ Sulan đỏ vẫn kết thúc ải dù viện binh chưa chết hết. Không thay bộ bài hay cơ chế nhận thưởng.

## Kiểm tra

- `SulanAdds7913Check`: **174 kiểm tra** gọi đúng lượt 2/4/6, bù số lính thiếu, giữ HP/vị trí lính sống, chuyển dạng, giới hạn số đơn vị, vị trí sinh hai hướng, đủ ba người chơi và cây đậu, lượt từng nhóm, chọn mục tiêu, đòn đánh thật, giáo đi qua đồng đội và thắng khi boss chết.
- `Sulan7912Check`: **144 kiểm tra** chuỗi chiêu/cảnh/lửa/cây đậu vẫn qua.
- `verify.ps1`: toàn bộ kiểm tra backend hiện có qua; log cục bộ `backend/build/verify-7913.log`. Bài chơi trọn ải dùng ngắm đường đạn thật ở ải Sulan thay vì đạn cheat homing bay thẳng vào hàng lính chắn trước boss; vẫn kiểm tra thắng trong giới hạn 200 lượt.
- `UiSulanAdds7913Check`: hai **JAR phát hành thực tế**, 320×240 heap 32MB và 640×480 heap 128MB. Đã đọc và render đội vàng 3+1, đội đỏ 5+2, cây đậu và chuyển động lính từ snapshot server; kèm kiểm tra TCP/chunk/hồi phục gói, map, điều kiện cấp và PvP.
- Build/preverify CLDC/MIDP, kiểm tra manifest/JAD, class đóng gói và hash ở `docs/BUILD-7.9.13.json`. Trang tải và Docker giữ bộ cài hiện tại 7913.

Chưa kiểm tra trên Nokia vật lý hoặc máy chủ đang chạy. Cập nhật backend và cài lại client cùng 7.9.13 để có đầy đủ animation và AI mới.
