# Rà soát migration Gunny 7.7.0 — 13/11/2026

Đã sửa và kiểm thử trên PostgreSQL **17.11** riêng, dùng schema Auth mô phỏng và
baseline SQL của repository. Không chạy SQL Supabase production; không build lại,
thay hoặc khởi động lại container game production.

## Lỗi đã tái hiện và sửa

1. Khi policy tắt, deadline trả NULL khiến gia hạn tính lại từ hiện tại, có thể
   rút ngắn `protected_until` và mất tác dụng khi bật lại. Bản sửa cộng vào
   `pending_extension_days`, giữ mốc bảo vệ; lúc bật, cộng ngày chờ vào hạn mới
   hoặc mốc bảo vệ cũ nếu dài hơn. Ngày chờ không hao hụt trong lúc policy tắt,
   chỉ được áp dụng một lần và vẫn dùng hạn riêng nếu tài khoản có override.
2. Key thiếu/null từng được chấp nhận; retry dựa vào audit; cùng key đổi số ngày
   bị bỏ qua âm thầm. Bản sửa bắt buộc UUID hợp lệ và lưu biên nhận ở bảng riêng
   `gunny_offline_extensions` với khóa chính UUID. Retry cùng nội dung không cộng
   lại; khác tài khoản/số ngày bị từ chối. Biên nhận còn sau dọn audit/xóa tài khoản,
   nên key không được dùng lại cho một người khác.
3. Guard dùng inner join nên bỏ sót Auth user chưa có profile. Đã đổi sang left
   join để đối chiếu tất cả UUID/email/password hash, gồm NULL và user chưa có
   profile; thêm snapshot riêng để bảo vệ lịch xóa thủ công trong transaction.
4. Mốc hoạt động lấy giá trị mới nhất của last-seen, last-login, ngày tạo và mốc
   theo dõi; last-seen cũ không che lần đăng nhập mới. Audit xóa do không hoạt
   động được ghi đúng `inactive-delete`.

## Lịch xóa thủ công và dữ liệu cũ

Migration, gia hạn không hoạt động và bật/tắt retention giữ nguyên `delete_at`,
`delete_reason`, `appeal`, `cancel_on_login`.

**Tắt retention không tắt lịch xóa thủ công.** Lịch đã đến hạn vẫn được maintenance/
login xử lý như trước, kể cả có ngày gia hạn chờ. Với lịch chưa đến hạn, đăng nhập
hủy lịch khi `cancel_on_login=true`, còn false giữ nguyên lịch.

Policy vẫn tắt mặc định, hạn mặc định vẫn 90 ngày. Migration chạy lặp không reset
policy đã chỉnh, mốc bảo vệ, ngày chờ, lịch thủ công hoặc dữ liệu người chơi.
Giữ nguyên UUID, email đăng nhập, password hash và mọi cột profile. Nếu guard
phát hiện thay đổi ngoài dự kiến, toàn bộ transaction bị hủy.

## Idempotency, đồng thời và lịch sử

`retention-save` và `extend-offline` cùng khóa hàng policy trước, sau đó mới khóa
hàng tài khoản. Khóa chính UUID của ledger đảm bảo chỉ một biên nhận cho mỗi key.

Audit cũ có UUID/ngày hợp lệ được nhập vào ledger mà không áp dụng ngày lần nữa.
Nếu cùng key đã được dùng với tài khoản/số ngày khác nhau trong lịch sử, migration
hủy transaction để chủ máy rà soát; không tự chọn, sửa hoặc xóa lịch sử. Bản sửa
không suy đoán/khôi phục hiệu lực của các gia hạn đã chạy bởi bản nháp cũ.

## Kết quả kiểm thử

**39 ca PostgreSQL đều qua**, gồm policy tắt/bật, hạn chung/riêng, ngày chờ, mốc
bảo vệ dài hơn, retry, key sai/thiếu/null, key khác payload, yêu cầu đồng thời,
lịch xóa thủ công, chạy migration lặp, nhập biên nhận cũ, rollback khi dữ liệu cũ
bị thay đổi và quyền RLS/RPC. Log đầy đủ: `docs/sql-retention-770-review.log`.

```sh
python3 backend/tests/retention770_review.py --port PORT_POSTGRES_LOCAL
```

Runner cần psycopg 3 và server test khởi động với `-c gunny.isolated_test=1`.
Chỉ dùng localhost và tài khoản test; kiểm tra cờ cách ly trước khi ghi, sau đó
mỗi ca tự tạo/xóa một database riêng. Không nhận URL hoặc credential production.

**11 ca kiểm thử tool cập nhật cũng qua.** Gate kiểm tra thêm cột ngày chờ và
bảng biên nhận, để schema của bản nháp cũ không được coi là sẵn sàng triển khai.

## File để chủ máy rà soát/chạy thủ công

`supabase/migrations/202611120001_virtual_accounts_retention.sql`

Đã đồng bộ bản sao trong GUNNY-ALL.sql, nguồn GunnyNokia/server và ZIP phát hành.
Chủ máy chỉ tự chạy migration riêng trong project `mnquwasxacifkktmckdv` sau khi
rà soát. Đợt kiểm tra này không chạy SQL hay cập nhật container production.
