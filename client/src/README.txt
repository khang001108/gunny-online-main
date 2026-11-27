Gunny Java 7.9.20 HF44 - Fix hiển thị sát thương chí mạng / BPow

Nội dung:
- Tách kiểu hiển thị popup sát thương thành 3 loại:
  1) Thường: số vàng thường.
  2) Chí mạng / hiệu ứng may mắn / xuyên thủ đặc biệt: số đỏ, KHÔNG nền.
  3) BPow: nền nổ đỏ + số vàng.
- Đồng bộ giao thức snapshot popup giữa backend và client (boolean -> byte style).

File đã sửa:
- client/src/Battle.java
- client/src/OnlineClient.java
- client/src/PhoneCanvas.java
- backend/src/Battle.java
- backend/src/GunnyServer.java

Lưu ý:
- Client và server phải cập nhật cùng lúc vì dữ liệu popup sát thương đã đổi định dạng truyền.
- Không đổi công thức sát thương, chí mạng, may mắn hay BPow. Chỉ đổi hiển thị.
- Đã kiểm tra biên dịch backend bằng javac OK.
- Chưa build JAR Java ME trong môi trường này vì thiếu toolchain MIDP/CLDC.
