# Gunny Java Online 7.8.8

Source hiện tại để đẩy Git và deploy trực tiếp bằng Xubi. Không cần ZIP Pi 5.

- Client Nokia E72/Asha 302: 320×240; E6: 640×480. JAR/JAD hiện tại ở `public/downloads`.
- Backend Java 17+, TCP game và HTTP web/admin. Docker build trực tiếp từ `backend/src`, `backend/assets` và `public`.
- Cường hóa xác suất +15, đá cấp 1–5, bùa may mắn/bảo vệ; hòm cấp, túi phó bản và thẻ VIP có thời hạn.
- Huyệt Ma Kiến và Bộ Lạc Tà Thần dùng chiến đấu tổ đội, AI và thưởng do server xác nhận.
- Bản này sửa thẻ thưởng bị về 0, điều hướng Shop, chọn ải và bố cục phòng phó bản. HP gốc đạt 5.000 ở cấp 70.

## Cập nhật bằng Xubi

Đẩy toàn bộ thay đổi của repo này lên nhánh `main`, gồm cả việc xóa file cũ và bốn JAR/JAD 788. Trong Xubi chọn đúng repo/nhánh rồi Deploy. Trang `/health` và `/download/` phải hiện 7.8.8; tải lại JAR/JAD để cập nhật điện thoại.

Không sửa `gunny.env` khi cập nhật. Cổng container: 8080 HTTP, 22122 TCP; Xubi có thể ánh xạ 18080:8080 và 22123:22122. Địa chỉ client mặc định `socket://gunnyjava.xubi.org:22123`.

Không có SQL mới riêng cho 7.8.8. Database đã cập nhật đủ 7.8.7 không cần chạy SQL nữa. Nếu còn schema 7.7.0, dùng [SQL cập nhật gộp](supabase/UPDATE-CURRENT.sql); cài mới dùng [GUNNY-ALL.sql](supabase/GUNNY-ALL.sql). Các migration trong `supabase/migrations` được giữ làm source schema. Không có thao tác tự chạy SQL khi khởi động server.

## Build và kiểm tra

```sh
docker build -t gunny-online:7.8.8 .
bash verify.sh
```

Windows chạy `./verify.ps1`. Build client:

```sh
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Xem [báo cáo hiện tại](docs/BAO-CAO-7.8.8.md), [hướng dẫn client](client/README.md), [quản trị](docs/QUAN-TRI.md), [Pi/systemd](deploy/pi5/README.md) và [Portainer](deploy/portainer/README.md).

Các bản phát hành, ZIP, ảnh kiểm thử và backup cũ đã được dọn. Git vẫn giữ lịch sử commit; bản clone nhẹ có thể dùng `git clone --depth 1 <URL-repo>`.
