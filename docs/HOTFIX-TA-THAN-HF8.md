# Gunny 7.9.18 Hotfix HF8

## Mục tiêu
Chuẩn hóa font và vị trí các tiêu đề lớn trên client theo kiểu chữ đậm giống tiêu đề "Xếp hạng".

## Đã sửa
- `OnlineCreate.java`
  - Chuẩn hóa tiêu đề loại phòng: `Ải viễn chinh`, `Thi đấu`, `Tổ đội` về cùng font lớn.
  - Căn giữa theo một trục cố định, tránh lệch vị trí giữa 3 chế độ.
- `BagScreen.java`
  - Đổi tiêu đề `Túi` sang font lớn.
  - Giữ `(B)` bằng font nhỏ để vẫn nhìn thấy hotkey trên E72.
- `ForgeScreen.java`
  - Đổi tiêu đề `Tiệm rèn` sang font lớn, hạ thấp xuống vùng nền vàng cho rõ hơn.
- `OnlineSocial.java`
  - Đổi tiêu đề popup `Bạn bè` / `Mời` sang font lớn, căn giữa.
- `GamePresentation.java`
  - Đổi tiêu đề `Thiết lập` sang font lớn, căn giữa khung.
- `LobbyChat.java`
  - Đổi tiêu đề `Trò chuyện` sang font lớn, căn giữa.

## Ghi chú
- HF8 chỉ sửa **client UI**, không thay đổi server.
- Nếu server đã ở HF6/HF7 thì chỉ cần giải nén đè repo và build lại JAR.
