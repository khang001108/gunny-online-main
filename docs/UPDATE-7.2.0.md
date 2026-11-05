# Cập nhật 7.2.0

Tài khoản mới chọn nhân vật nam/nữ và tên khi đăng nhập lần đầu; máy chủ lưu lựa chọn. Tài khoản cũ không phải chọn lại. Túi có hai nút người đỏ/nữ và xanh/nam, hỗ trợ điều hướng/OK và chạm. Đổi giới tính giữ tiền và đồ sở hữu; ô đang mặc chỉ dùng đồ đã sở hữu của giới tính đích, món chưa có được thay bằng món khởi đầu. Được đổi trong phòng chờ, khóa trong trận.

Nền phòng thường và tổ đội dùng chung bitmap gốc `asset.core.framesmallBg`, phủ kín cả phần dưới bảng bên phải. Không cắt ảnh tham khảo làm asset.

Chạy migration `202611050005_gunny_character.sql` sau các migration 7.1.9. Cài đúng JAR/JAD 7.2.0 và server cùng gói. Chưa triển khai dịch vụ hoặc chạy SQL trên Supabase thật.
