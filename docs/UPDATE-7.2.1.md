# Cập nhật 7.2.1

Chọn bản đồ dùng điều hướng theo lưới hai cột. Di chuyển cập nhật bản đồ xem trước và viền vàng rõ; đi sang phải để chọn thời gian, xuống cuối để tới Xác nhận. Hỗ trợ giữ phím. Chỉ Xác nhận mới lưu lựa chọn.

Popup mời căn lại hàng danh sách, nút đóng, hai đầu thanh cuộn, thêm rãnh/con trượt và chạm thanh cuộn để đổi trang.

Các biểu tượng bộ, áo, tóc, mặt, kính số 1 là những món khởi đầu theo loại; tháo hết đồ hiển thị nhân vật nền. Không xóa đồ/tài khoản cũ. HTTP 404 từ RPC được báo rõ API bị thiếu thay vì thông báo dịch vụ chung. Chạy đủ migration đến `202611050005_gunny_character.sql` trên Supabase, sau đó `notify pgrst, 'reload schema';` nếu RPC chưa được cập nhật trong cache. Không cần tạo lại tài khoản.

Chưa triển khai lên server hoặc chạy SQL trên Supabase thật.
