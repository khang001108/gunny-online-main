# Cập nhật 7.1.9

Offline dùng G mở cheat; xu, tiền mua sắm và đồ được lưu riêng. Online không kế thừa cheat và đồ offline. Tài khoản online mới có 0 xu, 0 tiền mua sắm và bộ đồ/vũ khí khởi đầu. Giữ nguyên dữ liệu tài khoản đang có.

Thắng tự do/đấu đơn: 500 xu và một thẻ ngẫu nhiên 200–1.000 xu. Thắng tổ đội: 1.000 xu và một thẻ 500–2.000 xu. Thua/hòa không nhận xu hoặc thẻ. Máy chủ xác nhận, chống cộng lặp và chống hai người chọn cùng thẻ.

Bỏ thách đấu phòng bằng mã; vẫn có tìm trận tự động, mời người chơi và bot bằng H.

Trước khi chạy server mới, chạy `supabase/migrations/202611050004_gunny_progression.sql` sau migration trang bị. SQL không xóa số dư hoặc đồ cũ. Cài JAR/JAD 7.1.9 đúng máy. Gói này chưa được triển khai lên dịch vụ hoặc chạy trên Supabase thật.
