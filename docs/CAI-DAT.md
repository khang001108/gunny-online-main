# Gunny Online — Render Free + Supabase Free

## Bản 6.2.0: cập nhật cả server và sáu địa hình

Xem [hướng dẫn cập nhật 6.2.0](UPDATE-6.2.0.md). Cần push toàn bộ backend mới và sáu ảnh địa hình lên GitHub, đợi Render deploy rồi mở lại game mới. Không cần thay đổi Supabase.

Code đã có máy chủ và kết nối online trong cả hai JAR. Chưa có server công khai cho đến khi bạn làm các bước dưới đây. Không cần treo máy tính sau khi Render triển khai xong.

## 1. Tạo Supabase riêng cho game

1. Vào <https://supabase.com/dashboard>, chọn **New project** trong tổ chức dùng gói Free.
2. Đặt tên `gunny-java`. Chọn Singapore nếu có để gần Render. Tự đặt và giữ mật khẩu database; game không cần bạn nhập mật khẩu này.
3. Đợi project sẵn sàng. Mở **SQL Editor → New query**.
4. Mở file [SQL khởi tạo Supabase](../supabase/migrations/202611030001_gunny.sql), chép toàn bộ nội dung, bấm **Run**.
5. Kiểm tra có hai bảng `profiles` và `match_results`. Không tắt RLS. Người chơi chỉ được đọc hồ sơ của mình; cập nhật vàng, thắng trận và mua đồ do server thực hiện.
6. Trong **Project Settings / Data API**, lấy **Project URL**, ví dụ `https://abcdef.supabase.co`.
7. Trong **Project Settings → API Keys**, lấy **Publishable key** (`sb_publishable_...`) và **Secret key** (`sb_secret_...`). Hai khóa này chỉ đặt trong Environment của Render. Không chép Secret key vào JAR, GitHub hoặc gửi vào chat.

