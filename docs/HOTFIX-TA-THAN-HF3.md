# Gunny 7.9.18 HF3 — Tà Thần
Bản lũy tích HF1/HF2, giữ toàn bộ sửa bot và phòng chờ.
- Ải 1: nếu không còn lính/chiến sĩ sống khi đến vòng địch và mục tiêu 20 lính chưa đủ, chỉ hồi sinh/refill, trả lượt cho đội người chơi. Không tạo lượt NPC hoặc chuyển camera sang địch trong vòng hồi sinh. Các vòng sau vẫn đánh bình thường.
- Chiến sĩ, giáo Sulan và hỏa cầu dùng thời gian bay cùng Bulan: 85–150 bước vật lý, trọng lực 20, không gió. Giáo NPC xuyên lính cùng phe, không gây sát thương cho lính đồng minh. Giữ chùm ba giáo của chiến sĩ.
- Cả ba banner mở màn dùng chung font đậm và bảng màu. Tạo sẵn PNG cho assets1/2; script `client/tools/boss_titles_hf3.py` dùng font Arimo Bold (tương thích metric Arial Bold) hoặc Arial Bold trên Windows.
- Sulan đỏ x2 HP theo mức cũ: Dễ 16000, Thường 40600, Khó 53200 (cấu hình mặc định). Giữ 3 lính thường của HF2.
- Gọi vật tổ HP mỗi lượt boss đỏ; mỗi 5 lần gọi, hồi 15% HP tối đa một lần (không vượt maxHp), bỏ hồi tự động mỗi lượt. Đậu Hà Lan là người nói cả ba câu dẫn truyện, không hiện thoại của đậu đã chết. Bong bóng neo vào người nói, ưu tiên phía trên hoặc dưới khi thiếu chỗ.
- Cả hai dạng Sulan quay về nhân vật đang chơi, kể cả khi di chuyển/bay qua lưng; từng phát giáo/lửa quay đúng mục tiêu.
- Backend marker `/health`: `"tribalBotFix":2, "tribalHF3":1`. Không đổi schema SQL hoặc wire protocol.
- Backend Java 17 đã biên dịch; chạy TribalHF3Check (100), TribalBotWireCheck (150), TribalHotfix7918Check (885), SulanAdds7913Check (152), Sulan7915Check và TribalCheck (87). Các kiểm tra BulanSequenceCheck, Sulan7912Check, TribalWavesCheck, TribalVideo797Check và TribalTcpCheck cũng chạy qua trong quá trình sửa.
- Chưa build/preverify JAR do thiếu bộ ecj/CLDC/MIDP và chưa chạy trực tiếp KEmulator. Giải nén đè repo, đóng giả lập, chạy client/Build.bat, commit + push, rebuild backend Xubi, kiểm tra marker rồi tạo phòng mới.
