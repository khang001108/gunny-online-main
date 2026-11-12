# Docker / Portainer và TCP online

Mẫu `compose.yaml` dành cho Caddy chạy trực tiếp trên host, reverse proxy về
`127.0.0.1:8080`, giống cấu hình hiện có. TCP game publish riêng `22122:22122/tcp`.
Nếu Caddy chạy trong container, cần dùng network chung và upstream `gunny:8080`.

## Image và stack

Tại thư mục gốc repo, build image trên Docker endpoint sẽ chạy game:

```sh
docker build -t gunny-online:7.7.0 .
```

Trong Portainer, chọn đúng Environment → stack game hiện có. Đối chiếu và cập nhật
stack đó theo `compose.yaml`; không tạo container thứ hai chiếm cùng cổng. Điền các
biến Supabase và admin bằng cấu hình đang dùng trong mục Environment variables.
`GUNNY_IMAGE` là image đã build ở trên hoặc image từ registry riêng của bạn.
Không dùng đường dẫn assets systemd `/opt/gunny/assets1` trong Docker; image dùng
`/app/assets1`. Không bật `GUNNY_TEST_MODE` trên server công khai.

Cập nhật container sẽ ngắt trận đang nằm trong RAM. Chỉ cập nhật sau khi đã kiểm tra
stack thực tế và chọn thời điểm phù hợp.

## Đường mạng cần có

| Dịch vụ | Router chuyển tiếp đến | Xử lý trên host |
| --- | --- | --- |
| Game raw TCP | `192.168.1.28:22122/TCP` | Docker → `gunny:22122` |
| Web HTTP | `192.168.1.28:80/TCP` | Caddy |
| Web HTTPS | `192.168.1.28:443/TCP` | Caddy → `127.0.0.1:8080` |

Giữ IP LAN của host cố định. Tên miền game phải trỏ trực tiếp về IP công cộng hoặc
dịch vụ tunnel thực sự hỗ trợ raw TCP. Reverse proxy HTTPS không chuyển giao thức
socket game. Nếu có hai router, cần cấu hình cả hai; nếu WAN bị CGNAT cần đường
vào hỗ trợ raw TCP hoặc IP công cộng.

## Kiểm tra đúng giao thức

Từ thư mục gốc repo:

```sh
python3 deploy/check-network.py --tcp-host 127.0.0.1 --tcp-host 192.168.1.28 --web-url https://gunnyjava.xubi.org --web-connect-ip 192.168.1.28
```

Lệnh xác minh ba yêu cầu trên cùng socket, giao thức game, cầu TCP/API và chứng chỉ
HTTPS. Không đăng nhập, tạo tài khoản hay ghi dữ liệu. `/health` qua HTTP hiện trả
`tcp:0,longpoll:1` theo transport của yêu cầu; không dùng trường đó để kết luận TCP
đã tắt. Probe TCP phải trả `tcp:1,longpoll:0`.

Từ máy ở mạng khác (4G/5G hoặc Internet khác), chạy:

```sh
python3 deploy/check-network.py --tcp-host gunnyjava.xubi.org --web-url https://gunnyjava.xubi.org
```

Trong game dùng `socket://gunnyjava.xubi.org:22122`. Để thử riêng trong Wi-Fi nhà,
nhập `socket://192.168.1.28:22122` vào form địa chỉ game. Nếu mạng ngoài vào được
nhưng tên miền không vào được trong Wi-Fi nhà, kiểm tra NAT loopback hoặc cấu hình
DNS nội bộ trỏ tên miền về IP LAN. Một lần timeout từ LAN qua IP công cộng chưa đủ
để kết luận cổng Internet bị đóng.

Trước khi cập nhật lên 7.7.0, chủ máy tự chạy migration 7.7.0 theo docs/UPDATE-7.7.0.md và xác nhận đã hoàn tất. Container không tự chạy SQL.
