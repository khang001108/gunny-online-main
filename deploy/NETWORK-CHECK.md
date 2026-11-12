# Kết quả kiểm tra 12/11/2026

| Hạng mục | Kết quả |
| --- | --- |
| TCP `127.0.0.1:22122` | Qua: hai health frame và một API frame trên cùng socket |
| TCP `192.168.1.28:22122` | Qua: giao thức 1, TCP giữ kết nối, cầu TCP/API trả đúng lỗi chưa đăng nhập |
| HTTPS Caddy qua IP LAN với TLS hostname `gunnyjava.xubi.org` | Qua: chứng chỉ được xác minh, `/health` HTTP 200; trang tải game cũng truy cập được |
| DNS A `gunnyjava.xubi.org` | `14.244.241.25`, khớp IP công cộng quan sát từ host |
| TCP/HTTPS qua tên miền từ chính LAN | Timeout; chưa phân biệt được NAT loopback với lỗi đường vào Internet |
| Supabase thật, dùng cấu hình env hiện có | Auth settings HTTP 200; đọc profiles với các cột game cần HTTP 200; không ghi dữ liệu |
| Portainer `https://192.168.1.28:9443` | Đã xác thực bằng token người dùng cấp; endpoint local ID 3 và stack gunny-online ID 2 |

Đã biên dịch backend và chạy qua `Tcp768Check`, `TcpGameplayCheck`,
`CloudStoreCheck`, `ServerCheck`. Kiểm tra hai người qua TCP bao gồm vào phòng,
sẵn sàng, bắt đầu, chống lặp lệnh, quyền điều khiển lượt, bắn, giữ socket và logout.
Các tài khoản kiểm thử nằm trong RAM của server localhost riêng.

`TcpClient768Check` qua MicroEmulator cũng qua: `Connector.socket`, đăng nhập thật
qua gateway localhost, giữ nguyên một socket. Chưa thử điện thoại Nokia vật lý.
Kiểm tra Supabase thật ở trên chỉ đọc cấu hình/schema; chưa đăng nhập tài khoản
người chơi thật qua server đang chạy.

Đã sửa `.dockerignore` để image backend có đủ `hard0.dat` đến `hard5.dat`.
Ở bộ nguồn `GunnyNokia`, Dockerfile còn được bổ sung `Progression.java` và sáu
file hard tương ứng. Generator đóng gói được sửa cùng quy tắc hard để tránh mất
bản sửa khi sinh lại repo.

Sau khi được cấp token, đã kiểm tra container thật qua Portainer: `gunny-online`
đang chạy image `gunny-online:7.6.9`, user `gunny`, assets `/app/assets1`, TCP
`22122:22122`, HTTP `8080:8080`, không có bind mount. Log khởi động xác nhận hai
listener; có ghi nhận kết nối TCP từ IP công cộng, nhưng chưa xác nhận đăng nhập
hay chơi một trận thật từ mạng ngoài.

Container đang chạy độc lập, không mang nhãn Compose của stack. Stack
`gunny-online` vẫn dùng `eclipse-temurin:21-jre` và bind mount `/opt/gunny`; container
backup của stack đã dừng. Không redeploy stack cũ vì cấu hình đó khác container
đang phục vụ. Chưa build/redeploy image mới; các sửa đóng gói chỉ nằm ở mã nguồn.
Host chưa có Docker Compose CLI; mẫu stack đã được đọc kiểm tra YAML và mapping,
chưa được Compose thực thi.

Người dùng xác nhận website vào được qua 4G, Ubuntu lỗi website; game TCP qua 4G
chưa được thử. Kết hợp với HTTPS LAN qua đúng TLS hostname thành công, lỗi Ubuntu
rất phù hợp với NAT loopback khi tên miền phân giải ra IP công cộng.

Đã chuẩn bị `deploy/fix-ubuntu-dns.py` và chạy preview: thêm ánh xạ riêng trên Ubuntu
`gunnyjava.xubi.org -> 192.168.1.28` vào `/etc/hosts` cùng template cloud-init để giữ
qua lần tái tạo hosts. Script có sao lưu và có thể chạy lặp. Người dùng đã áp dụng thành công bằng sudo;
đã xác minh ánh xạ tồn tại ở cả hai file. Lệnh áp dụng:

```sh
sudo python3 /home/khazg/gunny/gunny-online-main/deploy/fix-ubuntu-dns.py --apply
```

Kiểm tra sau khi áp dụng đã qua: tên miền phân giải thành `192.168.1.28`, ba yêu cầu
trên cùng socket TCP thành công, cầu TCP/API hoạt động; HTTPS trả HTTP 200 với
chứng chỉ được xác minh. Lệnh kiểm tra:

```sh
python3 /home/khazg/gunny/gunny-online-main/deploy/check-network.py --tcp-host gunnyjava.xubi.org --web-url https://gunnyjava.xubi.org
```

Router gateway là `192.168.1.1`. Không có thay đổi nào lên router hoặc container
đang chạy trong lần kiểm tra này. Xem [hướng dẫn Portainer](portainer/README.md)
để chạy probe từ mạng 4G/5G.
