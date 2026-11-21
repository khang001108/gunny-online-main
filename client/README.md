# Client Java ME

Mã nguồn client được lấy từ `D:/gunny/GunnyNokia/src` để có thể sửa và build ngay trong repo này. Không đưa thư viện build hoặc tài nguyên trùng lặp vào Git; tài nguyên được giữ từ các JAR phát hành hiện có.

Trong màn hình đăng nhập/đăng ký, chọn **Máy chủ**:

- **4G / mạng khác:** `socket://gunnyjava.xubi.org:22123`.
- **Wi-Fi cùng Pi:** `socket://192.168.1.28:22123`.

Có thể sửa cả hai địa chỉ. Chọn mạng rồi bấm **Lưu**. Địa chỉ được lưu trên điện thoại và được ưu tiên hơn mặc định trong JAD khi mở lại game. **Trở về** bỏ các thay đổi chưa lưu. Hai địa chỉ mặc định cũ ở cổng 22122 được chuyển sang 22123.

Cài lại bộ cài mới để có màn hình này; chỉ thay JAD không cập nhật được giao diện trong JAR. Hệ điều hành điện thoại có thể hỏi có giữ dữ liệu ứng dụng khi cập nhật.

## Build

Cần Java, Python và thư mục công cụ chứa `ecj.jar`, `proguard.jar` (6.2.2), `cldcapi11.jar`, `midpapi20.jar`:

```powershell
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Script biên dịch CLDC 1.1 / MIDP 2.0, preverify và cập nhật hai JAR/JAD 7.8.7 cùng ZIP cài E6 trong `public/downloads`. Cần giữ các bộ cài hiện tại làm nguồn tài nguyên khi build. `client/build` chỉ chứa file sinh ra.

## Kiểm thử màn hình và lưu cấu hình

Cần thêm `microemulator.jar`, `mmapi.jar` trong thư mục công cụ:

```powershell
javac --release 17 -encoding UTF-8 -cp 'public/downloads/Gunny-E6-Online-783.jar;D:/gunny/GunnyNokia/tools/microemulator.jar' -d client/build/test client/tests/ServerSettingsCheck.java
java -Djava.awt.headless=true -cp 'client/build/test;public/downloads/Gunny-E72-2009-Online-783.jar;D:/gunny/GunnyNokia/tools/microemulator.jar;D:/gunny/GunnyNokia/tools/mmapi.jar' ServerSettingsCheck 1
java -Djava.awt.headless=true -cp 'client/build/test;public/downloads/Gunny-E6-Online-783.jar;D:/gunny/GunnyNokia/tools/microemulator.jar;D:/gunny/GunnyNokia/tools/mmapi.jar' ServerSettingsCheck 2
```

Cổng client 22123 là cổng bên ngoài của `gunny-xubi`. Cổng lắng nghe bên trong container có thể vẫn là 22122 nếu Docker ánh xạ `22123:22122/tcp`; thay đổi này không sửa cấu hình máy chủ đang chạy.

## Tr? v? ph?ng sau tr?n

PvP v? ph? b?n d?ng l?nh `returnroom` sau khi nh?n th??ng ?? gi? nguy?n ph?ng, v? tr? ng??i ch?i v? bot. Ng??i v? tr??c ? ph?ng ch? trong khi ??ng ??i nh?n th?; khi t?t c? ?? v?, tr?n ???c ??t l?i v? c? th? s?n s?ng ch?i ti?p. Th?ng ?i 1 v?n chuy?n sang ph?ng ch? ?i 2. Ho?n th?nh ho?c thua ph? b?n quay v? thi?t l?p ?i 1. C?n c?p nh?t c? backend v? b? c?i client m?i.
