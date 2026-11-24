# Gunny Java 7.9.18 HF6

Bản patch tích lũy HF1–HF5. Giải nén đè vào gốc repo, build client, push Git và rebuild backend. Health: `dungeonHF6: 1`.

- Phòng ải viễn chinh: ba ô chờ có focus 60/62/64, tọa độ điều hướng theo đúng bố cục, viền sáng, OK đóng/mở hoặc bỏ AI ở ô phụ như thao tác chạm; phím mềm Mời có thể nhắm tới ô trống đang chọn. Ô có người chơi thật không bị xóa.
- Camera Huyệt Ma Kiến ải 2: ưu tiên đạn đang bay trước trạng thái thoại Nữ vương/triệu hồi; hết đạn trả về hành vi camera cũ. Camera tự do giữ nguyên.
- Kết quả phó bản: mọi ải, thắng hoặc thua, sau khi nhận đủ thưởng gửi returnroom thay vì leave. Server giữ mã phòng, thành viên, AI, cấu hình; khi cả đội trở về, ải cuối đặt lại ải đầu của đúng phó bản. Chế độ ghép trận solo vẫn rời phòng; phòng riêng/PvP đội dùng returnroom như kiểm thử gốc.
- Backend không đổi cấu trúc giao thức hoặc SQL, chỉ bổ sung dấu nhận diện HF6.

Kiểm thử đã chạy: javac Java 17 toàn bộ backend và tests; ReturnRoomCheck; TribalTcpCheck (137); TribalBotWireCheck (150); SequenceHF5Check (23). Client ReturnRoomClientCheck được mở rộng cho focus, camera, luồng kết quả; chưa chạy do thiếu MicroEmulator/SDK Java ME trong môi trường này. Chưa build JAR.
