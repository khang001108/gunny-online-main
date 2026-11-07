# Tối ưu zoom B POW — 7.6.1

Cài JAR/JAD 7.6.1 đúng E72/E6. Cập nhật gói server để trang tải phát đúng JAR mới. Không có SQL mới; nếu đã chạy GUNNY-ALL.sql thì không cần chạy lại. Luật gió theo cấp từ 7.6.0 giữ nguyên.

Nền cinematic POW chỉ được chụp một lần, thay vì vẽ lại địa hình và đọc toàn màn hình ở mọi khung hình. Giữ nền và tâm zoom ổn định trong đoạn cinematic; HUD, vòng sáng và ảnh nhân vật vẫn vẽ theo nhịp cũ. Khi zoom/độ tối không đổi, dùng lại khung hình đã xử lý, không chạy lại vòng lặp tính màu toàn màn hình.

Khi zoom thay đổi, tọa độ theo cột và độ tối cơ bản được tính sẵn để giảm phép chia trong vòng lặp điểm ảnh. Ảnh offscreen được giải phóng ngay sau khi chụp; các bộ đệm được dọn khi kết thúc POW, sang trận mới hoặc về sảnh. Ghép ảnh nhân vật POW dùng chung một bộ đệm tạm cho các lớp trong lần ghép, giảm cấp phát và áp lực thu gom rác.

Giữ nguyên asset, ngoại hình đang mặc, công thức zoom/làm tối, thời lượng 150 tick và thời điểm bắn. Áp dụng chung offline và online. Nền giữ yên có chủ ý trong cinematic, trở lại vẽ trận bình thường khi POW kết thúc.

Kiểm tra pixel so với công thức cũ ở các mốc zoom vào/giữ/zoom ra, tái sử dụng bộ đệm và khung hình, giải phóng bộ nhớ, giả lập E72/E6 và các luồng online. Chưa đo FPS trên Nokia thật; không cam kết mức tăng FPS cụ thể trên thiết bị.
