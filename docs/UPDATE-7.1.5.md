# Cập nhật Gunny Java 7.1.5

Không cần SQL mới. Không đổi giao thức trận đấu, vật phẩm hoặc tài khoản; game dùng được với server 7.1.3/7.1.4 có `cards:1` và `items:2`. Gói server mới cập nhật trang tải và các bộ cài 7.1.5.

## Túi đồ PC

- Bố cục hai cột: trái là nhân vật và các ô đang mặc; phải là lưới đồ trong túi. Chỉ giữ hai tab Trang bị và Thời trang.
- Bỏ các mục thông tin/chỉ số, tu luyện, thẻ bài, vật tổ, châu báu, phụ kiện, ma thạch, miếu thần, thừa kế, kho và mật mã khỏi cửa sổ túi.
- Dùng bitmap túi PC gốc từ `e83c3e1ceb91075f8311.swf`: nền nhân vật, ô đồ đang mặc, tab Trang bị và Thời trang. Ô lưới từ `7553dad733fe38904604.swf`, symbol `bagAndInfo.cell.bagCellBgAsset` số 302. Khung cửa sổ, X và nút dùng asset UI gốc đã có trong dự án; không cắt ảnh tham khảo của người dùng.
- Trang bị chỉ liệt kê vũ khí đã sở hữu. Thời trang giữ sáu loại đồ sẵn có: nguyên bộ, áo, mũ, tóc, mặt, kính; 25 ô mỗi trang. Bấm ô đang mặc để lọc loại tương ứng; bấm dòng lọc để trở về Toàn bộ.
- Bốn phím hướng di chuyển giữa ô đồ, ô đang mặc, tab, trang, nút Trang bị và X. OK tại đồ để mặc; chạm chọn đồ rồi bấm Trang bị. B ở sảnh mở túi; B hoặc phím mềm phải đóng túi, B trong trận vẫn là POW.
- X/phím trở về giữ đúng nguồn mở túi: sảnh chính, popup đăng nhập, sảnh online hoặc phòng. Đóng túi giải phóng các ảnh dùng riêng.
- Quy tắc thay đồ online giữ như server cũ: thay đồ ở sảnh. Khi đang trong phòng, túi báo rời phòng trước và không đổi hình nhân vật cục bộ gây lệch với máy chủ. Vũ khí chưa sở hữu vẫn mua qua cửa hàng.

## Kiểm tra

Kiểm tra bằng MicroEmulator tại 320×240 và 640×480: bố cục, toàn bộ điều hướng, phân trang, lọc, mặc vũ khí/nguyên bộ/phối đồ, RMS và tài khoản Memory trên server localhost. Kiểm tra bộ cài JAD/JAR và toàn bộ gói server bằng verify.ps1. Chưa thử trên Nokia thật. Không tự triển khai máy chủ.
