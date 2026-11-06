# Đồng bộ trang bị và bảng thẻ online — 7.5.9

Cài JAR/JAD 7.5.9 đúng E72 hoặc E6 trên cả hai máy. Cập nhật gói server để trang tải phát đúng bản mới. Không có thay đổi SQL: nếu đã chạy GUNNY-ALL.sql ở 7.5.8 thì không cần chạy lại. Project mới hoặc chưa cập nhật dữ liệu dùng duy nhất supabase/GUNNY-ALL.sql.

Sửa nhánh vẽ người chơi online đứng nghiêng trên dốc: luôn dùng ngoại hình của đúng người chơi, không dùng đồ local của P2 để vẽ P1. Ngoại hình nhất quán giữa đứng thẳng, đứng nghiêng, bắn và ảnh POW.

Người thua có sát thương được chuyển sang bảng thẻ khi server mở, cùng với người thắng. Khi đồng bộ kết quả cuối, điều kiện được kiểm tra lại sau khi đọc sát thương. Nhận thẻ xong có thể bấm OK hoặc quay lại để rời phòng, không phải chờ người khác nhận thẻ. Người còn lại vẫn nhận thẻ của mình bình thường, không cộng trùng tiền.

Kiểm tra bằng hai client giả lập ở hai độ phân giải: ngoại hình khi nghiêng, đồng bộ bảng thẻ thắng/thua, nhận thẻ, rời phòng trước người khác, số dư và EXP. Chưa thử trên Nokia thật hoặc Supabase/Render thật.
