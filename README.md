# Gunny Online — Render + Supabase

Bộ server độc lập để đưa lên GitHub một lần, rồi triển khai Render Free và kết nối Supabase Free. Không cần thư mục `GunnyNokia`, `Resource` hay `gunny-main` ở bên ngoài repo để build server.

## Cấu trúc

```text
gunny-online-main/
├── README.md
├── Dockerfile
├── render.yaml
├── .dockerignore
├── .gitignore
├── .env.example
├── verify.ps1
├── backend/
│   ├── src/           # Máy chủ Java và luật trận dùng chung với game
│   ├── assets/        # Địa hình cần cho server tính va chạm
│   ├── tests/         # Kiểm thử HTTP và tích hợp Supabase REST
│   └── run-local.ps1
├── supabase/
│   └── migrations/
│       └── 202611030001_gunny.sql
└── docs/
    └── CAI-DAT.md
```

## Đưa lên GitHub và triển khai

1. Tạo repo GitHub riêng, nên để **Private**. Upload **toàn bộ nội dung bên trong** thư mục này vào gốc repo. Sau khi upload phải thấy `Dockerfile` và `render.yaml` ngay ở trang đầu, không nằm trong một thư mục lồng thêm.
2. Tạo project Supabase riêng cho game. Chạy [file SQL khởi tạo](supabase/migrations/202611030001_gunny.sql) trong **SQL Editor**.
3. Render → **New → Web Service** → chọn repo → **Docker**, **Free**, Singapore. **Root Directory để trống**, Dockerfile `./Dockerfile`, Health Check `/health`. Hoặc dùng Blueprint với `render.yaml` có sẵn.
4. Thêm ba biến trong Environment của Render:

| Biến | Giá trị lấy từ Supabase |
| --- | --- |
| `SUPABASE_URL` | Project URL, dạng `https://PROJECT.supabase.co` |
| `SUPABASE_PUBLISHABLE_KEY` | Publishable key |
| `SUPABASE_SECRET_KEY` | Secret key, chỉ ở server |

5. Deploy và mở `https://TEN-SERVER.onrender.com/health` để kiểm tra. Game Java → **Online (O)** → nhập URL gốc Render → đăng nhập.

**Không điền khóa thật vào `.env.example` rồi upload GitHub.** File đó là mẫu; nhập khóa trong Environment của Render. Không cần nhập API key vào điện thoại.

Đọc [hướng dẫn đầy đủ](docs/CAI-DAT.md) để cấu hình tài khoản/email, tạo phòng và kiểm tra hai người chơi.

## Chạy thử trên máy tính

Cần JDK 17 trở lên. Tại thư mục này:

```powershell
.\backend\run-local.ps1 -TestAccounts
```

Nhập `http://127.0.0.1:8080` trên giả lập cùng máy. Chế độ này chỉ bind localhost và dùng tài khoản thử trong RAM; không phải server cloud.

Chạy bộ kiểm tra:

```powershell
.\verify.ps1
```

## Trạng thái

Code đã được thử với hai phiên game Java qua HTTP thật và trên cả 320×240, 640×480. Repo này là bộ triển khai, chưa có URL Render/Supabase đang chạy sẵn. Các bản JAR điện thoại được cung cấp riêng, không cần đưa vào repo server.

Render Free có thể ngủ và khởi động lại; phòng/trận nằm trong RAM sẽ mất khi server khởi động lại. Hồ sơ và phần thưởng đã ghi thành công được lưu ở Supabase. Kết nối HTTPS trên Nokia vật lý vẫn cần thử sau khi triển khai.
