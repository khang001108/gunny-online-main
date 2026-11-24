# Hotfix Bộ Lạc Tà Thần — nền 7.9.18

## Áp dụng
- Giải nén ZIP hotfix đè vào thư mục gốc repo 7.9.18 (nơi có `backend`, `client`, `public`, `verify.sh`). Giữ nguyên các tài nguyên và JAR gốc để làm seed cho script build.
- Chạy `client/Build.bat` trên Windows (cần ECJ, ProGuard, CLDC 1.1 và MIDP 2.0 như cấu hình build cũ). Hai JAR/JAD ở `public/downloads` sẽ được dựng lại.
- Commit mã nguồn và JAR/JAD mới; deploy lại backend qua Xubi. Cài lại JAR mới trên điện thoại/KEmulator. Không cần SQL migration hay cấu hình môi trường mới. Không dùng client cũ cùng server mới trong phó bản.
- Hotfix giữ số phiên bản 7.9.18, không phải gói JAR đã build. Không chép lại JAR cũ lên server sau khi build.

## Các thay đổi
1. Bot đồng minh được giữ trong phòng chờ và trong trận khi selectedBots >= 0; loading lấy roster theo ghế hiện diện từ lobby, không phụ thuộc HP/NPC còn sót từ trận trước.
2. Ải 1: 6 lính tối đa cùng lúc, thêm tối đa 1 chiến sĩ; bù lính thiếu đến tổng 20. Khi đủ 20 lính bị hạ, chiến thắng dù chiến sĩ còn sống. Tọa độ y của lính khi hành quân mở màn được bám theo mặt đất mỗi bước; xóa vận tốc rơi cũ.
3. Ải 2: Bulan rút ngắn số bước mô phỏng đường đạn khoảng 40%, điều chỉnh trọng lực riêng cho đạn Spirit để giữ quỹ đạo cong, không thay đổi sát thương/kỹ năng khác.
4. Ải 3: loại hệ số giảm sát thương 100/(100+640) riêng của Sulan đỏ. Giữ công thức sát thương theo khoảng cách thông thường và HP gốc; ba lính thường + hai chiến sĩ đỏ. Vàng: cú gầm dùng hàng animation callA (row 3) từ atlas gốc, sau đó camera chuyển sang người chịu sát thương. Không vẽ mảnh mặt nạ tự thêm. Chuyển dạng: 75 tick chết vàng -> 65 tick sinh đỏ -> 15 tick đứng đỏ -> gọi đậu, rồi kết thúc ở tick 240. Chỉ tạo NPC đỏ sau đoạn chết vàng, thay vì đổi state/HP ngay từ đầu.
5. Đồng bộ lượt: giữ actor/turn gốc khi chờ xác nhận cuối cinematic; sau xác nhận bắt đầu lượt người chơi với thời gian và thể lực mới. Không phát khung hình idle liên tục lúc client điều khiển; client chấp nhận terminal authoritative của Sulan để tránh pending vô hạn.

## Kiểm thử
Đã biên dịch toàn bộ backend và backend/tests bằng javac 17. PASS: TribalHotfix7918Check (885 assertions), Sulan7912Check, Sulan7915Check, SulanAdds7913Check, TribalCheck, TribalVideo797Check, TribalWavesCheck, BulanSequenceCheck, Raid798Check, DungeonRetreat7918Check, Loading796Check, DungeonLevels7911Check, Fix7914Check, Expansion780Check và TribalTcpCheck (118 checks). Các kiểm thử cũ về số lính/roar/cinematic đã được cập nhật theo yêu cầu mới. Chưa biên dịch JAR CLDC, chưa chạy UI KEmulator/thiết bị thật do thiếu bộ công cụ Java ME trong môi trường này.
