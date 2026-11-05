# Tài khoản mới, xác nhận email, túi đồ và admin 7.5.3

## Cập nhật máy chủ và Supabase

Giữ bản sao lưu rồi cập nhật toàn bộ server. Chạy `supabase/UPDATE-7.5.3-ALL.sql` trong SQL Editor nếu từng thấy lỗi thiếu `gunny_equip`: file gồm các migration cần thiết theo thứ tự, kết thúc bằng 7.5.3. Không xoá tài khoản, số dư hoặc đồ cũ. Nếu chắc chắn đã chạy đủ SQL trước, chỉ cần migration `202611060003_gunny_features.sql`. Các defaults mới chỉ áp dụng cho tài khoản tạo sau khi chạy SQL; không thu hồi đồ của người đã chơi.

Trang admin thêm Cộng xu / tiền trong Quản lý tài khoản, số nguyên không âm, cần lý do, ví không vượt 999.999. Có xác nhận số cộng và log trong gunny_admin_audit/gunny_wallet_grants. UUID chống cộng trùng khi gửi lại cùng yêu cầu. Quyền chỉ dành cho admin/service_role. Số dư người đang ở sảnh cập nhật qua polling hiện có; không đổi sát thương hoặc vật lý trận.

## Xác nhận email

Ảnh chuyển tới localhost:3000 do cấu hình URL của Supabase, không phải điện thoại thiếu máy chủ cục bộ.

1. Supabase → Authentication → URL Configuration: Site URL là `https://gunny-online-main.onrender.com` (không để localhost).
2. Redirect URLs: thêm `https://gunny-online-main.onrender.com/auth/confirmed`.
3. Supabase → Authentication → Email Templates → Confirm signup: dán mẫu `supabase/templates/confirm-signup-vi.html` trong gói mới. Mẫu đưa TokenHash tới trang /auth/confirmed, xác minh thật qua server rồi mới chúc mừng. Không gửi khoá service_role cho trình duyệt. Liên kết cũ trỏ localhost không thể tự sửa; dùng thư mới hoặc đăng nhập nếu email đã được xác nhận.
4. Trên Render, tự dùng RENDER_EXTERNAL_URL; có thể đặt GUNNY_PUBLIC_URL nếu dùng tên miền riêng. Khi đổi miền nhớ đổi Site URL/Redirect URLs cùng miền.

Trang `/account/` cho người chơi đăng nhập bằng email/mật khẩu game và xem tên/ID/số dư. Cookie HttpOnly/Secure, không lưu mật khẩu/token Supabase vào localStorage; trang người chơi không phải trang admin và không cho cộng tiền. Trang xác nhận hỗ trợ cả email TokenHash mới và access-token fragment cũ, xoá token khỏi thanh địa chỉ trước khi xử lý. Giao diện không tuyên bố email đã xác nhận khi chưa kiểm tra được token. Web đăng nhập không đóng phiên đang chơi trong JAR.

## Game

- Tài khoản online mới không có vật phẩm, không tự mặc quần áo/vũ khí; hiển thị nhân vật cơ bản. Chọn nhân vật xong tự vào sảnh.
- Món số 1 (item 0) ở mọi loại đều có giá 0 trong Java và SQL. Mặc thử món chưa sở hữu rồi chọn giỏ hàng để nhận miễn phí. Phải tự mua và trang bị vũ khí trước khi vào trận. Không thay đồ cũ của tài khoản hiện có.
- Shop: trỏ đến đâu viền ở đó nhấp nháy; viền mục đang hoạt động đứng yên. Trái/phải trên hàng mục đổi mục ngay; xuống vào mục con, xuống nữa vào thẻ; lên từ hàng thẻ đầu về mục con, lên nữa về mục chính. Ở mép thẻ trái/phải đổi trang nếu có.
- Túi: L → Trang bị, P → Thời trang, có phím nổi. Mép trái/phải của lưới dùng chuyển trang. OK lần đầu xem và chọn đồ để di chuyển; điều hướng tới ô trống rồi OK đặt. OK lần nữa ở ô nguồn để mặc như trước; 0 mặc món đang cầm; R hoặc mềm phải huỷ di chuyển. Mềm trái chỉ xem thông tin. Các vị trí được lưu riêng từng tài khoản trên server, riêng nam/nữ và riêng offline trong RMS, không thêm/mất/nhân bản đồ khi sắp xếp. Chưa lưu được sẽ hoàn lại vị trí trước.
- Sửa nút cửa ở menu chính khi chọn bị nhỏ trên màn hình lớn: dùng cùng ảnh gốc, cho phép tăng kích thước đúng tỷ lệ, đồng bộ hiệu ứng cửa phòng.

Không deploy Render hoặc chạy SQL trên dữ liệu thật trong quá trình làm. Kiểm tra loopback/giả lập; chưa thử trên điện thoại Nokia thật.


# Gunny Java 6.3.1

Upload toàn bộ gói server mới rồi chờ Render deploy, sau đó cài JAR/JAD 6.3.1 đúng E72 hoặc E6. `/health` phải có `"departures":1`. Bản này không cần SQL mới nếu đã chạy migration `202611040001_gunny_rewards.sql` của 6.3.0; giữ nguyên tài khoản, xu và mẫu email đã cấu hình.

- Hộp tạo phòng dùng bố cục và ảnh native theo mẫu mới; tên phòng/mật mã/checkbox vẫn có tương tác, mật mã không hiển thị chữ “Không”.
- Nền sảnh đổi sang ảnh có ô đất trống; bỏ logo lớn khỏi nền sảnh, giữ logo ở đăng nhập. Công trình native nằm ở ô dưới phải và có nhãn Online, chọn được bằng phím hoặc chạm. Nút Online cũ được thay bằng công trình này.
- Thanh công cụ lấy lại bitmap gốc trong SWF `01abe2636b0f2ab4da71`: shop 32, túi 31, chân thú 63, ngựa 49, bạn bè 29, giấy/bút dùng ảnh native `source-assets/lobby/info.png`, cửa 25, khung 42. Không phóng lại thanh từ ảnh chụp bị vỡ pixel.
- Luồng kết nối có bộ giám sát độc lập, bao gồm cả khi Connector.open chưa trả về. Đăng nhập bị kẹt quá 30 giây sẽ về sảnh để thử lại; kết nối trong trận tối đa 60 giây. Phím phải/Trở về/nút X khi đang kết nối có thể hủy ngay. Phản hồi cũ sau hủy không được ghi đè phiên mới. Không thử lại mù một yêu cầu đăng ký/thay đổi dữ liệu.
- Khi người chơi chủ động rời trận hoặc không phản hồi 45 giây, đối phương nhận thông báo. Trận tạm dừng, đồng hồ không chạy. Chọn **Tiếp tục chơi** để máy điều khiển nhân vật đã thoát, hoặc **Kết thúc, nhận thắng**. Server xác nhận quyền lựa chọn và chiến thắng, lưu thưởng theo cơ chế chống cộng lặp hiện có. Đồng đội cùng phe không được nhận thắng khi người cùng phe thoát.
- Các client cũ không hỗ trợ thông báo rời trận vẫn dùng đường dữ liệu cũ. Nên cập nhật tất cả máy lên 6.3.1 để dùng đủ lựa chọn mới.

Trang tải vẫn ở `/download/`, kèm game hai máy, bản vá và HTML/PDF hướng dẫn. Không tự deploy hoặc thay cấu hình trên tài khoản Render/Supabase thật. Kiểm tra trên giả lập không thay thế thử trực tiếp Nokia vật lý.
