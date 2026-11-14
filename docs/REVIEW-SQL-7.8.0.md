# Rà soát SQL Gunny 7.8.0

Đối tượng: `supabase/migrations/202611130001_forge_dungeon.sql`. Chỉ chạy kiểm thử
trên PostgreSQL 17 dùng riêng, localhost, database tạm mỗi ca. Không kết nối hoặc
chạy SQL trên Supabase production. Migration tài khoản 7.7.0 giữ nguyên bản đã rà soát.

32 ca kiểm thử trong `backend/tests/expansion780_review.py` kiểm tra:

- Chạy migration lần đầu/lặp lại giữ UUID, email, password hash, mọi trường cũ của
  profile và lịch retention; giữ đá/cấp rèn/biên nhận/phần thưởng khi chạy lại.
- Đá mới mặc định 0, 130 cấp rèn mặc định 0, chưa mở khó; kiểm tra biên mảng và dữ
  liệu không hợp lệ. Không sửa policy hay lịch xóa thủ công đang có.
- Mua đúng 80 tiền/đá, rèn đúng 4/8/12… đá, quyền sở hữu, giới tính, cấp rèn dự kiến,
  giới hạn tiền/đá/cấp. Mọi lỗi đều không trừ số dư.
- Chỉ cho rèn mới vũ khí/áo/nón/kính. Từ chối nguyên bộ/tóc/mặt nhưng giữ nguyên
  mọi cấp rèn đã có của chúng khi chạy lại migration; không reset dữ liệu cũ.
- Request key không được đổi user/thao tác/payload, retry chỉ trừ một lần; nhiều
  request đồng thời được tuần tự hóa. Biên nhận còn tồn tại sau khi xóa tài khoản.
- Hai lượt chọn và thứ tự chọn, thẻ dùng chung, retry cùng kết quả, xung đột/đồng
  thời không cấp thêm thưởng; chặn chế độ hoặc người chơi không đủ điều kiện.
- Phó bản chỉ thưởng đá 10–100; cấp 15, mở khó sau thắng thường, thua không mở khó,
  không nhận thẻ; kết quả trận idempotent, không thưởng tiền phó bản.
- Quyền truy cập RPC/bảng riêng chỉ cho service role.

Migration khóa bảng profile trước khi chụp dữ liệu cũ, tránh giao dịch khác sửa
profile trong cửa sổ kiểm tra bảo toàn. RPC mua/rèn khóa advisory theo request key
và khóa profile; nhận thẻ khóa hàng kết quả trận. Giao dịch kết thúc phó bản khóa
các profile theo thứ tự UUID để giảm nguy cơ deadlock giữa các đội giao nhau.

Giới hạn: đây là kiểm thử schema PostgreSQL tương thích với schema hiện tại trong
repo; không phải bản sao dữ liệu production. Chủ máy cần sao lưu và rà soát trước
khi tự chạy migration. Không có xác nhận triển khai production trong phiên làm việc này.
