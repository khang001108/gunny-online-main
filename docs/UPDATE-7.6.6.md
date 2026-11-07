# Cỡ cả sáu map và Mộc Diệp Trấn — 7.6.6

Cả 6 map tăng chiều cao từ 220 lên 264 đơn vị (+20%), giữ chiều ngang 480 và kích thước nhân vật. Dùng lại ảnh fore gốc từ Resource, không vẽ nền mới. Đất/vật thể cao và dày hơn, phần mép dưới đi xuống sát vùng thanh lực thay vì bị cắt ngang cao trong vùng chơi.

Đồng bộ ảnh E72/E6, chiều cao vật lý, dữ liệu va chạm, mặt nạ vùng cứng, địa hình server, phá đất/phát lại và tỷ lệ bản đồ nhỏ. Giữ nguyên cỡ nhân vật, HUD, sức nổ, tốc độ đạn và mức gió theo cấp.

Mộc Diệp Trấn bỏ toàn bộ mặt nạ cứng: phá được cả gỗ và vòng sắt như trước. Hai quy tắc khác giữ nguyên: Đu Quay không đục được, Phố Nấm Lùn phá phần cỏ trên và giữ lớp đất cứng.

Bắt buộc cập nhật toàn bộ server (backend/assets/land0..5.png và hard0..5.dat) và JAR/JAD 7.6.6 trên tất cả máy cùng chơi. Không dùng chung JAR cũ vì kích thước địa hình/va chạm đã đổi. Không có SQL mới nếu đã cập nhật giá shop bằng GUNNY-ALL.sql từ 7.6.2.

Đã kiểm tra kích thước/va chạm của cả 6 map ở hai độ phân giải, dữ liệu client/server đồng nhất, vùng cứng, Mộc Diệp Trấn phá toàn bộ, camera và bản đồ nhỏ. Chưa thử trên Nokia thật hoặc Render/Supabase thật.
