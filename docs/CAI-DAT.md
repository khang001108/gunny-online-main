# Level asset gốc và một file SQL — 7.5.8

1. Supabase → SQL Editor: mở **supabase/GUNNY-ALL.sql**, sao chép toàn bộ và Run. Dùng cùng một file cho project mới hoặc project đã chạy các bản trước, kể cả 7.5.7. Không chạy thêm UPDATE-7.5.3-ALL, UPDATE-7.5.7 hay từng migration sau đó.
2. Cập nhật toàn bộ server lên Render, giữ nguyên biến môi trường đang dùng.
3. Cài JAR/JAD 7.5.8 đúng E72/E6.

File SQL gộp chạy trong một giao dịch và chỉ giữ định nghĩa mới nhất của mỗi API. Gộp cả bản vá gunny_equip, shop, trang bị, tài khoản, quản trị, cấp tiền/xu, thông báo, EXP và thưởng người thua. Có thể chạy lại, không xóa/reset tài khoản, đồ, số dư, số trận thắng, EXP hay lịch xóa đang có. Các file cũ đã chuyển vào history để tra cứu; migrations giữ cho quy trình CLI, không phải danh sách cần chạy thêm bằng tay. Template email chỉ là giao diện thư, không tạo dữ liệu game.

Nếu Supabase đã bật pg_cron, file gộp thiết lập duy nhất lịch dọn tài khoản mỗi phút. Nếu chưa bật, server vẫn bảo trì khi đang chạy; muốn lịch chạy cả khi Render ngủ thì bật pg_cron rồi chạy lại cùng file GUNNY-ALL.sql.

Đã thay chữ level tự ghép bằng bitmap gốc `asset.LevelIcon.Level_1` đến `Level_70` lấy từ thư viện Flash đã có. Không tự vẽ số hoặc tô lại màu. Cấp 01–10 bạc; 11–20 xanh lá; 21–30 xanh dương; 31–40 cam; 41–50 bạc/tím; 51–60 vàng; 61–70 huy hiệu vàng có cánh. Dùng đúng hình từng cấp ở túi đồ, phòng chờ, trong trận và kết quả.

Bộ asset không có level 0 nên bắt đầu 01. 01→02 cần 50 EXP, 02→03 cần 100, 03→04 cần 150. Tổng 300 EXP đạt cấp 04. EXP đã tích lũy giữ nguyên, không cộng lại thưởng. Cấp cao nhất theo bộ ảnh là 70 (120.750 EXP), hiển thị MAX; tổng EXP vẫn tiếp tục lưu. Thắng +300 EXP, thua/hòa +100 EXP. Offline lưu riêng RMS, không chuyển sang online.

Túi đồ giữ bảng thông tin dưới nhân vật, level dùng huy hiệu gốc kèm EXP, xếp hạng theo tổng trận thắng, số trận thắng và lực chiến gồm đồ đang mặc. Thưởng người thua có sát thương giữ nguyên từ 7.5.7: 250 xu đấu đơn/phòng riêng, 500 xu tổ đội, được mở một thẻ. Thua không gây sát thương nhận EXP, không có xu/thẻ. Hòa không có xu/thẻ. Ảnh POW dùng đồ thực đang mặc.

Kiểm tra cục bộ gồm ngưỡng cấp 01/02 và 70, bitmap gốc/màu, giao diện hai độ phân giải, API EXP/thưởng, cú pháp SQL/PLpgSQL và gói cài. Chưa thử trên Nokia thật hoặc chạy SQL trên Supabase của bạn.

Sửa bố cục offline: bỏ nút Bạn bè khỏi thanh phòng, điều hướng và vùng bấm; giữ Mời bot. Huy hiệu level nằm trong thẻ nhân vật. Túi đồ nâng nhân vật/ô mặc và đưa toàn bộ bảng thông tin vào nền sáng bên trong. Không cần SQL mới cho các sửa bố cục này.
