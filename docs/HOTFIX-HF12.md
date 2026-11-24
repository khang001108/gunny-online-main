# HF12 — Xem thông tin bạn bè nam/nữ
Áp dụng đè lên repo đã cập nhật HF11.
- Phím hướng chọn hai nút giới tính ở cuối popup thông tin; OK, phím mềm trái hoặc chạm nút để xem.
- Xem trước nhân vật và trang bị của người được xem theo cùng quy tắc đổi giới tính của game, không gọi lệnh gender, không ghi hồ sơ.
- Server hiện chỉ lưu một bộ đang mặc; giới tính còn lại là bản xem trước dựa trên đồ sở hữu tương thích, không phải bộ đồ thứ hai đã lưu riêng.
- Cập nhật cả backend và client; `/health` có `profileGenderHF12: 1`. Không cần SQL.
- Backend Java 17 biên dịch thành công; ProfileGenderHF12Check: 24/24. Chưa build JAR/kiểm thử trực tiếp trên KEmulator.
