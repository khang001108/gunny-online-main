# Gunny 7.9.18 — Xếp hạng toàn bộ tài khoản, trừ Cheat

## Thay đổi
- Không còn lấy bảng xếp hạng từ `GunnyServer.sessions` (chỉ người đang online).
- Đọc hồ sơ đã lưu từ Supabase `profiles` bằng khóa chỉ có trên server.
- Lọc tài khoản có `test_cheats=true` ngay tại Supabase và kiểm tra lại trong Java.
- Cho phép `test_cheats=false` hoặc NULL (hồ sơ cũ).
- Khi Admin bỏ chọn Cheat, tài khoản sẽ trở lại bảng khi người chơi mở/tải lại bảng.
- Giữ cách xếp hạng gốc: thắng nhiều nhất, sau đó max_damage cao nhất, rồi id.
- Không thay đổi protocol E72/E6, không sửa JAR, không cần SQL migration.
- Giới hạn: server vẫn chỉ trả top 128 tài khoản vì client v7.9.18 dùng mảng 128 phần tử; danh sách được xếp hạng toàn cục, không bị giới hạn người đang online.

## Cài vào repository Git
1. Sao lưu `backend/src/CloudStore.java` và `backend/src/Ranking793.java` đang dùng.
2. Chép hai file tương ứng từ ZIP này vào `backend/src/`, giữ nguyên đường dẫn.
3. Commit và push code lên repository mà Pi 5/Xubi Server Manager đang sử dụng.
4. Build/deploy lại backend/container từ source mới (restart đơn thuần sẽ KHÔNG cập nhật lớp Java trong image cũ).
5. Không phải cập nhật `client/src` hay JAR/JAD.

## Kiểm thử cục bộ (JDK 17+ tại thư mục gốc repo)
mkdir -p out-ranking-check
javac --release 17 -encoding UTF-8 -d out-ranking-check backend/src/*.java backend/tests/*.java
java -cp out-ranking-check Ranking794Check
java -cp out-ranking-check RankingCloudAllCheck
java -cp out-ranking-check CloudStoreCheck

## Các lưu ý
- ZIP nguồn người dùng tải lên bị mất central directory và bị cắt giữa file JAR lớn; chỉ các file backend được khôi phục nguyên vẹn và sử dụng để sửa. Vì vậy bản giao nộp là ZIP bản vá, KHÔNG phải toàn bộ repository hoàn chỉnh.
- Đã biên dịch Java 17 và chạy ba bài kiểm thử qua dữ liệu Memory/mock Supabase (PASS).
- Chưa chạy với Supabase thật, chưa deploy Pi 5 hoặc kiểm tra với Nokia thật.
- Đừng commit `.env` hoặc khóa Supabase lên Git.
