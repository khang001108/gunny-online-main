# Gunny Java Online 7.5.4

Bộ server độc lập cho Render + Supabase, kèm web tải game dùng được trên trình duyệt màn 320px.

## Cập nhật server đang dùng — 7.5.4

Bản 7.5.4 sửa máy bay, hiệu ứng, đạo cụ và popup; [chi tiết](docs/UPDATE-7.5.4.md). Không thêm SQL. Nếu đã chạy đủ SQL 7.5.3, bỏ qua bước 1.

1. Giữ bản sao lưu, chạy [SQL cập nhật đầy đủ](supabase/UPDATE-7.5.3-ALL.sql) trong Supabase SQL Editor. File giữ tài khoản/đồ/số dư cũ, cài API còn thiếu và bản mới theo đúng thứ tự. Nếu đã có đủ toàn bộ SQL 7.5.2 thì chỉ cần `202611060003_gunny_features.sql`. Không chạy lại từng SQL cũ phía dưới sau bản cập nhật mới.
2. Upload toàn bộ gói vào gốc repo và deploy Render. Giữ các biến Supabase đã có; admin dùng GUNNY_ADMIN_PASSWORD ít nhất 16 ký tự. Cài JAR/JAD 7.5.4 đúng E72/E6.
3. Supabase → Authentication → URL Configuration: Site URL `https://gunny-online-main.onrender.com`, Redirect URLs thêm `https://gunny-online-main.onrender.com/auth/confirmed` (đổi tên miền nếu server của bạn khác). Giữ mẫu email mặc định của Supabase; không cần sửa Confirm sign up. Không để localhost:3000.
4. Xem [hướng dẫn đầy đủ 7.5.3](docs/UPDATE-7.5.3.md) cho tài khoản người chơi, cộng tiền admin, đồ số 1 miễn phí và sắp xếp túi. Đường dẫn `/account/` là người chơi, `/admin/` là quản trị.

## Project mới

Tạo Supabase project, chạy lần lượt các file trong `supabase/migrations`. Tạo Render Web Service dùng Dockerfile ở gốc repo, Singapore, Health Check `/health`. Giữ Root Directory trống. Thêm `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `SUPABASE_SECRET_KEY` trong **Environment của Render**. Không ghi khóa thật vào file và upload lên GitHub. Bộ ví dụ `.env.example` không chứa khóa hoạt động.

Các mục phiên bản cũ dưới đây chỉ để đối chiếu; bản 7.5.3 đã gộp SQL đầy đủ ở trên.

## Quản trị 7.5.0

Mở `/admin/` sau khi chạy migration quản trị và cấu hình GUNNY_ADMIN_PASSWORD. Xem docs/QUAN-TRI.md. Không có mật khẩu mặc định.

## Shop 7.4.4

Chạy thêm [migration shop](supabase/migrations/202611050001_gunny_shop.sql), cập nhật server và game cùng bản. Xu dùng số dư kiếm trong game; mua bằng giỏ hàng, không nạp tiền. [Hướng dẫn](docs/UPDATE-7.4.4.md).

## Bốn vũ khí 7.4.4

Sau migration shop, chạy [migration vũ khí](supabase/migrations/202611050002_gunny_arsenal.sql), cập nhật game và server 7.4.4 (`arsenal:2`). [Chi tiết](docs/UPDATE-7.4.4.md).

## Tiền mua sắm và trang bị 7.4.4

Sau SQL shop/vũ khí, chạy [migration trang bị và ví](supabase/migrations/202611050003_gunny_equipment.sql). Tỷ lệ 100 xu chiến đấu = 1 tiền mua sắm; đồ đang mặc có chỉ số thực trong trận. Cập nhật cả game/server (`equipment:1`). [Hướng dẫn](docs/UPDATE-7.4.4.md).

## Web tải game

`https://TEN-SERVER.onrender.com/download/` có JAD/JAR cả E72 và E6, bản vá và hướng dẫn HTML/PDF. **Giữ thư mục public trong repo** vì Docker cần đóng gói các file này. Trang không dùng JavaScript hay tài nguyên bên ngoài. Nokia chưa truy cập HTTPS được có thể nhận file từ máy tính hoặc dùng gói `Gunny-download.zip` qua HTTP trong cùng Wi-Fi.

## Thẻ danh sách phòng

Thẻ dùng nhãn Tự do / Khiêu chiến và ảnh map theo phòng thật. Khóa ô đồng minh hiện 1/1, mở trống hiện 1/2; ô có AI cũng tính đã có người. Cần cập nhật cả game và server (`cards:1`). [Chi tiết 7.4.4](docs/UPDATE-7.4.4.md).

## Thanh công cụ trong phòng

Năm biểu tượng dùng lại hình và viền vàng của sảnh Online, đặt sát nhau dưới phòng; giữ tooltip và hỗ trợ chạm/kéo/thả. Khung bản đồ thay Quan chiến. [Chi tiết 7.4.4](docs/UPDATE-7.4.4.md).

## Thiết lập phòng

Bảng tạo phòng dùng asset gốc; tạo thường hiện Thi đấu, tạo Tổ đội hiện Tổ đội. Có khóa mật mã và hỗ trợ bốn phím hướng. [Chi tiết 7.4.4](docs/UPDATE-7.4.4.md).

## Đạo cụ

Chọn ba ô Z/X/C trong phòng, mỗi đạo cụ dùng một lần mỗi trận và tốn thể lực. Q/W/E/R/T và B giữ buff cũ; F đổi vũ khí. [Chi tiết sáu đạo cụ và thể lực](docs/UPDATE-7.4.4.md).

## Phòng và lượt

Đấu đơn chờ và ghép người chơi thật ở màn VS. Tổ đội có 6 ô mở cho trận 3v3, đủ 6 người sẵn sàng sẽ tự bắt đầu. Tạo phòng mở thiết lập riêng, giữ giao diện phòng cổ điển.

Thao tác và bắn chạy ngay trên client bằng vật lý offline; server kiểm tra và xử lý ngầm. Máy còn lại nhận và xem tuần tự đoạn di chuyển, POW và từng loạt đạn sau khi server xử lý xong đoạn đó. Không phải chờ cả lượt. Rào cuối lượt chờ các máy phát lại xong trước khi chạy đồng hồ lượt kế tiếp. [Cách cập nhật 7.4.4](docs/UPDATE-7.4.4.md). Kết quả dùng ảnh gốc, sau đó lật 1 trong 9 thẻ để nhận xu; SQL bảo đảm không cộng thưởng hai lần.

## Chạy thử

JDK 17 trở lên. Chạy `./verify.ps1` để kiểm tra HTTP, giao thức cũ/mới, tài khoản mock, bản đồ, thưởng lặp và tải file an toàn. Chạy `./backend/run-local.ps1 -TestAccounts` để tạo server localhost 8080 với tài khoản thử trong RAM. Chế độ thử không dùng được trên Render. Game giả lập nhập `http://127.0.0.1:8080`.

Đã thử với 6 MIDlet trong MicroEmulator ở 320×240 và 640×480. Chưa thử bản này trên Nokia vật lý hay Supabase/Render thật. Phòng đang chơi vẫn ở RAM; Render khởi động lại sẽ mất trận chưa lưu. Hồ sơ và phần thưởng đã ghi được lưu tại Supabase.

[Hướng dẫn cập nhật, email, xu và web](docs/UPDATE-6.3.1.md)
