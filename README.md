# Gunny Java 7.9.22

Repo hiện tại chứa backend, client Java ME và bộ cài E72/E6 cùng phiên bản.

| Thư mục / file | Mục đích |
|---|---|
| `backend/src`, `backend/assets` | Server và bản đồ va chạm |
| `client/src` | Source client; các lớp luật chiến đấu dùng chung với backend |
| `client/assets1`, `client/assets2` | Tài nguyên bổ sung đang dùng cho E72 / E6 |
| `client/source-assets`, `client/tools`, `client/wing-config` | Nguồn ảnh, công cụ tái tạo và cấu hình cánh |
| `public/downloads` | Hai cặp JAR/JAD 7.9.22 và file SHA256 |
| `public/admin`, `public/account` | Web quản trị và tài khoản |
| `supabase` | SQL cài mới, cập nhật hiện tại và chuỗi migration |
| `deploy`, `Dockerfile` | Cấu hình và công cụ triển khai |
| `backend/tests`, `client/tests` | Kiểm tra server, giao thức và MIDP |
| `Gunny-Wing-Studio-V3.1.html` | Công cụ chỉnh cánh độc lập, chứa tài nguyên gốc nhúng sẵn |

Build client từ thư mục gốc:

```powershell
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Giữ các JAR hiện tại vì script dùng chúng làm nguồn tài nguyên nền khi build lại. Thư mục `client/build` và `backend/build` được tạo lại khi build/test; không cần lưu trong repo.

Hướng dẫn: [cài đặt](docs/CAI-DAT.md), [client và build](client/README.md), [quản trị](docs/QUAN-TRI.md), [đạn/nổ thường và Bpow](docs/WEAPON-COMBAT-E72.md), [dọn repo](docs/CLEANUP-REPO.md).
