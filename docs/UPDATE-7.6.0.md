# Gió nhẹ cho người chơi cấp thấp — 7.6.0

Cập nhật cả server và JAR/JAD 7.6.0 đúng E72/E6. Không có SQL mới; đã chạy GUNNY-ALL.sql ở 7.5.8 thì không cần chạy lại. Project mới dùng duy nhất supabase/GUNNY-ALL.sql.

Giới hạn gió theo cấp: cấp 01 tối đa ±1.0, cấp 02 ±1.1, cấp 11 ±2.0, từ cấp 21 trở lên ±3.0 như trước. Giới hạn tăng 0.1 mỗi cấp. Gió vẫn ngẫu nhiên cả hai chiều, có thể bằng 0; chỉ giảm biên độ, không đổi công thức gió riêng của từng vũ khí hay trọng lực.

Offline dùng cấp hiện tại của người chơi khi bắt đầu trận. Online server lấy giới hạn thấp nhất từ các tài khoản người thật trong phòng, bỏ qua cấp của bot. Cả hai đội dùng cùng mức gió; giới hạn được chốt khi bắt đầu trận. Có hiệu lực ngay lượt đầu và mọi lượt sau, gồm đấu đơn, tổ đội và phòng riêng. Đồng hồ gió hiển thị đúng gió thật được server gửi về, không có hệ số ẩn theo từng người.

Giữ các sửa từ 7.5.9: ngoại hình online trên dốc đúng người chơi, người thua có sát thương mở được thẻ và có thể rời phòng sau khi nhận thẻ riêng. Giữ asset level 01–70, bố cục túi đồ và thanh phòng offline đã sửa.

Kiểm tra ngưỡng cấp, giới hạn gió nhiều lượt, cả hai chiều, giao diện hai độ phân giải và đồng bộ gió giữa hai client với server. Chưa thử trên Nokia vật lý hoặc Supabase/Render thật.
