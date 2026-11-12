# Cập nhật Gunny Java 7.1.6 — Shop PC

Cập nhật game và server. **Có migration SQL mới**: `supabase/migrations/202611050001_gunny_shop.sql` trong gói server (bản nguồn: `server/shop-716.sql`). Chạy sau các migration cũ, không xóa hay chạy lại schema gốc. Gói chỉ chuẩn bị file; chưa chạy SQL hoặc triển khai server thay người dùng.

## Shop

- Bên trái là phòng thử và nhân vật xem trước; bên phải là món hàng. Chỉ giữ Trang bị và Làm đẹp, bỏ giới thiệu/đạo cụ/huy chương, tặng/xin và các mục phụ.
- Trang bị: 4 vũ khí hiện tại, áo, mũ, kính. Làm đẹp: tóc, mặt, nguyên bộ. Mỗi loại trang phục có 10 món, phân trang 6 món/trang. Chưa thêm vũ khí thứ 5 hoặc các loại trang sức/cánh chưa có luật trong game.
- Chạm món hoặc OK để thêm/bỏ giỏ; bốn phím hướng cũng xem thử được. Đổi nhóm không xóa giỏ. Bấm biểu tượng xe giỏ hàng để mua cả giỏ bằng Xu, chính là số dư kiếm từ chơi game trước đây; không có nạp tiền hoặc tiền mặt.
- Xem thử không đổi đồ đang mặc hoặc lưu lên tài khoản. Mua thêm vào túi, không tự mặc. Vào túi để trang bị sau khi mua.
- Giữ giá 4 vũ khí: lựu đạn 0, lu gạch 450, phi tiêu 800, sấm sét 1200 xu. Nguyên bộ 300, áo/mũ/tóc 150, mặt/kính 100 xu mỗi món. Đã sở hữu không bị tính tiền lần nữa.
- Asset từ `Source Flash/FlashSV1/ui/vietnam/swf/shop.swf`: BodyInfoBg, RankingCellItemBg, LeftMoneyPanelBuyBtn, tab Trang bị/Làm đẹp và nút chuyển trang. Không cắt giao diện từ ảnh tham khảo.

## Túi và tài khoản

Túi chỉ hiện đồ đã có. Mỗi loại có món số 1 ban đầu; bộ và các món đang mặc trước cập nhật được giữ. Các món trước đây chỉ được liệt kê miễn phí, chưa mặc/chưa mua, nay nằm trong shop. Offline lưu quyền sở hữu trong RMS riêng, đọc bộ đang mặc hiện có khi nâng cấp lần đầu. Online lưu 12 mặt nạ sở hữu (6 loại × 2 giới tính) trong cột wardrobe.

Máy chủ tự tính giá từ catalog, không nhận giá hoặc số dư do client khai. Checkout SQL khóa hàng tài khoản và xử lý cả giỏ trong một giao dịch; thiếu xu không trừ tiền hoặc cấp một phần đồ. Gửi lại cùng giỏ không mua trùng đồ đã có. Máy chủ cũng kiểm tra sở hữu khi mặc trang phục.

## Cập nhật

1. Sao lưu dữ liệu theo cách quản trị hiện dùng. Chạy riêng migration shop mới trong Supabase SQL Editor.
2. Cập nhật toàn bộ gói server; `/health` phải có `shop:1`, `items:2`, `cards:1`.
3. Cài JAR/JAD 7.1.6 đúng E72 hoặc E6. Shop bản mới yêu cầu server có shop:1 để mua online. Thay/mua đồ ở sảnh trước khi vào phòng.

Không sửa tiền hoặc đồ từ file client để cấp vào tài khoản online. Chỉ service_role được gọi RPC checkout; khóa không có trong JAR.

## Xác minh

Kiểm tra giả lập hai độ phân giải, xem trước độc lập, giỏ nhiều món, thiếu xu, phân trang, điều hướng, lưu RMS, mua rồi mặc đồ online. Kiểm tra HTTP server với Memory: tính giá, giỏ lỗi, yêu cầu lặp/concurrent, từ chối mặc đồ chưa mua và mua trong phòng; mock Supabase xác minh RPC và cột wardrobe. Migration được kiểm tra cú pháp SQL/PLpgSQL, chưa chạy trên Supabase thật. Chưa thử trên điện thoại Nokia thật.
