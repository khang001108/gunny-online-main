# EXP, level, thông tin nhân vật và thưởng — 7.5.7

1. Supabase → SQL Editor: chạy **supabase/UPDATE-7.5.7.sql** nếu project đã có SQL 7.5.6 và bản vá trang bị. File thêm EXP và thay luật thưởng; giữ nguyên tài khoản, đồ, tiền, số trận thắng cũ. Project mới có thể chạy SQL gộp UPDATE-7.5.3-ALL.sql đã cập nhật thay vì chạy từng migration.
2. Cập nhật toàn bộ gói server lên Render.
3. Cài JAR/JAD 7.5.7 đúng E72 hoặc E6. Giữ bản sao lưu trước khi cập nhật.

Level bắt đầu 0. Mỗi cấp cần riêng 50 × (cấp hiện tại + 1) EXP: cấp 0→1 cần 50, 1→2 cần 100, 2→3 cần 150. Tổng 300 EXP đạt cấp 3. Thắng +300 EXP, thua +100 EXP; hòa +100 EXP. Không tính trận hướng dẫn offline. Người chơi cũ bắt đầu phần EXP mới từ 0; số trận thắng đã lưu vẫn được giữ.

Túi đồ dùng nền tooltip asset gốc, hiển thị level, EXP trong cấp/mốc tiếp theo, xếp hạng theo số trận thắng, tổng trận thắng và lực chiến. Các ô trang bị và nhân vật được nâng lên để chừa chỗ. Người bằng số trận thắng đồng hạng; offline không có xếp hạng server. Lực chiến trong bản Java được tính bằng tổng Công kích + Phòng thủ + May mắn của đồ đang mặc (dùng cùng ItemRules với luật trận); không tính đồ chỉ đang sở hữu.

Level xuất hiện trong phòng chờ và cạnh thanh máu/tên nhân vật trong trận. Màn kết quả dùng asset gốc, hiện tổng EXP trận và EXP đồng đội. EXP online là dữ liệu Supabase; offline lưu riêng RMS, không chuyển qua tài khoản online.

Thưởng trận: thắng đấu đơn/tạo phòng 500 xu, tổ đội 1.000 xu. Người thua có gây sát thương nhận lần lượt 250/500 xu và được mở một thẻ thưởng theo cùng cơ chế với người thắng. Thua không gây sát thương vẫn nhận 100 EXP nhưng không nhận xu/thẻ. Hòa không có xu/thẻ. Sát thương được lấy từ mô phỏng server; kết quả/thẻ gửi lại không cộng trùng. Chỉ áp dụng luật mới cho trận được lưu sau khi cập nhật SQL.

Ảnh POW ghép theo mặt nạ trang bị đang mặc, gồm lớp cơ thể/mặt cơ bản khi không mặc đồ. Không dùng lại bộ đồ cũ chỉ vì chỉ số item/skin còn lưu trong hồ sơ. Tương thích đọc giao thức cũ được giữ; dữ liệu EXP/level bổ sung qua API riêng.

Đã kiểm tra luật EXP, ngưỡng cấp, hạng, thưởng người thua, chống cộng trùng; giả lập E72/E6 kiểm tra thông tin, phòng chờ, kết quả/thẻ và so sánh pixel ảnh POW trần/trang bị. Chưa kiểm thử trên Nokia thật hoặc chạy migration trên Supabase của bạn.
