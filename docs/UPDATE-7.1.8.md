# Gunny Java 7.1.8 — Ví mua sắm, phòng thử và chỉ số trang bị

**Cập nhật cả game và server, chạy SQL mới** `supabase/migrations/202611050003_gunny_equipment.sql` sau SQL shop 7.1.6 và vũ khí 7.1.7. Không chạy lại schema gốc. Gói chỉ chuẩn bị file, chưa chạy SQL hoặc triển khai lên dịch vụ thật.

## Hai số dư

- Xu chiến đấu giữ trong `coins`, vẫn nhận từ kết quả và thẻ thưởng. Giữ nguyên toàn bộ số dư cũ.
- Tiền mua sắm dùng biểu tượng tờ tiền vàng gốc, lưu riêng trong `shopping_money`; khởi đầu 0 khi nâng cấp.
- Tab Đổi xu bên cạnh Làm đẹp: **100 xu chiến đấu = 1 tiền mua sắm**. Ô nhập hỏi số tiền mua sắm muốn nhận; phần xu dư giữ lại.
- Giá shop giữ con số cũ nhưng đơn vị là tiền mua sắm. Checkout và mua vũ khí chỉ trừ ví này. Không có nạp tiền thật.
- Máy chủ tự tính tỷ lệ và giá. Đổi tiền có mã giao dịch để nhận lại cùng yêu cầu không trừ thêm lần nữa; SQL khóa tài khoản và ghi nhận đổi tiền trong cùng giao dịch.

## Shop và túi

Shop dùng đúng khung nhân vật, ô mặc và hình món đang mặc của túi. Chọn món chỉ mặc thử vào ô tương ứng; mỗi loại một món, đồ mới thay món cũ. OK tại ô mặc thử để gỡ. Mặc nguyên bộ thay các món rời; chọn món rời chuyển sang phối đồ. Không thay đồ thật cho đến khi trang bị từ túi.

Shop cũng hỗ trợ phím mềm trái xem chỉ số trước khi mua. Thẻ hàng dùng nền nâu gốc, icon nằm trong ô riêng, tên và giá không đè lên icon. Giá và tổng thanh toán có biểu tượng tờ tiền vàng. Tổng chỉ tính món đang mặc thử chưa sở hữu; gỡ món là giảm tổng. Bấm xe giỏ hàng để mua vào túi, không tự mặc.

Trong túi:

- Phím mềm trái/Thông tin: tên, Công kích, Phòng thủ, May mắn; chỉ bốn dòng này.
- OK tại món trong túi để trang bị. OK tại ô đang mặc để gỡ. OK khi đang xem thông tin cũng thực hiện thao tác đó.
- Biểu tượng đi từ ô túi sang đúng ô mặc, hoặc ngược lại khi gỡ. Đồ vẫn thuộc tài khoản, không bị xóa/bán khi gỡ. Ô của đồ đã mặc để trống trong lưới túi, không hiện trùng hai nơi.
- Bỏ nút Trang bị và dòng OK: mặc phía dưới; giữ hai tab Trang bị/Thời trang.
- Có thể thay đồ trong phòng chờ; server đặt lại sẵn sàng và cập nhật avatar. Khóa thay đồ khi đã vào trận. Tháo vũ khí sẽ cần trang bị lại trước khi bắt đầu.

## Chỉ số có tác dụng thực

Mỗi món trang phục rời hoặc nguyên bộ: **Công kích 5 / Phòng thủ 5 / May mắn 5**. Một bộ đóng góp một lần 5; phối năm món rời đóng góp 25. Không cộng chồng nguyên bộ với các món rời.

| Vũ khí | Công kích | Phòng thủ | May mắn |
|---|---:|---:|---:|
| Lựu đạn | 290 | 40 | 20 |
| Lu gạch | 330 | 35 | 25 |
| Phi tiêu | 330 | 30 | 35 |
| Sấm sét | 330 | 25 | 20 |
| Tủ thuốc | 300 | 20 | 40 |
| Tủ lạnh | 370 | 65 | 5 |
| Giỏ đồ | 315 | 30 | 30 |
| Pháo | 390 | 25 | 15 |

Đây là chỉ số cân chỉnh cho bản Java ME. Công kích vũ khí + công kích trang phục là sát thương cơ sở trước buff. Máu tối đa = 2000 + tổng Phòng thủ. Thể lực tối đa mỗi lượt = 240 + tổng May mắn. Một điểm Phòng thủ thêm một máu, một điểm May mắn thêm một thể lực. Hồi máu và thanh HUD dùng đúng giới hạn mới. Đổi vũ khí F trong trận tính lại giới hạn nhưng không hồi miễn phí máu/thể lực khi đổi qua lại.

Mã `ItemRules.java` dùng chung cho tooltip, client và server. Server lấy quyền sở hữu và ô đang mặc từ hồ sơ, tự tính chỉ số khi bắt đầu; không nhận sát thương/máu/thể lực tự khai từ client. Thông tin giới hạn trong phát lại được suy ra từ cùng bộ trang bị và vũ khí của từng nhân vật.

## Phòng

Hai thẻ thường dùng nguyên hình native 124/127/180, giữ tỷ lệ khoảng 180:189; không kéo dài nền/ảnh chờ. Hạ thẻ xuống dưới hàng hiển thị Xu và Tiền mua sắm. Cập nhật vùng chạm, biểu tượng đóng ô, emoji và viền chọn theo vị trí mới. Phòng Tổ đội sáu ô giữ bố cục riêng.

## Cập nhật và tương thích

1. Nếu chưa chạy SQL shop/vũ khí, chạy hai file đó trước, theo thứ tự tên trong `supabase/migrations`.
2. Chạy riêng `202611050003_gunny_equipment.sql` trong Supabase SQL Editor. Giữ tiền và đồ cũ; số dư mua sắm mới mặc định 0. Các ô đang mặc được khởi tạo từ bộ/phối đồ hiện có.
3. Cập nhật toàn bộ server rồi cài JAR/JAD 7.1.8 cho E72 hoặc E6. /health cần có `equipment:1`, `arsenal:2`, `shop:1`.
4. Dùng tab Đổi xu để nhận tiền mua sắm theo tỷ lệ 100:1.

Phiên mới dùng `X-Gunny-Equipment: 1`. Sảnh, mời và ghép trận tách với phiên cũ để không trộn luật máu/thể lực khác nhau. Hồ sơ cơ bản vẫn giữ định dạng cũ; phần ví/ô mặc mở rộng chỉ gửi cho client có hỗ trợ. Offline lưu ví/ô mặc riêng trong RMS, không lẫn với hồ sơ online.

## Xác minh

Giả lập 320×240 và 640×480: khung shop/túi, thử/gỡ/tổng tiền, thông tin mềm trái, chuyển icon hai chiều, đổi xu, lưu riêng offline/online, thẻ phòng tự nhiên và hai số dư. Server Memory trên localhost kiểm tra đổi lặp/concurrent, thanh toán đúng ví, sở hữu, thay đồ phòng chờ, khóa khi vào trận, sát thương/máu/thể lực và giới hạn hồi phục. Mock Supabase kiểm tra các RPC và trường mới. Hai client kiểm tra đoạn bắn/phát lại với trang bị rời và nguyên bộ; địa hình khớp server rồi mới đổi lượt. Chạy lại bộ kiểm tra server và kiểm tra JAR/JAD/gói phát hành.

Migration được kiểm tra cú pháp SQL/PLpgSQL, chưa chạy trên Supabase thật. Chưa thử trên Nokia thật.
