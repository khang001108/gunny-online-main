# Cài đặt Gunny Java 7.9.22

## Bộ cài trên điện thoại

Dùng một cặp JAD/JAR trong `public/downloads`:

- E72: `Gunny-E72-2009-Online-7922.jad` và `.jar`.
- E6: `Gunny-E6-Online-7922.jad` và `.jar`.

Hai file của mỗi cặp cần nằm cùng thư mục. Giữ tên MIDlet/Vendor để dữ liệu RMS và thiết lập trên máy tiếp tục đọc được. SHA256 của bốn file ở `Gunny-7922-SHA256.txt`.

Trong màn hình đăng nhập, chọn **Máy chủ**:

- 4G / mạng khác: `socket://gunnyjava.xubi.org:22123`.
- Wi-Fi cùng Pi: `socket://192.168.1.127:22123`.

Có thể sửa địa chỉ rồi bấm **Lưu**. Game dùng TCP cho đăng nhập, phòng, chiến đấu, túi, shop, chat và kết quả; không cần các gói TLS/HTTPS cũ. Trang tải, web quản trị và kết nối Supabase của backend vẫn dùng HTTPS.

## Backend và Docker

Cập nhật cả backend và JAR/JAD cùng phiên bản. Dockerfile biên dịch Java 17 và kiểm tra bộ cài hiện tại trước khi đóng image. Giữ cấu hình và dữ liệu của server đang chạy khi cập nhật.

Cổng TCP bên trong container là 22122; máy chủ hiện tại có thể ánh xạ cổng ngoài 22123 vào đó. Hướng dẫn công cụ cập nhật nằm ở [deploy/CAP-NHAT-DOCKER.md](../deploy/CAP-NHAT-DOCKER.md), [Portainer](../deploy/portainer/README.md) và [Pi 5](../deploy/pi5/README.md).

## Supabase

- Cài mới: xem `supabase/GUNNY-ALL.sql`.
- Project đang có dữ liệu: dùng `supabase/UPDATE-CURRENT.sql` theo trạng thái schema hiện tại; giữ chuỗi `supabase/migrations` để đối chiếu và cập nhật các database đã cài.
- Không chạy lại SQL cài mới lên project đang có tài khoản. Repo và image không tự chạy SQL khi khởi động.
- Khóa dịch vụ đặt ở backend, không đưa vào JAR hoặc Git.

Tài khoản dùng email làm tên đăng nhập; backend xác nhận email khi tạo tài khoản, không gửi thư. UUID giữ vai trò liên kết nhân vật, tiền, vật phẩm và dữ liệu xã hội. Xem [quản trị](QUAN-TRI.md) cho chính sách tài khoản và bảo trì.

## Build và kiểm tra

Hướng dẫn công cụ CLDC/MIDP và build: [client/README.md](../client/README.md). Backend có `verify.ps1` / `verify.sh`; các kiểm tra riêng nằm trong `backend/tests` và `client/tests`.

Thông tin kiểm tra đạn/nổ thường và Bpow, cùng những test cũ đang lỗi, được ghi ở [WEAPON-COMBAT-E72.md](WEAPON-COMBAT-E72.md). Chưa đo FPS trên điện thoại Nokia thật.
