# Gunny 7.7.1 — đồng bộ cấp và cập nhật trận đấu TCP

Bản chuẩn bị ngày 13/11/2026. Chưa triển khai production, chưa chạy SQL Supabase,
chưa thay container Docker đang phục vụ người chơi. Không chuyển project.

- Cấp lấy từ EXP hiện có của từng tài khoản, gửi cùng đội hình mỗi lần đồng bộ phòng
  và cùng trạng thái chiến đấu. P1/P2 nhìn thấy đúng cấp của nhau ngay khi vào trận.
  EXP sau nhận thưởng được cập nhật cho cả hai máy; trạng thái trận cũ không ghi đè
  lại cấp mới. Không sửa UUID, email, password hash, profile hay dữ liệu vật phẩm.
- Mỗi lần lên cấp cộng 5 công, 5 thủ, 5 may mắn. Lv 1 có cộng cấp bằng 0;
  Lv 20 có +95 mỗi chỉ số; Lv 70 có +345. Trang bị cộng riêng và không làm mất
  phần tăng theo cấp khi thay vũ khí. Theo cơ chế hiện có của game, thủ tăng giới hạn
  HP, may mắn tăng giới hạn thể lực; công tăng sát thương. Lực chiến tính cả ba phần.
- 8 AI theo thứ tự danh sách có Lv 1, 5, 10, 15, 20, 30, 45, 60. EXP/cấp của AI
  được dùng chung trong danh sách mời, phòng và trận. Chỉ số theo cấp áp dụng cả
  online và offline.
- Phòng mới dùng trạng thái hiện tại khi đường đạn còn bay, thay cho đợi gom đoạn
  chuyển động rồi phát lại. Socket thông báo đánh thức socket lệnh ngay khi đường đạn,
  POW, người chơi hoặc phòng thay đổi. Máy bắn dự đoán tại chỗ; server vẫn quyết định
  sát thương, địa hình, chuyển lượt và phần thưởng. P2 chỉ mô phỏng phần hiển thị.
- Chọn map theo ba bước: map → thời gian tấn công → xác nhận. OK hoặc phím mềm trái
  tiến một bước; phím mềm phải lùi một bước. Chỉ xác nhận cuối mới gửi thay đổi.
  Khung sáng của nút xác nhận dùng đúng vị trí/kích thước nút ở cả hai độ phân giải.
- Có dấu chủ phòng trên thẻ online, gồm phòng thường, đấu đơn và 3v3. Ghế chủ phòng
  do server gửi, không suy từ người đang xem. Danh sách bạn bè/mời có cấp cạnh giới tính.

## Kiểm thử

- `bash gunny-online-main/verify.sh`: 656 kiểm tra backend thành công, trong đó
  519 kiểm tra mới về cấp 1–70, EXP sát ngưỡng, ba chỉ số, thay trang bị, AI,
  gói trạng thái trực tiếp, đánh thức socket và đường tải bộ cài 771.
- 13 kiểm thử Python cho tool cập nhật: xác nhận SQL, schema, phục hồi khi lỗi,
  kiểm tra đủ tính năng cấp/TCP trực tiếp và đúng phiên bản JAD trước triển khai.
- Hai MIDlet đóng gói thật được chạy đồng thời qua hai kết nối TCP riêng,
  kiểm tra cấp người khác trong phòng/trận/danh sách, chọn thời gian online,
  giữ lực dừng đồng hồ, P2 thấy đạn khi còn bay, chuyển lượt, nhận kết quả,
  tăng cấp sau nhận thưởng và đăng xuất. Chạy cả 320×240 và 640×480.
- 15 kiểm tra UI ở mỗi độ phân giải và 12 kiểm tra hồi quy gán phím/giữ lực ở
  mỗi độ phân giải. Ảnh kiểm tra được giữ trong `docs/review771/`.
- Tool readiness đã chạy với server chính khởi động riêng bằng tài khoản RAM:
  HTTP health, hai bản tin TCP và cả hai JAD 7.7.1 đều hợp lệ.
- Các phép đo độ chờ trong báo cáo là loopback trên máy kiểm thử, không phải số đo
  qua Internet/4G. Chưa thử điện thoại Nokia hoặc tay cầm vật lý.

Chạy lại đầy đủ từ workspace có sẵn `GunnyNokia`, JDK và công cụ MIDP:
`bash GunnyNokia/verify-771.sh`. Log lần kiểm tra này: `docs/gameplay-771-verification.log`.

## Chuẩn bị cập nhật Pi 5

Cập nhật cả server và hai bộ cài 771 cùng phiên bản. Client mới từ chối server thiếu
khả năng đồng bộ mới; server production mới yêu cầu luật 771 để tránh đọc sai gói tin.

Không có migration mới cho cấp/chỉ số/AI. Bản này tiếp tục dùng migration tài khoản
7.7.0 đã rà soát ở `supabase/migrations/202611120001_virtual_accounts_retention.sql`.
SHA256 vẫn là `274110436b25eae327c6c811efb4ee535fb4978c33759515a04b10f347da2f9f`.
Xem `REVIEW-SQL-7.7.0-2026-11-13.md` về các 39 kiểm thử PostgreSQL trước đó.
Không chạy lại PostgreSQL trong lần sửa gameplay này vì nội dung SQL giữ nguyên.

Tool `sudo bash deploy/cap-nhat-gunny.sh` hiện chuẩn bị phiên bản 7.7.1; image đích
là `gunny-online:7.7.1`, tự build nếu chưa có. Tool vẫn yêu cầu chủ máy xác nhận đã tự
chạy SQL và kiểm tra schema trước khi thay container. Lần làm việc này chỉ sửa mã,
đóng gói và chạy các kiểm thử riêng; chưa build image 7.7.1 hay cập nhật production.
