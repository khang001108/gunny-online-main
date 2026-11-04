# Gunny Java Online 6.3.2

Bộ server độc lập cho Render + Supabase, kèm web tải game dùng được trên trình duyệt màn 320px.

## Cập nhật server đang dùng

1. Nếu chưa chạy bản 6.3.0, Supabase → SQL Editor: chạy [migration nhận thưởng](supabase/migrations/202611040001_gunny_rewards.sql). Giữ hồ sơ, xu và tài khoản hiện có.
2. Upload **toàn bộ nội dung** gói này vào gốc repo GitHub hiện tại. Phải có `Dockerfile`, `render.yaml`, `.dockerignore`, `backend/`, `public/`, `supabase/` ngay ở gốc. Đợi Render deploy.
3. `/health` phải có `async:1`, `seats:6`, `departures:1`. Cài JAR/JAD 6.3.2 đúng E72 hoặc E6. Cấu hình [email tiếng Việt](supabase/templates/confirm-signup-vi.html) và Site URL theo [hướng dẫn](docs/UPDATE-6.3.1.md).

## Project mới

Tạo Supabase project, chạy lần lượt hai file trong `supabase/migrations`. Tạo Render Web Service dùng Dockerfile ở gốc repo, Singapore, Health Check `/health`. Giữ Root Directory trống. Thêm `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY` trong **Environment của Render**. Không ghi khóa thật vào file và upload lên GitHub. Bộ ví dụ `.env.example` không chứa khóa hoạt động.

## Web tải game

`https://TEN-SERVER.onrender.com/download/` có JAD/JAR cả E72 và E6, bản vá và hướng dẫn HTML/PDF. **Giữ thư mục public trong repo** vì Docker cần đóng gói các file này. Trang không dùng JavaScript hay tài nguyên bên ngoài. Nokia chưa truy cập HTTPS được có thể nhận file từ máy tính hoặc dùng gói `Gunny-download.zip` qua HTTP trong cùng Wi-Fi.

## Phòng và lượt

Đấu đơn chờ và ghép người chơi thật ở màn VS. Tổ đội có 6 ô mở cho trận 3v3, đủ 6 người sẵn sàng sẽ tự bắt đầu. Tạo phòng mở thiết lập riêng, giữ giao diện phòng cổ điển.

Thao tác và bắn chạy ngay trên client; server kiểm tra các đoạn thao tác, quyết định HP/địa hình/kết quả. Rào cuối lượt chờ máy còn lại nhận xong dữ liệu trước khi chạy đồng hồ lượt kế tiếp. Kết quả dùng ảnh gốc, sau đó lật 1 trong 9 thẻ để nhận xu; SQL bảo đảm không cộng thưởng hai lần.

## Chạy thử

JDK 17 trở lên. Chạy `./verify.ps1` để kiểm tra HTTP, giao thức cũ/mới, tài khoản mock, bản đồ, thưởng lặp và tải file an toàn. Chạy `./backend/run-local.ps1 -TestAccounts` để tạo server localhost 8080 với tài khoản thử trong RAM. Chế độ thử không dùng được trên Render. Game giả lập nhập `http://127.0.0.1:8080`.

Đã thử với 6 MIDlet trong MicroEmulator ở 320×240 và 640×480. Chưa thử bản này trên Nokia vật lý hay Supabase/Render thật. Phòng đang chơi vẫn ở RAM; Render khởi động lại sẽ mất trận chưa lưu. Hồ sơ và phần thưởng đã ghi được lưu tại Supabase.

[Hướng dẫn cập nhật, email, xu và web](docs/UPDATE-6.3.1.md)