Máy chủ cũng hỗ trợ tên biến cũ `SUPABASE_ANON_KEY` / `SUPABASE_SERVICE_ROLE_KEY` nếu bạn đang dùng các khóa JWT legacy. Không trộn publishable key với secret key. Code dùng khóa mới trên header `apikey`, đúng theo [tài liệu Supabase](https://supabase.com/docs/guides/getting-started/api-keys).

### Cho người chơi tự đăng ký ngay trong game

1. Trong **Authentication**, bật **Allow new users to sign up**.
2. Mở **Authentication → Sign In / Providers → Email**, bật phương thức Email.
3. Muốn đăng ký xong vào game ngay, **tắt Confirm email** rồi **Save**. Khi đó không cần cấu hình SMTP, người chơi không cần xác minh chủ email.
4. Người chơi mở **Online (O) → Đăng ký tài khoản**, nhập tên nhân vật, email, mật khẩu và nhập lại mật khẩu, rồi bấm **Tạo tài khoản**. Game sẽ tự đăng nhập khi tạo xong.

Không cần thêm từng người trong Supabase Users. Nếu muốn giữ xác minh email, để Confirm email bật và cấu hình [Custom SMTP](https://supabase.com/docs/guides/auth/auth-smtp). Khi đó game hướng dẫn xác nhận email rồi đăng nhập, thay vì vào ngay. SMTP tích hợp của Supabase chỉ gửi tới địa chỉ thuộc nhóm quản trị project và có hạn mức thấp.

Thiết lập này tuân theo [cấu hình Supabase Auth](https://supabase.com/docs/guides/auth/general-configuration); game không dùng quyền admin để bỏ qua xác minh.

### Sửa email xác nhận chuyển nhầm về localhost:3000

Sau khi cập nhật máy chủ có trang xác nhận, vào **Supabase → Authentication → URL Configuration**:

- **Site URL**: `https://gunny-online-main.onrender.com`
- **Redirect URLs**: thêm `https://gunny-online-main.onrender.com/auth/confirmed` nếu dùng đường dẫn này làm địa chỉ chuyển trang.
- Bấm **Save**. Nếu dùng hostname Render khác, thay đúng hostname của bạn.

Trong **Email Templates → Confirm signup**, giữ liên kết `{{ .ConfirmationURL }}` theo mẫu mặc định. Không thay bằng liên kết chỉ trỏ tới Site URL vì như vậy sẽ bỏ qua bước xác nhận của Supabase. Giữ Confirm email bật nếu muốn xác minh email người chơi.

Máy chủ phục vụ trang tiếng Việt tại `/` và `/auth/confirmed`, hướng dẫn quay lại Gunny đăng nhập. Trang không lưu token trong trình duyệt, không tự đăng nhập và không xác nhận thay Supabase; lỗi xác nhận được hiển thị nếu URL có thông báo lỗi, rồi tham số/fragment bị xóa khỏi thanh địa chỉ.

Email đã gửi trước khi đổi Site URL có thể vẫn chứa địa chỉ localhost. Nếu đã bấm liên kết và sau đó mới thấy lỗi localhost, tài khoản có thể đã được xác nhận: thử đăng nhập ngay trong game. Kiểm tra trạng thái xác nhận trong **Authentication → Users** khi cần. Đổi Site URL không sửa được các liên kết đã gửi.

Không cần sửa schema hay cài JAR mới cho thay đổi này. Cập nhật `backend/src/GunnyServer.java` trong repository độc lập, push lên GitHub và đợi Render deploy, sau đó mở URL gốc để kiểm tra có trang Gunny.

### Cập nhật bản 6.1.2 để thao tác online mượt hơn

Cài JAR/JAD Smooth 6.1.2 đúng E72 hoặc E6. Client phản hồi di chuyển, góc và thanh lực ngay trên máy, rồi làm mượt vị trí từ máy chủ. Server vẫn quyết định sát thương, địa hình, lượt và phần thưởng.

Để lực và góc lúc thả nút khớp với màn hình khi mạng chậm, cập nhật `backend/src/GunnyServer.java` từ gói server mới, push lên GitHub và đợi Render deploy. `/health` sẽ có `"controls":2` bên cạnh `"protocol":1`. Không cần sửa Supabase hay tạo lại tài khoản. JAR cũ vẫn kết nối được; JAR mới gặp server cũ sẽ dùng cách gửi lực cũ.

## 2. Đưa bộ server lên GitHub

1. Giải nén `gunny-online-main.zip`, rồi mở thư mục `gunny-online-main`.
2. Tạo repository **Private**, ví dụ `gunny-java-server`, trên GitHub.
3. Dùng **Add file → Upload files** để tải toàn bộ nội dung thư mục đã giải nén lên repo, giữ nguyên cấu trúc. Thư mục gốc phải có `Dockerfile`, `render.yaml`, `backend/`, `supabase/`.
4. Không tải file `.env` chứa khóa thật lên GitHub. Bộ ZIP chỉ có `.env.example` là mẫu.

Bạn chỉ cần bộ ZIP server nhỏ này, không cần upload gói `Resource`, `gunny-main`, JAR điện thoại hay thư mục `build`.

## 3. Tạo dịch vụ Render miễn phí

1. Vào <https://dashboard.render.com>, chọn **New → Web Service**.
2. Kết nối GitHub, chọn repo riêng vừa tạo. Render có thể yêu cầu cấp quyền đọc repo này.
3. Tên: `gunny-java-online`. Region: **Singapore**. Language/Runtime: **Docker**. Root directory để trống. Dockerfile path: `./Dockerfile`.
4. Instance Type: **Free**. Không chọn database Render, ổ đĩa, gói trả phí hay background worker.
5. Thêm các Environment Variables:

| Tên biến | Giá trị |
| --- | --- |
| `SUPABASE_URL` | Project URL Supabase, không thêm `/rest/v1` |
| `SUPABASE_PUBLISHABLE_KEY` | Publishable key của project |
| `SUPABASE_SECRET_KEY` | Secret key của cùng project; chỉ ở Render |
| `MAX_ROOMS` | `8` |

6. Đặt Health Check Path là `/health`, rồi bấm **Deploy Web Service**.
7. Đợi trạng thái **Live**. Mở `https://TEN-DICH-VU.onrender.com/health`. Phải nhận được `{"ok":true,"protocol":1}`. Không cần tự chạy Node hay Java trên máy tính nữa.

Nếu dùng **New → Blueprint** thay vì Web Service, file `render.yaml` đã ghi sẵn Free, Singapore, Docker và `/health`; nhập ba biến Supabase khi Render yêu cầu. Không bật `GUNNY_TEST_MODE` trên Render: server chủ động từ chối chế độ tài khoản thử khi triển khai cloud.

## 4. Đăng nhập và chơi trên điện thoại/giả lập

1. Cài bản JAR online C3/E72 hoặc E6 được cung cấp riêng. Cho phép game kết nối Internet khi điện thoại hỏi quyền.
2. Ở sảnh hoặc màn tạo nhân vật, nhấn **Online (O)**. Có nút Đăng nhập và Đăng ký riêng, không phải tìm trong Menu.
3. Nhập URL gốc của Render, ví dụ `https://gunny-java-online.onrender.com`, không thêm `/api` hay `/health`.
4. Chọn **Đăng ký tài khoản**, nhập tên, email và hai lần mật khẩu từ 8 ký tự. Nếu Confirm email đã tắt, tạo xong sẽ tự đăng nhập. Nếu project vẫn bật xác minh email, xác nhận email rồi dùng nút Đăng nhập.
5. Game nhớ URL và email trên máy, không lưu mật khẩu. Không nhập bất kỳ API key Supabase nào vào game.
6. Người thứ nhất chọn **Tạo phòng**. Chọn bản đồ, Bot/Người và bot đồng minh bằng giao diện phòng hiện có, rồi bấm **Bắt đầu** để tạo phòng online.
7. Game hiển thị mã phòng, ví dụ `1001`. Người thứ hai chọn phòng trong danh sách hoặc **Nhập mã phòng**, chọn bên phải để đấu với chủ phòng, hoặc bên trái để cùng phe nếu còn chỗ.
8. Chủ phòng bấm **Bắt đầu** trên màn phòng online. Hai máy tải cùng map, xong màn VS mới chạy đồng hồ trận.
9. Điều khiển bắn, buff 1–5, B POW, phím hướng và nhả lực giữ như bản offline. Chỉ tài khoản sở hữu nhân vật đang có lượt được thao tác.
10. Trong danh sách phòng, **I** mở túi, **S** mở shop, **N** đổi tên và **R** tải lại danh sách. Mua đồ online do server trừ vàng; tài khoản mới có 1.000 vàng và vũ khí mặc định. Dữ liệu/cheat của bản offline không được nhập thành vàng online.

2v2 hỗ trợ người chơi ở các ô trống và bot máy chủ. Bot đồng minh đã chọn giữ trang phục/vũ khí. Nếu chọn Người ở ô thứ hai và không có người khác vào ô này, chủ phòng điều khiển cả hai nhân vật theo lượt như bản offline.

## 5. Kiểm tra sau khi triển khai

- Dùng hai tài khoản khác nhau trên hai máy/giả lập: thấy cùng mã phòng và tên đối thủ.
- Host bắt đầu: cả hai máy cùng map, cùng lượt, thấy cùng phát bắn và hố nổ.
- Thử dùng buff ở máy đang không có lượt: server không cho phép.
- Hết trận: tài khoản thắng cộng 300 vàng, tài khoản còn lại cộng 80; tải lại tài khoản vẫn giữ kết quả. Một tài khoản điều khiển hai nhân vật chỉ nhận thưởng một lần.
- Đăng xuất rồi đăng nhập: vàng, đồ và trang phục vẫn còn.

Kiểm thử tự động đã chạy trên máy phát triển bằng HTTP thật với hai MIDlet, ở cả 320×240 và 640×480. Phần tài khoản trong phép thử dùng adapter nội bộ chỉ bind localhost; Supabase thật, Render thật và Nokia thật vẫn cần bước kiểm tra sau khi bạn cấu hình dịch vụ.

## Giới hạn hiện tại

- Render Free có thể ngủ khi không có lưu lượng, lần kết nối đầu phải chờ khởi động. Supabase Free có thể pause sau một tuần ít hoạt động. Không dùng ping giả để giữ tài nguyên.
- Phiên đăng nhập hiện tại có thời hạn tối đa khoảng một giờ. Khi hết hạn, đăng nhập lại; game chỉ nhớ email, không nhớ mật khẩu.
- Phòng, phiên đăng nhập và trận đang chạy nằm trong RAM của server. Render khởi động lại sẽ mất phòng/trận và người chơi cần đăng nhập lại. Hồ sơ, vàng và kết quả đã lưu nằm trong Supabase.
- Kết quả được ghi bằng transaction idempotent trong database; server có thử lại nếu mạng lỗi. Nếu server bị tắt trước khi ghi thành công, không đảm bảo khôi phục phần thưởng của trận đó.
- Không có kiểm thử kết nối TLS trên Nokia vật lý trong môi trường này. Điện thoại cần HTTPS/TLS tương thích với Render. Client không hạ cấp gửi mật khẩu qua HTTP công khai; HTTP chỉ dành cho localhost khi thử trên PC.
- Chưa có trang quản trị, chat online, kết bạn hoặc khôi phục mật khẩu trong game. Có thể quản lý tài khoản qua dashboard Supabase.

## Chạy thử nội bộ khi cần sửa code

Trong PowerShell ở thư mục dự án:

```powershell
.\backend\run-local.ps1 -TestAccounts
```

Server này chỉ nhận `127.0.0.1:8080`, dùng tài khoản thử trong RAM và mất dữ liệu khi dừng. Trên giả lập cùng máy, nhập `http://127.0.0.1:8080` trong màn đăng nhập và đăng ký hai tài khoản thử. Nó không thay thế server Render/Supabase thật.

Chạy kiểm tra trên máy phát triển bằng JDK 17 trở lên:

```powershell
.\verify.ps1
```

Các phép thử dùng HTTP thật và tài khoản thử localhost, không tạo hay thay đổi project Supabase thật.

Mã luật trận nằm ở `backend/src/Battle.java`, sao chép từ game. Nếu thay đổi luật hoặc giao thức, cập nhật cả server và JAR điện thoại.
