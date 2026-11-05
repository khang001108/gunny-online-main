# Gunny Java Online 7.4.0

Bộ server độc lập cho Render + Supabase, kèm web tải game dùng được trên trình duyệt màn 320px.

## Cập nhật server đang dùng

1. Nếu chưa chạy bản 6.3.0, Supabase → SQL Editor: chạy [migration nhận thưởng](supabase/migrations/202611040001_gunny_rewards.sql). Giữ hồ sơ, xu và tài khoản hiện có.
2. Chạy thêm [migration bạn bè và thẻ chung](supabase/migrations/202611040002_gunny_social_shared.sql) trong SQL Editor bằng truy vấn mới, không ghi đè migration trước. Upload **toàn bộ nội dung** gói này vào gốc repo GitHub hiện tại. Phải có `Dockerfile`, `render.yaml`, `.dockerignore`, `backend/`, `public/`, `supabase/` ngay ở gốc. Đợi Render deploy.
3. `/health` phải có `async:1`, `seats:6`, `departures:1`, `pow:2`, `replay:1`, `chunks:1`, `social:1`, `items:2`. Cài JAR/JAD 7.4.0 đúng E72 hoặc E6. Cấu hình [email tiếng Việt](supabase/templates/confirm-signup-vi.html) và Site URL theo [hướng dẫn](docs/UPDATE-6.3.1.md).

## Project mới

Tạo Supabase project, chạy lần lượt các file trong `supabase/migrations`. Tạo Render Web Service dùng Dockerfile ở gốc repo, Singapore, Health Check `/health`. Giữ Root Directory trống. Thêm `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY` trong **Environment của Render**. Không ghi khóa thật vào file và upload lên GitHub. Bộ ví dụ `.env.example` không chứa khóa hoạt động.

## Shop 7.4.0

Chạy thêm [migration shop](supabase/migrations/202611050001_gunny_shop.sql), cập nhật server và game cùng bản. Xu dùng số dư kiếm trong game; mua bằng giỏ hàng, không nạp tiền. [Hướng dẫn](docs/UPDATE-7.4.0.md).

## Bốn vũ khí 7.4.0

Sau migration shop, chạy [migration vũ khí](supabase/migrations/202611050002_gunny_arsenal.sql), cập nhật game và server 7.4.0 (`arsenal:2`). [Chi tiết](docs/UPDATE-7.4.0.md).

## Tiền mua sắm và trang bị 7.4.0

Sau SQL shop/vũ khí, chạy [migration trang bị và ví](supabase/migrations/202611050003_gunny_equipment.sql). Tỷ lệ 100 xu chiến đấu = 1 tiền mua sắm; đồ đang mặc có chỉ số thực trong trận. Cập nhật cả game/server (`equipment:1`). [Hướng dẫn](docs/UPDATE-7.4.0.md).

## Web tải game

`https://TEN-SERVER.onrender.com/download/` có JAD/JAR cả E72 và E6, bản vá và hướng dẫn HTML/PDF. **Giữ thư mục public trong repo** vì Docker cần đóng gói các file này. Trang không dùng JavaScript hay tài nguyên bên ngoài. Nokia chưa truy cập HTTPS được có thể nhận file từ máy tính hoặc dùng gói `Gunny-download.zip` qua HTTP trong cùng Wi-Fi.

## Thẻ danh sách phòng

Thẻ dùng nhãn Tự do / Khiêu chiến và ảnh map theo phòng thật. Khóa ô đồng minh hiện 1/1, mở trống hiện 1/2; ô có AI cũng tính đã có người. Cần cập nhật cả game và server (`cards:1`). [Chi tiết 7.4.0](docs/UPDATE-7.4.0.md).

## Thanh công cụ trong phòng

Năm biểu tượng dùng lại hình và viền vàng của sảnh Online, đặt sát nhau dưới phòng; giữ tooltip và hỗ trợ chạm/kéo/thả. Khung bản đồ thay Quan chiến. [Chi tiết 7.4.0](docs/UPDATE-7.4.0.md).

## Thiết lập phòng

Bảng tạo phòng dùng asset gốc; tạo thường hiện Thi đấu, tạo Tổ đội hiện Tổ đội. Có khóa mật mã và hỗ trợ bốn phím hướng. [Chi tiết 7.4.0](docs/UPDATE-7.4.0.md).

## Đạo cụ

Chọn ba ô Z/X/C trong phòng, mỗi đạo cụ dùng một lần mỗi trận và tốn thể lực. Q/W/E/R/T và B giữ buff cũ; F đổi vũ khí. [Chi tiết sáu đạo cụ và thể lực](docs/UPDATE-7.4.0.md).

## Phòng và lượt

Đấu đơn chờ và ghép người chơi thật ở màn VS. Tổ đội có 6 ô mở cho trận 3v3, đủ 6 người sẵn sàng sẽ tự bắt đầu. Tạo phòng mở thiết lập riêng, giữ giao diện phòng cổ điển.

Thao tác và bắn chạy ngay trên client bằng vật lý offline; server kiểm tra và xử lý ngầm. Máy còn lại nhận và xem tuần tự đoạn di chuyển, POW và từng loạt đạn sau khi server xử lý xong đoạn đó. Không phải chờ cả lượt. Rào cuối lượt chờ các máy phát lại xong trước khi chạy đồng hồ lượt kế tiếp. [Cách cập nhật 7.4.0](docs/UPDATE-7.4.0.md). Kết quả dùng ảnh gốc, sau đó lật 1 trong 9 thẻ để nhận xu; SQL bảo đảm không cộng thưởng hai lần.

## Chạy thử

JDK 17 trở lên. Chạy `./verify.ps1` để kiểm tra HTTP, giao thức cũ/mới, tài khoản mock, bản đồ, thưởng lặp và tải file an toàn. Chạy `./backend/run-local.ps1 -TestAccounts` để tạo server localhost 8080 với tài khoản thử trong RAM. Chế độ thử không dùng được trên Render. Game giả lập nhập `http://127.0.0.1:8080`.

Đã thử với 6 MIDlet trong MicroEmulator ở 320×240 và 640×480. Chưa thử bản này trên Nokia vật lý hay Supabase/Render thật. Phòng đang chơi vẫn ở RAM; Render khởi động lại sẽ mất trận chưa lưu. Hồ sơ và phần thưởng đã ghi được lưu tại Supabase.

[Hướng dẫn cập nhật, email, xu và web](docs/UPDATE-6.3.1.md)
