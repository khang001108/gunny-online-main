# Chạy Gunny trên Raspberry Pi 5

Pi cần Raspberry Pi OS 64-bit, Java 17 trở lên, địa chỉ LAN cố định và tên miền HTTPS cho web. TCP game dùng cổng 22122; web dùng Caddy ở 80/443.

## 1. Chuẩn bị mã chạy

Chép toàn bộ gói `gunny-online-main` vào Pi, rồi tại thư mục đó chạy:

```sh
sudo apt update
sudo apt install -y openjdk-17-jdk-headless caddy
mkdir -p backend/build
javac --release 17 -encoding UTF-8 -d backend/build backend/src/*.java
sudo useradd --system --home /opt/gunny --shell /usr/sbin/nologin gunny 2>/dev/null || true
sudo mkdir -p /opt/gunny/classes /etc/gunny
sudo cp -r backend/build/. /opt/gunny/classes/
sudo cp -r backend/assets /opt/gunny/assets1
sudo cp -r public /opt/gunny/public
```

## 2. Cấu hình bí mật

Chép `deploy/pi5/gunny.env.example` thành `/etc/gunny/gunny.env`, điền URL/key Supabase, mật khẩu admin và tên miền thật. Sau đó:

```sh
sudo chown -R gunny:gunny /opt/gunny
sudo chown root:gunny /etc/gunny/gunny.env
sudo chmod 640 /etc/gunny/gunny.env
```

Không đưa file `gunny.env` lên GitHub và không đặt secret Supabase vào JAR/JAD.

## 3. Bật dịch vụ

```sh
sudo cp deploy/pi5/gunny.service /etc/systemd/system/gunny.service
sudo systemctl daemon-reload
sudo systemctl enable --now gunny
sudo systemctl status gunny
```

Kiểm tra tại Pi:

```sh
curl http://127.0.0.1:8080/health
sudo journalctl -u gunny -f
```

## 4. Web HTTPS và email xác nhận

Sửa tên miền trong `deploy/pi5/Caddyfile.example`, chép nội dung vào `/etc/caddy/Caddyfile`, rồi chạy:

```sh
sudo systemctl reload caddy
```

Trong Supabase Authentication → URL Configuration, đặt Site URL là `https://TEN-MIEN-CUA-BAN` và thêm Redirect URL `https://TEN-MIEN-CUA-BAN/auth/confirmed`.

## 5. Mở cổng game

Trên router, chuyển tiếp TCP 22122 về IP LAN của Pi 5. Chỉ cho phép 80/443 và 22122 qua tường lửa; không mở thẳng 8080. Nếu nhà mạng dùng CGNAT thì port-forward sẽ không vào được từ Internet; cần IP công cộng hoặc một dịch vụ tunnel hỗ trợ raw TCP.

Trong game nhập:

```text
socket://TEN-MIEN-HOAC-IP-CONG-CONG:22122
```

TCP thô không mã hóa email/mật khẩu. Với người chơi ngoài mạng nhà, nên đặt một lớp tunnel/mã hóa tương thích trước cổng này hoặc chỉ phát tài khoản riêng.
