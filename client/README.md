# Client Java ME

Mã nguồn client được lấy từ `D:/gunny/GunnyNokia/src` để có thể sửa và build ngay trong repo này. Không đưa thư viện build hoặc tài nguyên trùng lặp vào Git; tài nguyên được giữ từ các JAR phát hành hiện có.

Trong màn hình đăng nhập/đăng ký, chọn **Máy chủ**:

- **4G / mạng khác:** `socket://gunnyjava.xubi.org:22123`.
- **Wi-Fi cùng Pi:** `socket://192.168.1.127:22123`.

Có thể sửa cả hai địa chỉ. Chọn mạng rồi bấm **Lưu**. Địa chỉ được lưu trên điện thoại và được ưu tiên hơn mặc định trong JAD khi mở lại game. **Trở về** bỏ các thay đổi chưa lưu. Hai địa chỉ mặc định cũ ở cổng 22122 được chuyển sang 22123.

Cài lại bộ cài mới để có màn hình này; chỉ thay JAD không cập nhật được giao diện trong JAR. Hệ điều hành điện thoại có thể hỏi có giữ dữ liệu ứng dụng khi cập nhật.

## Build

Cần Java, Python và thư mục công cụ chứa `ecj.jar`, `proguard.jar` (6.2.2), `cldcapi11.jar`, `midpapi20.jar`:

```powershell
python client/build.py --tools D:/gunny/GunnyNokia/tools
```

Script biên dịch CLDC 1.1 / MIDP 2.0, preverify và cập nhật hai JAR/JAD 7.9.13 trong `public/downloads`. Cần giữ các bộ cài hiện tại làm nguồn tài nguyên khi build. `client/build` chỉ chứa file sinh ra.

## Kiểm thử màn hình và lưu cấu hình

Cần thêm `microemulator.jar`, `mmapi.jar` trong thư mục công cụ:

```powershell
javac --release 17 -encoding UTF-8 -cp 'public/downloads/Gunny-E6-Online-7913.jar;D:/gunny/GunnyNokia/tools/microemulator.jar' -d client/build/test client/tests/ServerSettingsCheck.java
java -Djava.awt.headless=true -cp 'client/build/test;public/downloads/Gunny-E72-2009-Online-7913.jar;D:/gunny/GunnyNokia/tools/microemulator.jar;D:/gunny/GunnyNokia/tools/mmapi.jar' ServerSettingsCheck 1
java -Djava.awt.headless=true -cp 'client/build/test;public/downloads/Gunny-E6-Online-7913.jar;D:/gunny/GunnyNokia/tools/microemulator.jar;D:/gunny/GunnyNokia/tools/mmapi.jar' ServerSettingsCheck 2
```

Cổng client 22123 là cổng bên ngoài của `gunny-xubi`. Cổng lắng nghe bên trong container có thể vẫn là 22122 nếu Docker ánh xạ `22123:22122/tcp`; thay đổi này không sửa cấu hình máy chủ đang chạy.

## Về phòng sau trận

Nhận đủ lượt sẽ mở cả bộ thẻ để xem thưởng; không cộng thêm vật phẩm từ các thẻ chưa chọn. Phó bản qua ải trung gian tiếp tục sang ải sau. Server xác nhận mọi phần thưởng và cập nhật HP theo cấp giống client.

Ảnh rèn gốc và ảnh thay thế nằm trong `client/assets1` / `client/assets2`; build chèn các ảnh này vào đúng bộ cài và loại hình đá cũ. Xem `client/ASSETS.json` cho nguồn tài nguyên.


## VIP / cánh và kiểm tra 7.9.6

Chọn thẻ VIP trong Túi → OK để dùng. Vào Thời trang chọn cánh → OK để mặc/tháo; ô Cánh ở góc dưới bên trái cũng tháo bằng OK. Shop có tab VIP ngay sau Đạo cụ. Toàn bộ thay đổi chờ server xác nhận.

`Ui790Check` chạy bằng cách thay tên lớp test ở ví dụ trên. Kiểm tra thêm VIP, cánh, crash tên rỗng ở danh sách mời, Bpow và xoay khung hình khi màn hình dọc. Dọc 240×320/480×640 xoay toàn bộ khung game và ánh xạ lại vị trí chạm; màn ngang không cấp phát thêm ảnh đệm này.

Asset còn thiếu và nguồn ảnh: `client/VIP-ASSETS.json`. Source chỉ có badge VIP1–9, dùng chữ rõ cấp cho VIP10–12. Vòng 12 mức được phối từ 3 mẫu gốc; thẻ dùng ảnh vipcard/vippack gốc kèm nhãn cấp. Chưa có đủ bộ icon/animation riêng 12 cấp để tái hiện nguyên bản hoàn toàn.

## Tà Thần 7.9.13

Ba ải theo video người dùng: 20 lính bộ lạc, anh em Bulan, Sulan vàng → đỏ. Sau mỗi 2 lượt boss gọi bù lính vàng 3+1, đỏ 5+2; lính có lượt đánh, chọn cả cây đậu và nhân vật. Cần cập nhật cả server và JAR/JAD. Không có SQL mới. Xem `../docs/BAO-CAO-7.9.13.md` và `../docs/TA-THAN-VIDEO.md`.
